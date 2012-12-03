
package sk.boinc.nativeboinc.util;

import java.io.File;


public class RuntimeUtils {

	public static int getRealCPUCount() {
		int cpusCount = Runtime.getRuntime().availableProcessors();
		
		for (int i = 0; i < 17; i++) {
			File cpuFile = new File("/sys/devices/system/cpu/cpu"+i);
			if (!cpuFile.exists())
				return Math.max(cpusCount, i);
		}
		
		return Math.max(cpusCount, 17);
	}
}
