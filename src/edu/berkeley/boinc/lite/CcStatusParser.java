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

public class CcStatusParser extends BoincBaseParser {
	private static final String TAG = "CcStatusParser";

	private CcStatus mCcStatus;

	
	public final CcStatus getCcStatus() {
		return mCcStatus;
	}

	public static CcStatus parse(String rpcResult) {
		try {
			CcStatusParser parser = new CcStatusParser();
			BoincBaseParser.parse(parser, rpcResult, true);
			return parser.getCcStatus();
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
		if (localName.equalsIgnoreCase("cc_status")) {
			if (Logging.INFO) { 
				if (mCcStatus != null) {
					// previous <cc_status> not closed - dropping it!
					Log.i(TAG, "Dropping unfinished <cc_status> data");
				}
			}
			mCcStatus = new CcStatus();
		}
	}

	@Override
	public void endElement(String localName) {
		super.endElement(localName);
		try {
			if (mCcStatus != null) {
				// We are inside <cc_status>
				if (localName.equalsIgnoreCase("cc_status")) {
					// Closing tag of <cc_status> - nothing to do at the moment
				}
				else {
					// Not the closing tag - we decode possible inner tags
					if (localName.equalsIgnoreCase("task_mode")) {
						mCcStatus.task_mode = Integer.parseInt(getCurrentElement());
					}
					else if (localName.equalsIgnoreCase("task_mode_perm")) {
						mCcStatus.task_mode_perm = Integer.parseInt(getCurrentElement());
					}
					else if (localName.equalsIgnoreCase("task_mode_delay")) {
						mCcStatus.task_mode_delay = Double.parseDouble(getCurrentElement());
					}
					else if (localName.equalsIgnoreCase("task_suspend_reason")) {
						mCcStatus.task_suspend_reason = Integer.parseInt(getCurrentElement());
					}
					if (localName.equalsIgnoreCase("network_mode")) {
						mCcStatus.network_mode = Integer.parseInt(getCurrentElement());
					}
					else if (localName.equalsIgnoreCase("network_mode_perm")) {
						mCcStatus.network_mode_perm = Integer.parseInt(getCurrentElement());
					}
					else if (localName.equalsIgnoreCase("network_mode_delay")) {
						mCcStatus.network_mode_delay = Double.parseDouble(getCurrentElement());
					}
					else if (localName.equalsIgnoreCase("network_suspend_reason")) {
						mCcStatus.network_suspend_reason = Integer.parseInt(getCurrentElement());
					}
					else if (localName.equalsIgnoreCase("network_status")) {
						mCcStatus.network_status = Integer.parseInt(getCurrentElement());
					}
					else if (localName.equalsIgnoreCase("ams_password_error")) {
						String current = getCurrentElement();
						if (current.length() > 1) {
							mCcStatus.ams_password_error = (0 != Integer.parseInt(current));
						}
						else {
							mCcStatus.ams_password_error = true;
						}
					}
					else if (localName.equalsIgnoreCase("manager_must_quit")) {
						String current = getCurrentElement();
						if (current.length() > 1) {
							mCcStatus.manager_must_quit = (0 != Integer.parseInt(current));
						}
						else {
							mCcStatus.manager_must_quit = true;
						}
					}
					else if (localName.equalsIgnoreCase("disallow_attach")) {
						String current = getCurrentElement();
						if (current.length() > 1) {
							mCcStatus.disallow_attach = (0 != Integer.parseInt(current));
						}
						else {
							mCcStatus.disallow_attach = true;
						}
					}
					else if (localName.equalsIgnoreCase("simple_gui_only")) {
						String current = getCurrentElement();
						if (current.length() > 1) {
							// handling stupid bug ?? (someone added 's' to integer)
							String toParse = current;
							if (toParse.endsWith("s"))
								toParse = toParse.substring(0, toParse.length()-1);
							mCcStatus.simple_gui_only = (0 != Integer.parseInt(toParse));
						}
						else {
							mCcStatus.simple_gui_only = true;
						}
					}
				}
			}
		}
		catch (NumberFormatException e) {
			if (Logging.INFO) Log.i(TAG, "Exception when decoding " + localName);
		}
	}
}
