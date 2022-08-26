package ghidra2frida;

import ghidra.framework.plugintool.ServiceInfo;

@ServiceInfo(defaultProvider = Ghidra2fridaPlugin.class)
public interface Ghidra2FridaService {
	
	// Server management
	public boolean launchPyroServer(String pyroHost, String pyroPort, boolean useVirtualEnvCheckBox, String pythonPathEnv);
	public boolean killPyroServer();
	public boolean pingServer();
	
	// Application management
	public boolean spawnApplication(boolean spawn, String device, String applicationId, String fridaPath, String host);
	public boolean killOrDetachApplication(boolean kill);
	public boolean pingApplication();
	
	// Status
	public boolean getServerStarted();
	public boolean getApplicationSpawned();
	
	// Misc
	public boolean reloadJs();
	public boolean detachAllHooks();
	
	// Main method: execute a method on the device using ghidra2frida bridge
	public String callExportedFunction(String name, String[] arguments) throws Exception;

}
