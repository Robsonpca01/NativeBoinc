/* 
 * AndroBOINC - BOINC Manager for Android
 * Copyright (C) 2010, Pavol Michalec
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package edu.berkeley.boinc.lite;

import sk.boinc.mobileboinc.debug.Logging;
import android.util.Log;

public class MessageCountParser extends BoincBaseParser {
	private static final String TAG = "MessageCountParser";

	private boolean mParsed = false;
	private boolean mInReply = false;
	private int mSeqno = -1;
	
	// Disable direct instantiation of this class
	private MessageCountParser() {}

	public final int seqno() {
		return mSeqno;
	}

	public static int getSeqno(String reply) {
		try {
			MessageCountParser parser = new MessageCountParser();
			BoincBaseParser.parse(parser, reply, true);
			return parser.seqno();
		}
		catch (BoincParserException e) {
			if (Logging.DEBUG) Log.d(TAG, "Malformed XML:\n" + reply);
			else if (Logging.INFO) Log.i(TAG, "Malformed XML");
			return -1;
		}
	}

	@Override
	public void startElement(String localName) {
		super.startElement(localName);
		if (localName.equalsIgnoreCase("boinc_gui_rpc_reply")) {
			mInReply = true;
		}
	}

	@Override
	public void endElement(String localName) {
		super.endElement(localName);

		try {
			if (localName.equalsIgnoreCase("boinc_gui_rpc_reply")) {
				mInReply = false;
			}
			else if (mInReply && !mParsed) {
				if (localName.equalsIgnoreCase("seqno")) {
					mSeqno = Integer.parseInt(getCurrentElement());
					mParsed = true;
				}
			}
		}
		catch (NumberFormatException e) {
			if (Logging.INFO) Log.i(TAG, "Exception when decoding " + localName);
		}
	}
}
