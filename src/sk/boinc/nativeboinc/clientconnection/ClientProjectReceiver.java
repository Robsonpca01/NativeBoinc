
package sk.boinc.nativeboinc.clientconnection;

import edu.berkeley.boinc.lite.ProjectConfig;

public interface ClientProjectReceiver extends ClientPollReceiver {
	public abstract boolean currentAuthCode(String projectUrl, String authCode);
	public abstract boolean currentProjectConfig(String projectUrl, ProjectConfig projectConfig);
	
	public abstract boolean onAfterProjectAttach(String projectUrl);
}
