
package sk.boinc.nativeboinc.util;


public class FileUtils {
	public final static String joinBaseAndPath(String base, String filePath) {
		String outPath;
		if (base.endsWith("/")) {
			if (filePath.startsWith("/"))
				outPath = base+filePath.substring(1);
			else
				outPath = base+filePath;
		} else {
			if (filePath.startsWith("/"))
				outPath = base+filePath;
			else
				outPath = base+"/"+filePath;
		}
		return outPath;
	}
}
