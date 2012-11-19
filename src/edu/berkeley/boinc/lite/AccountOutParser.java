
package edu.berkeley.boinc.lite;

import sk.boinc.mobileboinc.debug.Logging;
import android.util.Log;


public class AccountOutParser extends BoincBaseParser {
	private static final String TAG = "AccountOutParser";

	private AccountOut mAccountOut = null;
	
	public AccountOut getAccountOut() {
		return mAccountOut;
	}
	
	public static AccountOut parse(String rpcResult) {
		try {
			AccountOutParser parser = new AccountOutParser();
			BoincBaseParser.parse(parser, rpcResult, true);
			return parser.getAccountOut();
		} catch (BoincParserException e) {
			if (Logging.DEBUG) Log.d(TAG, "Malformed XML:\n" + rpcResult);
			else if (Logging.INFO) Log.i(TAG, "Malformed XML");
			return null;
		}
	}
	
	@Override
	public void startElement(String localName) {
		super.startElement(localName);
		if (localName.equalsIgnoreCase("error_num") ||
				localName.equalsIgnoreCase("error_msg") ||
				localName.equalsIgnoreCase("authenticator")) {
			if (mAccountOut==null)
				mAccountOut = new AccountOut();
		}
	}
	
	@Override
	public void endElement(String localName) {
		super.endElement(localName);
		try {
			if (mAccountOut != null) {
				if (localName.equalsIgnoreCase("error_num")) {
					mAccountOut.error_num = Integer.parseInt(getCurrentElement());
				} else if (localName.equalsIgnoreCase("error_msg")) {
					mAccountOut.error_msg = getCurrentElement();
				} else if (localName.equalsIgnoreCase("authenticator")) {
					mAccountOut.authenticator = getCurrentElement();
				}
			}
		} catch (NumberFormatException e) {
			if (Logging.INFO) Log.i(TAG, "Exception when decoding " + localName);
		}
	}
}
