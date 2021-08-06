/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra2frida;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import generic.jar.ResourceFile;
import ghidra.app.ExamplesPluginPackage;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.plugin.ProgramPlugin;
import ghidra.app.services.ConsoleService;
import ghidra.app.services.ProgramManager;
import ghidra.framework.Application;
import ghidra.framework.options.SaveState;
import ghidra.framework.plugintool.*;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.model.listing.Program;
import ghidra.util.Msg;

/**
 * TODO: Provide class-level documentation that describes what this plugin does.
 * - Kill pyro server on Ghidra exit?
 */
//@formatter:off
@PluginInfo(
		status = PluginStatus.STABLE, 
		packageName = ExamplesPluginPackage.NAME, 
		category = PluginCategoryNames.EXAMPLES, 
		shortDescription = "Plugin short description goes here.", 
		servicesProvided = { Ghidra2FridaService.class }, 
		description = "Plugin long description goes here.")
//@formatter:on
//public class Ghidra2fridaPlugin extends ProgramPlugin {
public class Ghidra2fridaPlugin extends Plugin {

	//private DockingAction helloAction;
	Ghidra2FridaComponentProvider ghidra2fridaComponentProvider;

	ConsoleService consoleService;

	Ghidra2FridaServiceProvider provider;

	String pyroServerAbolutePath;

	/**
	 * Plugin constructor.
	 * 
	 * @param tool The plugin tool that this plugin is added to.
	 */
	public Ghidra2fridaPlugin(PluginTool tool) {
		//super(tool, true, true);
		super(tool);

		/*
		// Create the hello action.
		helloAction = new DockingAction("Hello World", getName()) {
			@Override
			public void actionPerformed(ActionContext context) {
				// Msg.showInfo(this, "Hello World!");
				Msg.showInfo(this, tool.getActiveWindow(), "No file selected", "No file will be imported.");
				Msg.info(this, "Hello Program:: action");

				List<ResourceFile> risorse = Application.findFilesByExtensionInMyModule("py");
				Msg.info(this, risorse.size());
				for (int i = 0; i < risorse.size(); i++)
					Msg.info(this, risorse.get(i).getAbsolutePath());

				try {
					ResourceFile jniArchiveFile = Application.getModuleDataFile("ghidra2frida", "ghidraServicePyro.py");

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					Msg.showError(this, tool.getActiveWindow(), "File not found", e.toString());
				}

			}
		};
		// Enable the action - by default actions are disabled.
		helloAction.setEnabled(true);

		// Put the action in the global "View" menu.
		// helloAction.setMenuBarData(new MenuData(new String[] { "&File", "Hello World"
		// }));
		helloAction.setMenuBarData(
				new MenuData(new String[] { ToolConstants.MENU_TOOLS, "ghidra2frida", "Write Help Info File" }));

		// Add the action to the tool.
		tool.addAction(helloAction);
		 */
		copyPyroServer();		
		
		provider = new Ghidra2FridaServiceProvider(tool,pyroServerAbolutePath);
		registerServiceProvided(Ghidra2FridaService.class, provider);
		
		String pluginName = getName();
		ghidra2fridaComponentProvider = new Ghidra2FridaComponentProvider(tool, pluginName, provider);
		
		/*String topicName = this.getClass().getPackage().getName();
		String anchorName = "HelpAnchor";
		myProvider.setHelpLocation(new HelpLocation(topicName, anchorName));*/

	}

	@Override
	public void init() {
		super.init();

		// TODO: Acquire services if necessary

		consoleService = tool.getService(ConsoleService.class);
		consoleService.println("[*] ghidra2frida init");
		
		provider.setConsoleService(consoleService);

		//Ghidra2FridaService ghidra2FridaServiceService = tool.getService(Ghidra2FridaService.class);
		//consoleService.println(ghidra2FridaServiceService.testService("AAAA"));
		
	}

	public void copyPyroServer() {
		try {
			
			ResourceFile jniArchiveFile = Application.getModuleDataFile("ghidra2frida", "ghidraServicePyro.py");
			
			//InputStream inputStream = getClass().getClassLoader().getResourceAsStream("res/bridaServicePyro.py");
			BufferedReader reader = new BufferedReader(new InputStreamReader(jniArchiveFile.getInputStream()));
			File outputFile = new File(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator")
					+ "bridaServicePyro.py");

			FileWriter fr = new FileWriter(outputFile);
			BufferedWriter br = new BufferedWriter(fr);

			String s;
			while ((s = reader.readLine()) != null) {

				br.write(s);
				br.newLine();

			}
			reader.close();
			br.close();

			pyroServerAbolutePath = outputFile.getAbsolutePath();

		} catch (Exception e) {

			Msg.showError(this, tool.getActiveWindow(), "Error copying Pyro Server file", e.toString());

		}
	}

	/**
	 * Get the currently open program using the ProgramManager service.
	 */
	public Program getProgram() {

		ProgramManager pm = tool.getService(ProgramManager.class);
		if (pm != null) {
			return pm.getCurrentProgram();
		}
		return null;
	}
	
	
	@Override
	protected void dispose() {
		
		provider.killPyroServer();
		
	}
	
	public void writeConfigState​(SaveState saveState) {
		
		ghidra2fridaComponentProvider.saveConfigurationsInSaveState(saveState);
		
	}
		
	public void readConfigState​(SaveState saveState) {
		
		ghidra2fridaComponentProvider.loadConfigurationsFromSaveState(saveState);
		
	}
	
}
