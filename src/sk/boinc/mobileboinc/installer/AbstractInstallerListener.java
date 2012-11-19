

package sk.boinc.mobileboinc.installer;


public interface AbstractInstallerListener {
	public abstract int getInstallerChannelId();
	
	/* info: about distrib name
	 * distrib name determine not only distribution and also operation code
	 * please lookup on InstallerService
	 */
	public abstract void onChangeInstallerIsWorking(boolean isWorking);
	public abstract boolean onOperationError(InstallOp installOp, String distribName, String errorMessage);
}
