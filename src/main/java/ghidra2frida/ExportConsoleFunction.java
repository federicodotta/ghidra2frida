package ghidra2frida;

import java.util.ArrayList;

public class ExportConsoleFunction {
	
	String name;
	ArrayList<String> arguments;
	
	public ExportConsoleFunction(String name, ArrayList<String> arguments) {
		super();
		this.name = name;
		this.arguments = arguments;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ArrayList<String> getArguments() {
		return arguments;
	}
	public void setArguments(ArrayList<String> arguments) {
		this.arguments = arguments;
	}	
	
}
