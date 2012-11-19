
package edu.berkeley.boinc.lite;

import sk.boinc.mobileboinc.debug.Logging;
import android.util.Log;

public class ProjectConfigParser extends BoincBaseParser {
	private static final String TAG = "ProjectConfigParser";
	
	private ProjectConfig mProjectConfig = null;
	private boolean mInsidePlatforms = false;
	
	public ProjectConfig getProjectConfig() {
		return mProjectConfig;
	}
	
	public static ProjectConfig parse(String rpcResult) {
		try {
			ProjectConfigParser parser = new ProjectConfigParser();
			BoincBaseParser.parse(parser, rpcResult, true);
			return parser.getProjectConfig();
		}
		catch (BoincParserException e) {
			if (Logging.DEBUG) Log.d(TAG, "Malformed XML:\n" + rpcResult);
			else if (Logging.INFO) Log.i(TAG, "Malformed XML");
			return null;
		}
	}
	
	@Override
	public void startElement(String localName) {
		super.startElement(localName);
		if (localName.equalsIgnoreCase("project_config")) {
			if (Logging.INFO) {
				if (mProjectConfig != null) {
					// previous <project_config> not closed - dropping it!
					Log.i(TAG, "Dropping unfinished <project_config> data");			
				}
			}
			mProjectConfig = new ProjectConfig();
		} else if (localName.equalsIgnoreCase("platforms")) {
			mInsidePlatforms = true;
		}
	}
	
	@Override
	public void endElement(String localName) {
		super.endElement(localName);
		try {
			if (mProjectConfig != null) {
				if (localName.equalsIgnoreCase("project_config")) {
					// Closing tag of <project_config> - nothing to do at the moment
				} else if (localName.equalsIgnoreCase("platforms")) {
					mInsidePlatforms = false;
				}
				else {
					// Not the closing tag - we decode possible inner tags
					if (localName.equalsIgnoreCase("error_num")) {
						mProjectConfig.error_num = Integer.parseInt(getCurrentElement());
					} else if (localName.equalsIgnoreCase("error_msg")) {
						mProjectConfig.error_msg = getCurrentElement();
					} else if (localName.equalsIgnoreCase("name")) {
						mProjectConfig.name = getCurrentElement();
					} else if (localName.equalsIgnoreCase("platform_name") && mInsidePlatforms) {
						mProjectConfig.platforms.add(getCurrentElement());
					} else if (localName.equalsIgnoreCase("master_url")) {
						mProjectConfig.master_url = getCurrentElement();
					} else if (localName.equalsIgnoreCase("local_revision")) {
						mProjectConfig.local_revision = Integer.parseInt(getCurrentElement());
					} else if (localName.equalsIgnoreCase("min_passwd_length")) {
						mProjectConfig.min_passwd_length = Integer.parseInt(getCurrentElement());
					} else if (localName.equalsIgnoreCase("account_manager")) {
						mProjectConfig.account_manager = Integer.parseInt(getCurrentElement())!=0;
					} else if (localName.equalsIgnoreCase("use_username")) {
						mProjectConfig.use_username = Integer.parseInt(getCurrentElement())!=0;
					} else if (localName.equalsIgnoreCase("account_creation_disabled")) {
						mProjectConfig.account_creation_disabled = true;
					} else if (localName.equalsIgnoreCase("client_account_creation_disabled")) {
						mProjectConfig.client_account_creation_disabled = true;
					} else if (localName.equalsIgnoreCase("sched_stopped")) {
						mProjectConfig.sched_stopped = Integer.parseInt(getCurrentElement())!=0;
					} else if (localName.equalsIgnoreCase("web_stopped")) {
						mProjectConfig.web_stopped = Integer.parseInt(getCurrentElement())!=0;
					} else if (localName.equalsIgnoreCase("min_client_version")) {
						mProjectConfig.min_client_version = Integer.parseInt(getCurrentElement());
					} else if (localName.equalsIgnoreCase("terms_of_use")) {
						mProjectConfig.terms_of_use = getCurrentElement();
					}
				}
			}
		} catch (NumberFormatException e) {
			if (Logging.INFO) Log.i(TAG, "Exception when decoding " + localName);
		}
	}
}
