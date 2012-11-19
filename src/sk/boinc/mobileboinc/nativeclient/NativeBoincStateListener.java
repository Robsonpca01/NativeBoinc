
package sk.boinc.mobileboinc.nativeclient;


public interface NativeBoincStateListener extends AbstractNativeBoincListener {
	//public abstract void onClientStateChanged(boolean isRun);
	
	public abstract void onClientStart();
	public abstract void onClientStop(int exitCode, boolean stoppedByManager);
}
