
package edu.berkeley.boinc.lite;

import sk.boinc.nativeboinc.debug.Logging;
import android.util.Log;


public class AccountMgrInfoParser extends BoincBaseParser {
	private static final String TAG = "AccountMgrInfoParser";

	private AccountMgrInfo mAccountMgrInfo = null;
	
	public AccountMgrInfo getAccountMgrInfo() {
		return mAccountMgrInfo;
	}

	public static AccountMgrInfo parse(String rpcResult) {
		try {
			AccountMgrInfoParser parser = new AccountMgrInfoParser();
			BoincBaseParser.parse(parser, rpcResult, true);
			return parser.getAccountMgrInfo();
		} catch (BoincParserException e) {
			if (Logging.DEBUG) Log.d(TAG, "Malformed XML:\n" + rpcResult);
			else if (Logging.INFO) Log.i(TAG, "Malformed XML");
			return null;
		}
	}
	
	@Override
	public void startElement(String localName) {
		super.startElement(localName);
		if (localName.equalsIgnoreCase("acct_mgr_info")) {
			if (Logging.INFO) { 
				if (mAccountMgrInfo != null) {
					// previous <acct_mgr_info> not closed - dropping it!
					Log.i(TAG, "Dropping unfinished <acct_mgr_info> data");
				}
			}
			mAccountMgrInfo = new AccountMgrInfo();
		}
	}
	
	@Override
	public void endElement(String localName) {
		super.endElement(localName);
		try {
			if (mAccountMgrInfo != null) {
				// we are inside <acct_mgr_info>
				if (localName.equalsIgnoreCase("acct_mgr_info")) {
					// Closing tag of <acct_mgr_info> - nothing to do at the moment
				}
				else {
					// Not the closing tag - we decode possible inner tags
					if (localName.equalsIgnoreCase("acct_mgr_name")) {
						mAccountMgrInfo.acct_mgr_name = getCurrentElement();
					} else if (localName.equalsIgnoreCase("acct_mgr_url")) {
						mAccountMgrInfo.acct_mgr_url = getCurrentElement();
					} else if (localName.equalsIgnoreCase("have_credentials")) {
						mAccountMgrInfo.have_credentials = !getCurrentElement().equals("0");
					} else if (localName.equalsIgnoreCase("cookie_required")) {
						mAccountMgrInfo.cookie_required = !getCurrentElement().equals("0");
					} else if (localName.equalsIgnoreCase("cookie_failure_url"))
						mAccountMgrInfo.cookie_failure_url = getCurrentElement();
				}
			}
		} catch (NumberFormatException e) {
			if (Logging.INFO) Log.i(TAG, "Exception when decoding " + localName);
		}
	}
}
