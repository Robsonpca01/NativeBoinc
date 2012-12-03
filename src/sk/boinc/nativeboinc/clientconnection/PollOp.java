

package sk.boinc.nativeboinc.clientconnection;


public interface PollOp {
	// polling operation - 
	public static final int POLL_ATTACH_TO_BAM = 0;
	public static final int POLL_SYNC_WITH_BAM = 1;
	public static final int POLL_LOOKUP_ACCOUNT = 2;
	public static final int POLL_CREATE_ACCOUNT = 3;
	public static final int POLL_PROJECT_ATTACH = 4;
	public static final int POLL_PROJECT_CONFIG = 5;
	
	public static final int POLL_BAM_OPERATION_MASK = 1<<0;
	public static final int POLL_LOOKUP_ACCOUNT_MASK = 1<<1;
	public static final int POLL_CREATE_ACCOUNT_MASK = 1<<2;
	public static final int POLL_PROJECT_ATTACH_MASK = 1<<3;
	public static final int POLL_PROJECT_CONFIG_MASK = 1<<4;
	
	public static final int POLL_ALL_MASK = (1<<5)-1;
}
