

package sk.boinc.mobileboinc.installer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import sk.boinc.mobileboinc.debug.Logging;
import sk.boinc.nativeboinc.util.BaseParser;
import android.util.Log;
import android.util.Xml;

public class InstalledDistribListParser extends BaseParser {
	private static final String TAG = "InstalledDistribParser";
	
	private ArrayList<InstalledDistrib> mInstalledDistribs = null;
	private InstalledDistrib mDistrib = null;
	
	public ArrayList<InstalledDistrib> getInstalledDistribs() {
		return mInstalledDistribs;
	}

	public static ArrayList<InstalledDistrib> parse(InputStream result) {
		try {
			InstalledDistribListParser parser = new InstalledDistribListParser();
			Xml.parse(result, Xml.Encoding.UTF_8, parser);
			return parser.getInstalledDistribs();
		} catch (SAXException e) {
			if (Logging.DEBUG) Log.d(TAG, "Malformed XML:\n" + result);
			else if (Logging.INFO) Log.i(TAG, "Malformed XML");
			return null;
		} catch (IOException e2) {
			if (Logging.ERROR) Log.e(TAG, "I/O Error in XML parsing:\n" + result);
			return null;
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		if (localName.equalsIgnoreCase("distribs")) {
			mInstalledDistribs = new ArrayList<InstalledDistrib>();
		} else if (localName.equalsIgnoreCase("project")) {
			mDistrib = new InstalledDistrib();
		} else {
			// Another element, hopefully primitive and not constructor
			// (although unknown constructor does not hurt, because there will be primitive start anyway)
			mElementStarted = true;
			mCurrentElement.setLength(0);
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		if (mDistrib != null) {
			if (localName.equalsIgnoreCase("project")) {
				// Closing tag of <project> - add to vector and be ready for next one
				if (!mDistrib.projectUrl.equals("")) {
					// master_url is a must
					mInstalledDistribs.add(mDistrib);
				}
				mDistrib = null;
			} else {
				trimEnd();
				if (localName.equalsIgnoreCase("name")) {
					mDistrib.projectName = mCurrentElement.toString();
				} else if (localName.equalsIgnoreCase("url")) {
					mDistrib.projectUrl = mCurrentElement.toString();
				} else if (localName.equalsIgnoreCase("version")) {
					mDistrib.version = mCurrentElement.toString();
				} else if (localName.equalsIgnoreCase("file")) {
					mDistrib.files.add(mCurrentElement.toString());
				} else if (localName.equalsIgnoreCase("description")) {
					mDistrib.description = mCurrentElement.toString();
				} else if (localName.equalsIgnoreCase("changes")) {
					mDistrib.changes = mCurrentElement.toString();
				} else if (localName.equalsIgnoreCase("fromSDCard")) {
					mDistrib.fromSDCard = true;
				}
			}
		}
		mElementStarted = false;
	}
}
