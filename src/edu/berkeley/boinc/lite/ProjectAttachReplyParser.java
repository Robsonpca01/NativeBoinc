
package edu.berkeley.boinc.lite;

import sk.boinc.mobileboinc.debug.Logging;
import android.util.Log;


public class ProjectAttachReplyParser extends BoincBaseParser {
	private static final String TAG = "ProjectAttachReplyParser";

	private ProjectAttachReply mPAR;
	
	public ProjectAttachReply getProjectAttachReply() {
		return mPAR;
	}
	
	public static ProjectAttachReply parse(String rpcResult) {
		try {
			ProjectAttachReplyParser parser = new ProjectAttachReplyParser();
			BoincBaseParser.parse(parser, rpcResult, true);
			return parser.getProjectAttachReply();
		} catch (BoincParserException  e) {
			if (Logging.DEBUG) Log.d(TAG, "Malformed XML:\n" + rpcResult);
			else if (Logging.INFO) Log.i(TAG, "Malformed XML");
			return null;
		}
	}
	
	@Override
	public void startElement(String localName) {
		super.startElement(localName);
		if (localName.equalsIgnoreCase("project_attach_reply")) {
			if (Logging.INFO) { 
				if (mPAR != null) {
					// previous <project_attach_reply> not closed - dropping it!
					Log.i(TAG, "Dropping unfinished <project_attach_reply> data");
				}
			}
			mPAR = new ProjectAttachReply();
		}
	}
	
	@Override
	public void endElement(String localName) {
		super.endElement(localName);
		try {
			if (mPAR != null) {
				// we are inside <project_attach_reply>
				if (localName.equalsIgnoreCase("project_attach_reply")) {
					// Closing tag of <project_attach_reply> - nothing to do at the moment
				}
				else {
					// Not the closing tag - we decode possible inner tags
					if (localName.equalsIgnoreCase("error_num")) {
						mPAR.error_num = Integer.parseInt(getCurrentElement());
					} else if (localName.equalsIgnoreCase("message")) {
						mPAR.messages.add(getCurrentElement());
					}
				}
			}
		} catch (NumberFormatException e) {
			if (Logging.INFO) Log.i(TAG, "Exception when decoding " + localName);
		}
	}
}
