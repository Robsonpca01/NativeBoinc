
package edu.berkeley.boinc.nativeboinc;

import sk.boinc.nativeboinc.debug.Logging;
import android.util.Log;
import edu.berkeley.boinc.lite.BoincBaseParser;
import edu.berkeley.boinc.lite.BoincParserException;

public class UpdateProjectAppsReplyParser extends BoincBaseParser {
	private static final String TAG = "UpdateProjectAppsReplyParser";

	private UpdateProjectAppsReply mUPAR;
	
	public UpdateProjectAppsReply getUpdateProjectAppsReply() {
		return mUPAR;
	}
	
	public static UpdateProjectAppsReply parse(String rpcResult) {
		try {
			UpdateProjectAppsReplyParser parser = new UpdateProjectAppsReplyParser();
			BoincBaseParser.parse(parser, rpcResult, true);
			return parser.getUpdateProjectAppsReply();
		} catch (BoincParserException e) {
			if (Logging.DEBUG) Log.d(TAG, "Malformed XML:\n" + rpcResult);
			else if (Logging.INFO) Log.i(TAG, "Malformed XML");
			return null;
		}
	}
	
	@Override
	public void startElement(String localName) {
		super.startElement(localName);
		if (localName.equalsIgnoreCase("update_project_apps_reply")) {
			if (Logging.INFO) { 
				if (mUPAR != null) {
					// previous <update_project_apps_reply> not closed - dropping it!
					Log.i(TAG, "Dropping unfinished <update_project_apps_reply> data");
				}
			}
			mUPAR = new UpdateProjectAppsReply();
		}
	}
	
	@Override
	public void endElement(String localName) {
		super.endElement(localName);
		try {
			if (mUPAR != null) {
				// we are inside <update_project_apps_reply>
				if (localName.equalsIgnoreCase("update_project_apps_reply")) {
					// Closing tag of <update_project_apps_reply> - nothing to do at the moment
				}
				else {
					// Not the closing tag - we decode possible inner tags
					if (localName.equalsIgnoreCase("error_num")) {
						mUPAR.error_num = Integer.parseInt(getCurrentElement());
					} else if (localName.equalsIgnoreCase("message")) {
						mUPAR.messages.add(getCurrentElement());
					}
				}
			}
		} catch (NumberFormatException e) {
			if (Logging.INFO) Log.i(TAG, "Exception when decoding " + localName);
		}
	}
}
