package ghidra2frida;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import generic.jar.ResourceFile;
import ghidra.app.services.ConsoleService;
import ghidra.framework.Application;
import ghidra.framework.plugintool.PluginTool;
import ghidra.util.Msg;
import net.razorvine.pickle.PickleException;
import net.razorvine.pyro.PyroException;
import net.razorvine.pyro.PyroProxy;
import net.razorvine.pyro.PyroURI;

public class Ghidra2FridaServiceProvider implements Ghidra2FridaService {

	PluginTool tool;
	Process pyroServerProcess;
	PyroProxy pyroBridaService;
	boolean serverStarted;
	boolean applicationSpawned;
	String pyroServerAbolutePath;
	ConsoleService consoleService;

	Thread stdoutThread;
	Thread stderrThread;

	public Ghidra2FridaServiceProvider(PluginTool tool, String pyroServerAbolutePath) {
		this.tool = tool;
		this.pyroServerAbolutePath = pyroServerAbolutePath;
	}

	public void setConsoleService(ConsoleService consoleService) {
		this.consoleService = consoleService;
	}
	
	public boolean getServerStarted() {
		return serverStarted;
	}
	
	public boolean getApplicationSpawned() {
		return applicationSpawned;
	}
	
	public boolean copyJsSkeleton(File fridaPathFile) {
		
		try {

			ResourceFile ghidra2fridaJsFile = Application.getModuleDataFile("ghidra2frida",
					"ghidra2frida.js");

			InputStream inputStream = ghidra2fridaJsFile.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			File outputFile = new File(fridaPathFile.getAbsolutePath());

			// Check if file already exists
			if (outputFile.exists()) {
				JFrame parentDialogResult = new JFrame();
				int dialogResult = JOptionPane.showConfirmDialog(parentDialogResult,
						"The file ghidra2frida.js already exists. Would you like to overwrite it?",
						"Warning", JOptionPane.YES_NO_OPTION);
				if (dialogResult != JOptionPane.YES_OPTION) {
					reader.close();
					return false;
				}
			}

			FileWriter fr = new FileWriter(outputFile);
			BufferedWriter br = new BufferedWriter(fr);

			String s;
			while ((s = reader.readLine()) != null) {

				br.write(s);
				br.newLine();

			}
			reader.close();
			br.close();
			
			return true;

		} catch (Exception e) {

			consoleService.printlnError("Error copying Frida ghidra2frida JS file");
			consoleService.printlnError(e.toString());
			return false;

		}	
		
	}

	public boolean killPyroServer() {

		if (serverStarted) {
			
			stdoutThread.stop();
			stderrThread.stop();

			try {

				executePyroCall("shutdown",new Object[] {}); 
				pyroServerProcess.destroy();
				pyroBridaService.close();
				serverStarted = false;
				applicationSpawned = false;

				consoleService.println("Pyro server shutted down");
				
				return true;

			} catch (final Exception e) {

				consoleService.printlnError("Exception shutting down Pyro server");
				consoleService.printlnError(e.toString());
				
				return false;

			}

		} else {

			consoleService.printlnError("Pyro server is not started.");
			
			return false;

		}

	}
	
