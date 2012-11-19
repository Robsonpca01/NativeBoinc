

package sk.boinc.mobileboinc.nativeclient;

import java.util.ArrayList;

import sk.boinc.nativeboinc.util.TaskItem;

public interface NativeBoincTasksListener extends NativeBoincServiceListener {
	public abstract void getTasks(ArrayList<TaskItem> tasks);
}
