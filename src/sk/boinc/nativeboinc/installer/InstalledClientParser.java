
package sk.boinc.nativeboinc.installer;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import sk.boinc.nativeboinc.debug.Logging;
import sk.boinc.nativeboinc.util.BaseParser;
import android.util.Log;
import android.util.Xml;


public class InstalledClientParser extends BaseParser {
	private static final String TAG = "InstalledClientParser";
	
	private InstalledClient mClient = null;
	
	public InstalledClient getInstalledClient() {
		return mClient;
	}
	
	public static InstalledClient parse(InputStream result) {
		try {
			InstalledClientParser parser = new InstalledClientParser();
			Xml.parse(result, Xml.Encoding.UTF_8, parser);
			return parser.getInstalledClient();
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
		if (localName.equalsIgnoreCase("client")) {
			mClient= new InstalledClient();
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
		if (mClient!= null) {
			if (!localName.equalsIgnoreCase("client")) {
				trimEnd();
				if (localName.equalsIgnoreCase("version")) {
					mClient.version = mCurrentElement.toString();
				} else if (localName.equalsIgnoreCase("description")) {
					mClient.description = mCurrentElement.toString();
				} else if (localName.equalsIgnoreCase("changes")) {
					mClient.changes = mCurrentElement.toString();
				} else if (localName.equalsIgnoreCase("fromSDCard")) {
					mClient.fromSDCard = true;
				}
			}
		}
		mElementStarted = false;
	}
}
