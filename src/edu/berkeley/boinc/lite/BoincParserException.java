
package edu.berkeley.boinc.lite;


public class BoincParserException extends Exception {
	
	public BoincParserException() {
		
	}
	
	public BoincParserException(int lineNo, String message) {
		super("Error in "+lineNo+": "+message);
	}
	
	public BoincParserException(Throwable t) {
		super(t);
	}
}
