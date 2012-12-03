

package sk.boinc.nativeboinc.nativeclient;


public interface NativeBoincServiceListener extends AbstractNativeBoincListener {
	public abstract int getRunnerServiceChannelId();
	
	public abstract boolean onNativeBoincServiceError(WorkerOp workerOp, String message);
}
