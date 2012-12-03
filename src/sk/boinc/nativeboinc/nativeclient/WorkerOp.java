
package sk.boinc.nativeboinc.nativeclient;

public class WorkerOp {
	public final static int OP_GET_GLOBAL_PROGRESS = 1;
	public final static int OP_GET_TASKS = 2;
	public final static int OP_GET_PROJECTS = 3;
	public final static int OP_UPDATE_PROJECT_APPS = 4;
	
	public final static WorkerOp GetGlobalProgress = new WorkerOp(OP_GET_GLOBAL_PROGRESS);
	public final static WorkerOp GetTasks = new WorkerOp(OP_GET_TASKS);
	public final static WorkerOp GetProjects = new WorkerOp(OP_GET_PROJECTS);
	
	public final static WorkerOp UpdateProjectApps(String projectUrl) {
		return new WorkerOp(OP_UPDATE_PROJECT_APPS, projectUrl);
	}
	
	public int opCode;
	public String projectUrl;
	
	protected WorkerOp(int opCode) {
		this.opCode = opCode;
		this.projectUrl = null;
	}
	
	protected WorkerOp(int opCode, String projectUrl) {
		this.opCode = opCode;
		this.projectUrl = projectUrl;
	}
	
	@Override
	public boolean equals(Object ob) {
		if (this == null)
			return false;
		if (this == ob)
			return true;
		
		if (ob instanceof WorkerOp) {
			WorkerOp op = (WorkerOp)ob;
			if (this.opCode != op.opCode)
				return false;
			
			if (opCode == OP_UPDATE_PROJECT_APPS) {
				if (projectUrl != null)
					return projectUrl.equals(op.projectUrl);
				else
					return op.projectUrl == null;
			} else
				return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		if (opCode == OP_UPDATE_PROJECT_APPS && projectUrl != null)
			return opCode ^ projectUrl.hashCode();
		return opCode;
	}
	
	@Override
	public String toString() {
		return "["+opCode+","+projectUrl+"]";
	}
}
