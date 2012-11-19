

package sk.boinc.mobileboinc.installer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import sk.boinc.mobileboinc.debug.Logging;
import android.util.Log;

import edu.berkeley.boinc.lite.BoincBaseParser;
import edu.berkeley.boinc.lite.BoincParserException;


public class ProjectsClientStateParser extends BoincBaseParser {
	private static final String TAG = "ProjectsFromClientRetriever";

	private ProjectDescriptor mProjectDesc = null;
	private ArrayList<ProjectDescriptor> mProjects = new ArrayList<ProjectDescriptor>();
	
	public ProjectDescriptor[] getProjects() {
		return mProjects.toArray(new ProjectDescriptor[0]); // atrape
	}
	
	public static ProjectDescriptor[] parse(InputStream inputStream) {
		try {
			ProjectsClientStateParser parser = new ProjectsClientStateParser();
			InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
			BoincBaseParser.parse(parser, reader, false);
			return parser.getProjects();
		} catch (BoincParserException e) {
			if (Logging.DEBUG) Log.d(TAG, "Malformed XML:\n" + inputStream);
			else if (Logging.INFO) Log.i(TAG, "Malformed XML");
			return null;
		} catch (IOException e2) {
			if (Logging.ERROR) Log.e(TAG, "I/O Error in XML parsing:\n" + inputStream);
			return null;
		}
	}
	
	@Override
	public void startElement(String localName) {
		super.startElement(localName);
		if (localName.equalsIgnoreCase("project")) {
			mProjectDesc = new ProjectDescriptor();
		}
	}
	
	@Override
	public void endElement(String localName) {
		super.endElement(localName);
		if (localName.equalsIgnoreCase("project")) {
			mProjects.add(mProjectDesc);
			mProjectDesc = null;
		} else if (mProjectDesc != null) {
			if (localName.equalsIgnoreCase("project_name"))
				mProjectDesc.projectName = getCurrentElement();
			else if (localName.equalsIgnoreCase("master_url"))
				mProjectDesc.masterUrl = getCurrentElement();
		}
	}
}