	private static ExportConsoleFunction parseMethod(String call) {
		
		call = call.trim();
		
		String functionName;
		ArrayList<String> argumentsList;
		
		if(call.endsWith(")") && call.contains("(")) {
			
			functionName = call.split("\\(",2)[0].trim();

			if(functionName.length() == 0) {
				return null;						
			}
			
			String functionArgs = call.split("\\(",2)[1];
			
			functionArgs = functionArgs.substring(0,functionArgs.length()-1);
			
			argumentsList = new ArrayList<String>();
			
			if(functionArgs.trim().length() > 0) {
			
				String remainArgs = functionArgs.trim();
				
				while(remainArgs.length() > 0) {
					
					String regex = "^[,\\s]*\"(.*)$";
					Pattern pattern = Pattern.compile(regex);
					Matcher matcher = pattern.matcher(remainArgs);
					if (matcher.find()) {
						remainArgs = matcher.group(1);
					} else {
						return null;
					}
										
					int doubleQuoteCharIndex = remainArgs.indexOf('"', 0);
					
					if(doubleQuoteCharIndex != -1) {
					
						while( doubleQuoteCharIndex > 0 && remainArgs.charAt(doubleQuoteCharIndex-1) == '\\' ) {
							
							StringBuilder sb = new StringBuilder(remainArgs);
							sb.deleteCharAt(doubleQuoteCharIndex-1);
							remainArgs = sb.toString();
							
							doubleQuoteCharIndex = remainArgs.indexOf('"', doubleQuoteCharIndex);		
							
							if(doubleQuoteCharIndex == -1) {
								return null;
							}
							
						}
						
					} else {
						
						return null;
						
					}
						
					argumentsList.add(remainArgs.substring(0,doubleQuoteCharIndex));
										
					remainArgs = remainArgs.substring(doubleQuoteCharIndex+1,remainArgs.length()).trim();					
					
					
				}
				
			}
			
			return new ExportConsoleFunction(functionName,argumentsList);			
			
		}
		
		return null;		
		
	}
	
	public String executeDebugMethod(String functionCall) {
		
		String returnMessage = "";
		
		if (serverStarted && applicationSpawned) {
			
			ExportConsoleFunction parsedCall = parseMethod(functionCall);
			
			if(parsedCall != null) {
		
				try {
					
					final String s = (String)(executePyroCall("callexportfunction",new Object[] {parsedCall.getName(),parsedCall.getArguments().toArray()}));
					
					returnMessage += "* Call: " + functionCall + "\n";
					returnMessage += "* Output: " + s + "\n";
					
					
				} catch (Exception e) {
										
					returnMessage += "* ERROR: " + e.toString() + "\n";
					
				}
				
			} else {
				
				returnMessage += "* ERROR: Error in parsing";
				
			}
		
		} else {
			
			returnMessage += "* ERROR: Cannot execute method. Server must be running and application attached.";
			
		}
		
		return returnMessage;
				
	}
	
	public boolean reloadJs() {
		
		if (serverStarted && applicationSpawned) {
			
			try {
				
				executePyroCall("reload_script",new Object[] {});
				
				consoleService.println("*** JS file reloaded correctly.");
				return true;
				
			} catch (Exception e) {
				
				consoleService.printlnError("Exception with reloadJs");
				consoleService.printlnError(e.toString());
				return false;
				
			}
		
		} else {
			
			consoleService.printlnError("Cannot execute method. Server must be running and application attached.");
			return false;
			
		}
		
	}
	
	public boolean detachAllHooks() {
		
		if (serverStarted && applicationSpawned) {
			
			try {
				
				executePyroCall("callexportfunction",new Object[] {"detachAll",new String[] {}});
				
				consoleService.println("*** Detach all hooks correctly.");
				return true;
				
			} catch (Exception e) {
				
				consoleService.printlnError("Exception with detach all hooks");
				consoleService.printlnError(e.toString());
				return false;
				
			}
		
		} else {
			
			consoleService.printlnError("Cannot detach. Server must be running and application attached.");
			return false;
			
		}
		
	}
	
