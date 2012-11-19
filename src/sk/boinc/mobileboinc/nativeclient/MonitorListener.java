/**
 * 
 */
package sk.boinc.mobileboinc.nativeclient;

import edu.berkeley.boinc.nativeboinc.ClientEvent;


public interface MonitorListener {
	public abstract void onMonitorEvent(ClientEvent event);
	public abstract void onMonitorDoesntWork();
}
