/**
 * 
 */
package sk.boinc.mobileboinc.nativeclient;


public interface NativeBoincUpdateListener extends NativeBoincServiceListener {
	public abstract void updatedProjectApps(String projectUrl);
	public abstract void updateProjectAppsError(String projectUrl);
}
