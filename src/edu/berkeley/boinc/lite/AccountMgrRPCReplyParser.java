
package edu.berkeley.boinc.lite;

import sk.boinc.nativeboinc.debug.Logging;
import android.util.Log;


public class AccountMgrRPCReplyParser extends BoincBaseParser {
	private static final String TAG = "AccountMgrRPCReplyParser";

	private AccountMgrRPCReply mPAR;
	
	public AccountMgrRPCReply getAccountMgrRPCReply() {
		return mPAR;
	}
	
	public static AccountMgrRPCReply parse(String rpcResult) {
		try {
			AccountMgrRPCReplyParser parser = new AccountMgrRPCReplyParser();
			BoincBaseParser.parse(parser, rpcResult, true);
			return parser.getAccountMgrRPCReply();
		} catch (BoincParserException e) {
			if (Logging.DEBUG) Log.d(TAG, "Malformed XML:\n" + rpcResult);
			else if (Logging.INFO) Log.i(TAG, "Malformed XML");
			return null;
		}
	}
	
	@Override
	public void startElement(String localName) {
		super.startElement(localName);
		if (localName.equalsIgnoreCase("acct_mgr_rpc_reply")) {
			if (Logging.INFO) { 
				if (mPAR != null) {
					// previous <acct_mgr_rpc_reply> not closed - dropping it!
					Log.i(TAG, "Dropping unfinished <acct_mgr_rpc_reply> data");
				}
			}
			mPAR = new AccountMgrRPCReply();
		}
	}
	
	@Override
	public void endElement(String localName) {
		super.endElement(localName);
		try {
			if (mPAR != null) {
				// we are inside <acct_mgr_rpc_reply>
				if (localName.equalsIgnoreCase("acct_mgr_rpc_reply")) {
					// Closing tag of <acct_mgr_rpc_reply> - nothing to do at the moment
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
