
package edu.berkeley.boinc.lite;

import java.util.ArrayList;

import sk.boinc.nativeboinc.debug.Logging;
import android.util.Log;


public class ProjectListParser extends BoincBaseParser {
	private static final String TAG = "ProjectListParser";
	
	private ArrayList<ProjectListEntry> mProjectList = new ArrayList<ProjectListEntry>();
	private ProjectListEntry mProjectEntry = null;
	private boolean mInsidePlatforms = false;
	
	public ArrayList<ProjectListEntry> getProjectList() {
		return mProjectList;
	}
	
	public static ArrayList<ProjectListEntry> parse(String rpcResult) {
		try {
			ProjectListParser parser = new ProjectListParser();
			BoincBaseParser.parse(parser, rpcResult, true);
			return parser.getProjectList();
		}
		catch (BoincParserException e) {
			if (Logging.DEBUG) Log.d(TAG, "Malformed XML:\n" + rpcResult);
			else if (Logging.INFO) Log.i(TAG, "Malformed XML");
			return null;
		}
	}
	
	@Override
	public void startElement(String localName)  {
		super.startElement(localName);
		if (localName.equalsIgnoreCase("project")) {
			if (Logging.INFO) {
				if (mProjectEntry != null) {
					// previous <project> not closed - dropping it!
					Log.i(TAG, "Dropping unfinished <project> data");
				}
			}
			mProjectEntry = new ProjectListEntry();
		}
		else if (localName.equalsIgnoreCase("platforms")) {
			mInsidePlatforms = true;
		}
	}
	
	@Override
	public void endElement(String localName) {
		super.endElement(localName);
		try {
			if (mProjectEntry != null) {
				// We are inside <project>
				if (localName.equalsIgnoreCase("project")) {
					// Closing tag of <project> - add to vector and be ready for next one
					if (!mProjectEntry.name.equals("")) {
						// master_url is a must
						mProjectList.add(mProjectEntry);
					}
					mProjectEntry = null;
				} else if (localName.equalsIgnoreCase("platforms")) {
					mInsidePlatforms = false;
				} else {
					if (localName.equalsIgnoreCase("name")) {
						if (mInsidePlatforms) {
							/* platform name */
							mProjectEntry.platforms.add(getCurrentElement());
						} else {
							mProjectEntry.name = getCurrentElement();
						}
					} else if (localName.equalsIgnoreCase("url")) {
						mProjectEntry.url = getCurrentElement();
					} else if (localName.equalsIgnoreCase("general_area")) {
						mProjectEntry.general_area = getCurrentElement();
					} else if (localName.equalsIgnoreCase("specific_area")) {
						mProjectEntry.specific_area = getCurrentElement();
					} else if (localName.equalsIgnoreCase("description")) {
						mProjectEntry.description = getCurrentElement();
					} else if (localName.equalsIgnoreCase("home")) {
						mProjectEntry.home = getCurrentElement();
					} else if (localName.equalsIgnoreCase("image")) {
						mProjectEntry.image = getCurrentElement();
					}
				}
			}
		} catch (NumberFormatException e) {
			if (Logging.INFO) Log.i(TAG, "Exception when decoding " + localName);
		}
	}
}
