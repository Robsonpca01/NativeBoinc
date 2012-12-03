
package sk.boinc.nativeboinc.installer;


public class ProjectDistrib {
	public String projectName = "";
	public String projectUrl = "";
	public String version = "";
	public String filename = "";
	public String description = "";
	public String changes = "";
	
	public ProjectDistrib() { }
	
	public boolean binariesProvidedByNativeBOINC() {
		return filename != null && filename.length() != 0;
	}
}
