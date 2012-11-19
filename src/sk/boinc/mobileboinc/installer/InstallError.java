/**
 * 
 */
package sk.boinc.mobileboinc.installer;


public class InstallError {
	public String distribName;
	public String projectUrl;
	public String errorMessage;
	
	public InstallError(String distribName, String projectUrl, String errorMesage) {
		this.distribName = distribName;
		this.projectUrl = projectUrl;
		this.errorMessage = errorMesage;
	}
}
