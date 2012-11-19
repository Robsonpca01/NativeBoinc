
package sk.boinc.mobileboinc.clientconnection;


public interface ClientReceiver {
	public static final int PROGRESS_CONNECTING = 0;
	public static final int PROGRESS_AUTHORIZATION_PENDING = 1;
	public static final int PROGRESS_INITIAL_DATA = 2;
	public static final int PROGRESS_XFER_STARTED = 3;
	public static final int PROGRESS_XFER_FINISHED = 4;
	public static final int PROGRESS_XFER_POLL = 5;

	public abstract boolean clientError(BoincOp boincOp, int err_num, String message);
	public abstract void clientConnectionProgress(BoincOp boincOp, int progress);
	public abstract void clientConnected(VersionInfo clientVersion);
	public abstract void clientDisconnected(boolean disconnectedByManager);
	
	public abstract void onClientIsWorking(boolean isWorking);
}
