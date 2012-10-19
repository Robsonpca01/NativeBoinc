
package edu.berkeley.boinc.lite;

import java.util.ArrayList;


public class ProjectConfig {
	public int error_num = 0;
	public String error_msg = "";
	
	public String name = "";
	public String master_url = "";
	public int local_revision = 0;
	public int min_passwd_length = 0;
	public boolean account_manager = false;
	public boolean use_username = false;
	public boolean account_creation_disabled = false;
	public boolean client_account_creation_disabled = false;
	public boolean sched_stopped = false;
	public boolean web_stopped = false;
	public int min_client_version = 0;
	public String terms_of_use = "";
	public ArrayList<String> platforms = new ArrayList<String>();
}