	public boolean launchPyroServer(String pyroHost, String pyroPort, boolean useVirtualEnvCheckBox,
			String pythonPathEnv) {

		Runtime rt = Runtime.getRuntime();

		String[] startServerCommand;
		String[] execEnv;
		String debugCommandToPrint;

		if (useVirtualEnvCheckBox) {

			// Add / or \\ if not present
			pythonPathEnv = pythonPathEnv.trim().endsWith(System.getProperty("file.separator")) ? pythonPathEnv.trim()
					: pythonPathEnv.trim() + System.getProperty("file.separator");

			// System.getProperty("file.separator")
			if (System.getProperty("os.name").trim().toLowerCase().startsWith("win")) {

				startServerCommand = new String[] { pythonPathEnv + "Scripts\\python.exe", "-i", pyroServerAbolutePath,
						pyroHost.trim(), pyroPort.trim() };
				execEnv = new String[] { "VIRTUAL_ENV=" + pythonPathEnv, "PATH=" + pythonPathEnv + "Scripts" };

				debugCommandToPrint = "\"" + pythonPathEnv + "Scripts\\python.exe\" -i \"" + pyroServerAbolutePath
						+ "\" " + pyroHost.trim() + " " + pyroPort.trim();

			} else {

				startServerCommand = new String[] { pythonPathEnv + "bin/python", "-i", pyroServerAbolutePath,
						pyroHost.trim(), pyroPort.trim() };
				execEnv = new String[] { "VIRTUAL_ENV=" + pythonPathEnv, "PATH=" + pythonPathEnv + "bin/" };

				debugCommandToPrint = "\"" + pythonPathEnv + "bin/python\" -i \"" + pyroServerAbolutePath + "\" "
						+ pyroHost.trim() + " " + pyroPort.trim();

			}

		} else {

			startServerCommand = new String[] { pythonPathEnv, "-i", pyroServerAbolutePath, pyroHost.trim(),
					pyroPort.trim() };
			execEnv = null;

			debugCommandToPrint = "\"" + pythonPathEnv + "\" -i \"" + pyroServerAbolutePath + "\" " + pyroHost.trim()
					+ " " + pyroPort.trim();

		}

		try {

			pyroServerProcess = rt.exec(startServerCommand, execEnv);

			final BufferedReader stdOutput = new BufferedReader(
					new InputStreamReader(pyroServerProcess.getInputStream()));
			final BufferedReader stdError = new BufferedReader(
					new InputStreamReader(pyroServerProcess.getErrorStream()));

			// Initialize thread that will read stdout
			stdoutThread = new Thread() {

				public void run() {

					while (true) {

						try {

							final String line = stdOutput.readLine();

							// Only used to handle Pyro first message (when server start)
							if (line.equals("Ready.")) {

								pyroBridaService = new PyroProxy(new PyroURI(
										"PYRO:BridaServicePyro@" + pyroHost.trim() + ":" + pyroPort.trim()));
								serverStarted = true;
								
								consoleService.println("Pyro server started correctly");

							} else {

								consoleService.printlnError(line);

							}

						} catch (IOException e) {
							
							consoleService.printlnError("Error reading Pyro stdout");
							consoleService.printlnError(e.toString());
							
						}

					}
				}

			};
			stdoutThread.start();

			// Initialize thread that will read stderr
			stderrThread = new Thread() {

				public void run() {

					while (true) {

						try {

							final String line = stdError.readLine();
							
							consoleService.printlnError(line);

						} catch (IOException e) {

							consoleService.printlnError("Error reading Pyro stderr");
							consoleService.printlnError(e.toString());

						}

					}
				}

			};
			stderrThread.start();

			return true;

		} catch (IOException e) {

			Msg.showError(this, tool.getActiveWindow(), "Error reading Pyro stdout", e.toString());
			return false;

		}

	}
	
	
	public String callExportedFunction(String name, String[] arguments) throws Exception {
		
		String ret = (String)executePyroCall("callexportfunction",new Object[] {name, arguments});
		
		return ret;
		
	}
	
	
	public Object executePyroCall(String name, Object[] arguments) throws Exception {
		
		final ArrayList<Object> threadReturn = new ArrayList<Object>(); 
				
		final Runnable stuffToDo = new Thread()  {
		  @Override 
		  public void run() { 
			  try {
				threadReturn.add(pyroBridaService.call(name, arguments));
			} catch (PickleException | PyroException | IOException e) {
				threadReturn.add(e);
			}
		  }
		};

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future future = executor.submit(stuffToDo);
		executor.shutdown(); 

		try { 
		  //future.get(1, TimeUnit.MINUTES); 
			future.get(30, TimeUnit.SECONDS);
		}
		catch (InterruptedException | ExecutionException | TimeoutException ie) { 
			threadReturn.add(ie);
		}
				
		if (!executor.isTerminated())
			executor.shutdownNow(); 
		
		if(threadReturn.size() > 0) {
			if(threadReturn.get(0) instanceof Exception) {
				throw (Exception)threadReturn.get(0);
			} else {
				return threadReturn.get(0);
			}
		} else {
			return null; 
		} 
		
	}
	
