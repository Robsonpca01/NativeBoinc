
package sk.boinc.nativeboinc.nativeclient;

import java.util.ArrayList;

import sk.boinc.nativeboinc.util.TaskItem;

public interface NativeBoincTasksListener extends NativeBoincServiceListener {
	public abstract void getTasks(ArrayList<TaskItem> tasks);
}
