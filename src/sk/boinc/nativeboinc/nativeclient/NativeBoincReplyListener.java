

package sk.boinc.nativeboinc.nativeclient;

public interface NativeBoincReplyListener extends NativeBoincServiceListener {
	public abstract void onProgressChange(double progress);
}
