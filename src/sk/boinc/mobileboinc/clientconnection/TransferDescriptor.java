
package sk.boinc.mobileboinc.clientconnection;


public class TransferDescriptor {
	public String projectUrl;
	public String fileName;
	
	public TransferDescriptor(String projectUrl, String fileName) {
		this.projectUrl = projectUrl;
		this.fileName = fileName;
	}
	
	@Override
	public String toString() {
		return "Transfer:"+projectUrl+":"+fileName;
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (object == null)
			return false;
		if (object instanceof TransferDescriptor) {
			TransferDescriptor transferDesc = (TransferDescriptor)object;
			return this.projectUrl.equals(transferDesc.projectUrl) &&
					this.fileName.equals(transferDesc.fileName);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return projectUrl.hashCode() ^ fileName.hashCode();
	}
}
