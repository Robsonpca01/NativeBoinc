
package sk.boinc.nativeboinc.installer;


public interface InstallerProgressListener extends AbstractInstallerListener {
	public final static int FINISH_PROGRESS = 10000;
	
	/* info: about distrib name
	 * distrib name determine not only distribution and also operation code
	 * please lookup on InstallerService
	 */
	public abstract void onOperation(String distribName, String opDescription);
	public abstract void onOperationProgress(String distribName, String opDescription, int progress);
	public abstract void onOperationCancel(InstallOp installOp, String distribName);
	public abstract void onOperationFinish(InstallOp installOp, String distribName);
}
