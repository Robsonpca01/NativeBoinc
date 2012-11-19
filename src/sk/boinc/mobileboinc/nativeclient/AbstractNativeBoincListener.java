

package sk.boinc.mobileboinc.nativeclient;


public interface AbstractNativeBoincListener {
	public abstract boolean onNativeBoincClientError(String message);
	
	public abstract void onChangeRunnerIsWorking(boolean isWorking);
}
