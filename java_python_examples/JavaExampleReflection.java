import java.lang.reflect.Method;

public class SwiftDemangler2 extends GhidraScript {
	
	Object ghidra2FridaService;
	
	public String callGhidra2FridaFunction(String name, String[] parameters) throws Exception {
		
		if(ghidra2FridaService == null) {
			PluginTool pluginTool = state.getTool();
			ghidra2FridaService = pluginTool.getService(Class.forName("ghidra2frida.Ghidra2FridaService"));
			println("ghidra2frida service initialized");
		}
		
		Method ghidra2FridaCallExportedFunction = Class.forName("ghidra2frida.Ghidra2FridaService").getMethod("callExportedFunction", java.lang.String.class, java.lang.String[].class);		
		return (String)ghidra2FridaCallExportedFunction.invoke(ghidra2FridaService, name, parameters);
				
	}
	
	@Override
	protected void run() throws Exception {
		
	    try {
	    			    
		    String retVal = callGhidra2FridaFunction("functionName",new String[] {"argument1","argument2"});
		    		    
	    } catch(Exception e) {
	    	
	    	printerr(e.toString());
	    	
	    }
		 		
	}
}
