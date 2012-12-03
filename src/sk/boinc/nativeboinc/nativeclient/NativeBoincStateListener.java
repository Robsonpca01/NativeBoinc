
package sk.boinc.nativeboinc.nativeclient;


public interface NativeBoincStateListener extends AbstractNativeBoincListener {
	//public abstract void onClientStateChanged(boolean isRun);
	
	public abstract void onClientStart();
	public abstract void onClientStop(int exitCode, boolean stoppedByManager);
}
