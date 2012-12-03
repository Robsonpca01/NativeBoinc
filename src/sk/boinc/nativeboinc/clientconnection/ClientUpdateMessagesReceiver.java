

package sk.boinc.nativeboinc.clientconnection;

import java.util.ArrayList;


public interface ClientUpdateMessagesReceiver extends ClientReceiver {
	public abstract boolean updatedMessages(ArrayList<MessageInfo> messages);
}
