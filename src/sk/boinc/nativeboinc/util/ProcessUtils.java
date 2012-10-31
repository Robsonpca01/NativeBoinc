package sk.boinc.nativeboinc.util;

public class ProcessUtils {

	public native static int exec(String program, String dirPath, String[] args);
	public native static int execSD(String program, String dirPath, String[] args);
	public native static int waitForProcess(int pid) throws InterruptedException;
	
	static {
		System.loadLibrary("nativeboinc_utils");
	}
}
