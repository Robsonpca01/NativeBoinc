
package sk.boinc.mobileboinc.nativeclient;

public interface NativeBoincReplyListener extends NativeBoincServiceListener {
	public abstract void onProgressChange(double progress);
}
