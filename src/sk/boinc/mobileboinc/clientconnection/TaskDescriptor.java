
package sk.boinc.mobileboinc.clientconnection;


public class TaskDescriptor {
	public String projectUrl;
	public String taskName;
	
	public TaskDescriptor(String projectUrl, String taskName) {
		this.projectUrl = projectUrl;
		this.taskName = taskName;
	}
	
	@Override
	public String toString() {
		return "Task:"+projectUrl+":"+taskName;
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (object == null)
			return false;
		if (object instanceof TaskDescriptor) {
			TaskDescriptor taskDesc = (TaskDescriptor)object;
			return this.projectUrl.equals(taskDesc.projectUrl) &&
					this.taskName.equals(taskDesc.taskName);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return projectUrl.hashCode() ^ taskName.hashCode();
	}
}
