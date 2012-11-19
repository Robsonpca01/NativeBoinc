
package sk.boinc.mobileboinc.clientconnection;


public class PollError {
	public int errorNum;
	public int operation;
	public String message;
	public String param;
	
	public PollError(int errorNum, int operation, String message, String param) {
		this.errorNum = errorNum;
		this.operation = operation;
		this.message = message;
		this.param = param;
	}
}
