
package sk.boinc.nativeboinc.nativeclient;

import java.util.List;


public interface NativeBoincAutoInstallListener extends AbstractNativeBoincListener {
	public abstract void beginProjectInstallation(String projectUrl);
	/* called when projects not found or binaries provided by project */
	public abstract void projectsNotFound(List<String> projectUrls);
}
