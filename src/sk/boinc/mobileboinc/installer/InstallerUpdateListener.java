
package sk.boinc.mobileboinc.installer;

import java.util.ArrayList;

import sk.boinc.nativeboinc.util.UpdateItem;

public interface InstallerUpdateListener extends AbstractInstallerListener {
	public abstract void currentProjectDistribList(ArrayList<ProjectDistrib> projectDistribs);
	public abstract void currentClientDistrib(ClientDistrib clientDistrib);
	public abstract void binariesToUpdateOrInstall(UpdateItem[] updateItems);
	public abstract void binariesToUpdateFromSDCard(String[] projectNames);
}
