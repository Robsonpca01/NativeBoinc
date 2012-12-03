
package sk.boinc.nativeboinc.clientconnection;

import java.util.ArrayList;


public interface ClientUpdateTransfersReceiver extends ClientReceiver {
	public abstract boolean updatedTransfers(ArrayList<TransferInfo> transfers);
}
