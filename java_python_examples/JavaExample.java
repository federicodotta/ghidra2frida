import ghidra2frida.Ghidra2FridaService;

public class JavaExample extends GhidraScript {
	
	Ghidra2FridaService ghidra2FridaService;
		
	public String callGhidra2FridaFunction(String name, String[] parameters) throws Exception {
		
		if(ghidra2FridaService == null) {
			ghidra2FridaService = state.getTool().getService(Ghidra2FridaService.class);
			println("ghidra2frida service initialized");
		}
		
		return ghidra2FridaService.callExportedFunction(name,parameters);		
		
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
