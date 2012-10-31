
package sk.boinc.nativeboinc.util;

import java.io.IOException;
import java.util.Collection;

public class SDCardExecs {
	public final native static int openExecsLock(String dirpath, boolean isWrite) throws IOException;
	
	public final native static void readExecs(int fd, Collection<String> coll) throws IOException;
	public final native static boolean checkExecMode(int fd, String filename) throws IOException;
	public final native static void setExecMode(int fd, String filename,
			boolean execMode) throws IOException;
	
	public final native static void closeExecsLock(int fd);
	
	static {
		System.loadLibrary("nativeboinc_utils");
	}
}
