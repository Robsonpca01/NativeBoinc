

package sk.boinc.mobileboinc.nativeclient;

import java.util.ArrayList;

import edu.berkeley.boinc.lite.Project;


public interface NativeBoincProjectsListener extends NativeBoincServiceListener {
	public abstract void getProjects(ArrayList<Project> projects);
}
