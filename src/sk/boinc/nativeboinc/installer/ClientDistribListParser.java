
package sk.boinc.nativeboinc.installer;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import sk.boinc.nativeboinc.debug.Logging;
import sk.boinc.nativeboinc.util.BaseParser;
import android.util.Log;
import android.util.Xml;


public class ClientDistribListParser extends BaseParser {
	private static final String TAG = "ProjectDistribParser";
	
	private ClientDistrib mClientDistrib = null;
	
	public ClientDistrib getClientDistribs() {
		return mClientDistrib;
	}
	
	public static ClientDistrib parse(InputStream result) {
		try {
			ClientDistribListParser parser = new ClientDistribListParser();
			Xml.parse(result, Xml.Encoding.UTF_8, parser);
			return parser.getClientDistribs();
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
			mClientDistrib = new ClientDistrib();
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
		if (mClientDistrib != null) {
			if (localName.equalsIgnoreCase("client")) {
				// Closing tag of <client> - add to vector and be ready for next one
			} else {
				trimEnd();
				if (localName.equalsIgnoreCase("version")) {
					mClientDistrib.version = mCurrentElement.toString();
				} else if (localName.equalsIgnoreCase("file")) {
					mClientDistrib.filename = mCurrentElement.toString();
				} else if (localName.equalsIgnoreCase("description")) {
					mClientDistrib.description = mCurrentElement.toString();
				} else if (localName.equalsIgnoreCase("changes")) {
					mClientDistrib.changes = mCurrentElement.toString();
				}
			}
		}
		mElementStarted = false;
	}
}
