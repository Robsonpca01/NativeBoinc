/**
 * 
 */
package sk.boinc.mobileboinc.clientconnection;

import java.util.ArrayList;


public interface ClientUpdateTasksReceiver extends ClientReceiver {
	public abstract boolean updatedTasks(ArrayList<TaskInfo> tasks);
}
