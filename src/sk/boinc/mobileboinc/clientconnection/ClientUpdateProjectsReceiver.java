
package sk.boinc.mobileboinc.clientconnection;

import java.util.ArrayList;


public interface ClientUpdateProjectsReceiver extends ClientReceiver {
	public abstract boolean updatedProjects(ArrayList<ProjectInfo> projects);
}
