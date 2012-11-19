
package sk.boinc.mobileboinc.clientconnection;

import java.util.ArrayList;


public interface ClientUpdateMessagesReceiver extends ClientReceiver {
	public abstract boolean updatedMessages(ArrayList<MessageInfo> messages);
}
