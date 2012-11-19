
package sk.boinc.mobileboinc.clientconnection;

import java.util.ArrayList;


public interface ClientUpdateTransfersReceiver extends ClientReceiver {
	public abstract boolean updatedTransfers(ArrayList<TransferInfo> transfers);
}
