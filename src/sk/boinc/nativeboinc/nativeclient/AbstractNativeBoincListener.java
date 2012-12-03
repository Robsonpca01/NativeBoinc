

package sk.boinc.nativeboinc.nativeclient;


public interface AbstractNativeBoincListener {
	public abstract boolean onNativeBoincClientError(String message);
	
	public abstract void onChangeRunnerIsWorking(boolean isWorking);
}
