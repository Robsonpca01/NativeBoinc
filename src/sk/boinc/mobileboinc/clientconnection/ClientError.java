
package sk.boinc.mobileboinc.clientconnection;


public class ClientError {
	public int errorNum;
	public String message;
	
	public ClientError(int errorNum, String message) {
		this.errorNum = errorNum;
		this.message = message;
	}
}
