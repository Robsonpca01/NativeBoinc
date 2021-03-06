
package sk.boinc.nativeboinc.clientconnection;

import edu.berkeley.boinc.lite.AccountMgrInfo;


public interface ClientAccountMgrReceiver extends ClientPollReceiver {
	public abstract boolean onAfterAccountMgrRPC();
	public abstract boolean currentBAMInfo(AccountMgrInfo accountMgrInfo);
}
