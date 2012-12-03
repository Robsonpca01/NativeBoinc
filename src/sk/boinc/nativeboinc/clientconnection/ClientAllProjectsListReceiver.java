
package sk.boinc.nativeboinc.clientconnection;

import java.util.ArrayList;

import edu.berkeley.boinc.lite.ProjectListEntry;


public interface ClientAllProjectsListReceiver extends ClientReceiver {
	public abstract boolean currentAllProjectsList(ArrayList<ProjectListEntry> allProjects);
}
