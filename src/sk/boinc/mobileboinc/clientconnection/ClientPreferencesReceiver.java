

package sk.boinc.mobileboinc.clientconnection;

import edu.berkeley.boinc.lite.GlobalPreferences;


public interface ClientPreferencesReceiver extends ClientReceiver {
	public abstract void currentGlobalPreferences(GlobalPreferences globalPrefs);
	public abstract void onGlobalPreferencesChanged();
}
