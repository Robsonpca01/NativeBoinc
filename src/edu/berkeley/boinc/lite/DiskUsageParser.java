
package edu.berkeley.boinc.lite;

import java.util.ArrayList;
import java.util.HashMap;

import sk.boinc.mobileboinc.debug.Logging;
import android.util.Log;

public class DiskUsageParser extends BoincBaseParser {

	private static final String TAG = "DiskUsageParser";
	
	private HashMap<String, Double> mDiskUsageMap = new HashMap<String, Double>();
	
	private String mMasterUrl = null;
	private double mDiskUsage = 0.0;
	
	public static boolean parse(String rpcResult, ArrayList<Project> projects) {
		try {
			DiskUsageParser parser = new DiskUsageParser();
			BoincBaseParser.parse(parser, rpcResult, true);
			
			parser.setUpDiskUsage(projects);
			return true;
		}
		catch (BoincParserException e) {
			if (Logging.DEBUG) Log.d(TAG, "Malformed XML:\n" + rpcResult);
			else if (Logging.INFO) Log.i(TAG, "Malformed XML");
			return false;
		}
	}
	
	private void setUpDiskUsage(ArrayList<Project> projects) {
		for (Project project: projects) {
			Double diskUsage = mDiskUsageMap.get(project.master_url);
			if (diskUsage != null)
				project.disk_usage = diskUsage;
			else // unknown disk usage
				project.disk_usage = -1.0;
		}
	}
	
	@Override
	public void startElement(String localName) {
		super.startElement(localName);
		if (localName.equalsIgnoreCase("project")) {
			mMasterUrl = null;
		}
	}
	
	@Override
	public void endElement(String localName) {
		super.endElement(localName);
		try {
			if (localName.equalsIgnoreCase("project")) {
				// Closing tag of <project> - add to vector and be ready for next one
				if (mMasterUrl != null) {
					// master_url is a must
					mDiskUsageMap.put(mMasterUrl, mDiskUsage);
					mMasterUrl = null;
				}
			} else {
				if (localName.equalsIgnoreCase("master_url")) {
					mMasterUrl = getCurrentElement();
				} else if (localName.equalsIgnoreCase("disk_usage")) {
					mDiskUsage = Double.parseDouble(getCurrentElement());
				}
			}
		} catch (NumberFormatException e) {
			if (Logging.INFO) Log.i(TAG, "Exception when decoding " + localName);
		}
	}
}
