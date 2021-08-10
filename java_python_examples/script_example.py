from ghidra2frida import Ghidra2FridaService

ghidra2FridaService = None

def callGhidra2FridaFunction(name, parameters):
    global ghidra2FridaService
    if ghidra2FridaService is None:
        ghidra2FridaService = state.getTool().getService(Ghidra2FridaService)
        println("ghidra2FridaService initialized")

    return ghidra2FridaService.callExportedFunction(name,parameters);

try:

    ret_value = callGhidra2FridaFunction("function_name",["arg1","arg2"]);
    
except Exception as exc:   
    printerr(str(exc))