	public boolean spawnApplication(boolean spawn, String device, String applicationId, String fridaPath, String host) {
		
		if(serverStarted) { 
		
			try {
				
							
				if(spawn) {
					
					executePyroCall("spawn_application",new Object[] {applicationId.trim(), fridaPath.trim(),device, host});
					
					// Wait for 3 seconds in order to load hooks
					Thread.sleep(500);
		
					executePyroCall("resume_application", new Object[] {});
					
				} else {
					
					executePyroCall("attach_application",new Object[] {applicationId.trim(), fridaPath.trim(),device, host});
									
				}
				
				applicationSpawned = true;
							
				if(spawn) {
					consoleService.println("Application " + applicationId.trim() + " spawned correctly");
				} else {
					consoleService.println("Application with PID " + applicationId.trim() + " attached correctly");
				}
				
				return true;
				
			} catch (final Exception e) {
				
				consoleService.printlnError("Exception with " + (spawn ? "spawn" : "attach") + " application");
				consoleService.printlnError(e.toString());
				return false;
				
			}	
			
		} else {
			
			consoleService.printlnError("Can't " + (spawn ? "spawn" : "attach") + " application: server is not started");
			return false;
			
		}
		
	}
	
	public boolean pingServer() {
		
		if(serverStarted) {
			
			try {
				boolean result = (boolean)executePyroCall("ping",new Object[] {});
				
				if(result) {
					consoleService.println("Server is alive!");
					return true;
				} else {
					consoleService.printlnError("Something went wrong");
				}
				
			} catch (Exception e) {
				consoleService.printlnError(e.toString());
			}
			
			// If something went wrong set server as not alive and application as not attached
			serverStarted = false;
			applicationSpawned = false;
			return false;
			
		} else {
			consoleService.printlnError("This feature requires a runnning Pyro server");
			return false;
		}
		
		
	}
	
	public boolean pingApplication() {
		
		if(serverStarted && applicationSpawned) {
			
			try {
				boolean result = (boolean)executePyroCall("callexportfunction",new Object[] {"ping",new String[0]});
				
				if(result) {
					consoleService.println("Application is alive!");
					return true;
				} else {
					consoleService.printlnError("Something went wrong");
				}
				
			} catch (Exception e) {
				consoleService.printlnError(e.toString());
			}
			
			// If something went wrong set server as not alive and application as not attached
			applicationSpawned = false;
			
			return false;
			
		} else {
			consoleService.printlnError("This feature requires a runnning Pyro server and an attached/spawned application");
			return false;
		}		
		
	}
	
	public boolean killOrDetachApplication(boolean kill) {
		
		if(serverStarted && applicationSpawned) {
			
			try {
				
				if(kill)				
					executePyroCall("disconnect_application",new Object[] {});
				else
					executePyroCall("detach_application",new Object[] {});
				
				applicationSpawned = false;
								
				if(kill)
					consoleService.println("Killing application executed");
				else
					consoleService.println("Detaching  application executed");
				
				return true;
				
			} catch (final Exception e) {
				
				consoleService.printlnError("Exception killing application");
				consoleService.printlnError(e.toString());
				return false;
				
			}
			
			
		} else {
			
			consoleService.printlnError("Can't " + (kill ? "kill" : "detach") + " application: application is not attached");
			return false;
			
		}
				
	}

}
