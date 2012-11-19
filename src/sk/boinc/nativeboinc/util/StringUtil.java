

package sk.boinc.nativeboinc.util;

import java.util.ArrayList;
import java.util.Iterator;

public class StringUtil {

	public final static String joinString(String delimiter, ArrayList<String> messages) {
		if (messages == null || delimiter == null)
			return null;
		
		StringBuilder builder = new StringBuilder();
		Iterator<String> iter = messages.iterator();
		
		while(iter.hasNext()) {
			builder.append(iter.next());
			if (iter.hasNext())
				builder.append(delimiter);
		}
		return builder.toString();
	}
	
	public final static String normalizeHttpUrl(String url) {
		String out = url.trim();
		
		if (!url.startsWith("http://"))
			out = "http://" + url;
		if (!url.endsWith("/"))
			out = out+"/";
		
		return out;
	}
}
