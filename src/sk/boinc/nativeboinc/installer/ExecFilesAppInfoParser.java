
package sk.boinc.nativeboinc.installer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import sk.boinc.nativeboinc.debug.Logging;
import android.util.Log;

import edu.berkeley.boinc.lite.BoincBaseParser;
import edu.berkeley.boinc.lite.BoincParserException;


public class ExecFilesAppInfoParser extends BoincBaseParser {

	private static final String TAG = "ExecFilesAppInfoParser";
	
	private ArrayList<String> mExecFiles = new ArrayList<String>();
	
	private boolean mInsideFileInfo = false;
	private boolean mIsExecutable = false;
	private String mFilename = null;
	
	public ArrayList<String> getExecFiles() {
		return mExecFiles;
	}
	
	public static ArrayList<String> parse(InputStream result) {
		try {
			ExecFilesAppInfoParser parser = new ExecFilesAppInfoParser();
			InputStreamReader reader = new InputStreamReader(result, "UTF-8");
			BoincBaseParser.parse(parser, reader, false);
			return parser.getExecFiles();
		} catch (BoincParserException e) {
			if (Logging.DEBUG) Log.d(TAG, "Malformed XML:\n" + result);
			else if (Logging.INFO) Log.i(TAG, "Malformed XML");
			return null;
		} catch (IOException e2) {
			if (Logging.ERROR) Log.e(TAG, "I/O Error in XML parsing:\n" + result);
			return null;
		}
	}
	
	@Override
	public void startElement(String localName) {
		super.startElement(localName);
		if (localName.equalsIgnoreCase("file_info")) {
			mInsideFileInfo = true;
		}
	}
	
	@Override
	public void endElement(String localName) {
		super.endElement(localName);
		if (mInsideFileInfo) {
			if (localName.equalsIgnoreCase("file_info")) {
				if (mFilename != null && mIsExecutable) {
					mExecFiles.add(mFilename);
				}
				mInsideFileInfo = false;
				mFilename = null;
				mIsExecutable = false;
			} else if (localName.equalsIgnoreCase("executable"))
				mIsExecutable = true;
			else if (localName.equalsIgnoreCase("name"))
				mFilename = getCurrentElement();
		}
	}
}
