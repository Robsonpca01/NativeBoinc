
package sk.boinc.mobileboinc.clientconnection;


public interface ClientPollReceiver extends ClientReceiver {
	/**
	 * @param errorNum -- error number
	 * @param operation - poll operation code (from PollOp)
	 * @param errorMessage 
	 * @param param
	 */
	public abstract boolean onPollError(int errorNum, int operation, String errorMessage, String param);
	
	public abstract void onPollCancel(int opFlags);
}
