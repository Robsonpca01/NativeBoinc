
package sk.boinc.mobileboinc.installer;

import sk.boinc.mobileboinc.debug.Logging;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;


public class ResourcesLocker {
	private static final String TAG = "ResourcesLocker";
	
	private static final String PARTIALLOCK_NAME = "InstallerPartialLock";
	private static final String WIFILOCK_NAME = "InstallerWifiLock";
	
	private WifiLock mWifiLock = null;
	private WakeLock mPartialLock = null;
	
	private int mNestedWakeCounter = 0;
	private int mNestedWifiCounter = 0;
	
	public ResourcesLocker(Context context) {
		/* partial lock */
		PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
		mPartialLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PARTIALLOCK_NAME);
		/* wifi resources */
		WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		mWifiLock = wifiManager.createWifiLock(WIFILOCK_NAME);
	}
	
	public void destroy() {
		/* release locks */
		if (mPartialLock.isHeld())
			mPartialLock.release();
		if (mWifiLock.isHeld())
			mWifiLock.release();
		
		mPartialLock = null;
		mWifiLock = null;
	}
	
	public synchronized void acquireAllLocks() {
		mNestedWakeCounter++;
		if (Logging.DEBUG) Log.d(TAG, "Acquire partial lock:"+mNestedWakeCounter);
		
		if (mNestedWakeCounter == 1)
			mPartialLock.acquire();
		
		mNestedWifiCounter++;
		if (Logging.DEBUG) Log.d(TAG, "Acquire WiFi lock:"+mNestedWifiCounter);
		
		if (mNestedWifiCounter == 1)
			mWifiLock.acquire();
	}
	
	public synchronized void acquirePartialLock() {
		mNestedWakeCounter++;
		if (Logging.DEBUG) Log.d(TAG, "Acquire partial lock:"+mNestedWakeCounter);
		
		if (mNestedWakeCounter == 1)
			mPartialLock.acquire();
	}
	
	public synchronized void releaseAllLocks() {
		mNestedWakeCounter--;
		if (Logging.DEBUG) Log.d(TAG, "Release partial lock:"+mNestedWakeCounter);
		
		if (mNestedWakeCounter == 0)
			mPartialLock.release();
		
		mNestedWifiCounter--;
		if (Logging.DEBUG) Log.d(TAG, "Release WiFi lock:"+mNestedWifiCounter);
		
		if (mNestedWifiCounter == 0)
			mWifiLock.release();
	}
	
	public synchronized void releasePartialLocks() {
		mNestedWakeCounter--;
		if (Logging.DEBUG) Log.d(TAG, "Release partial lock:"+mNestedWakeCounter);
		
		if (mNestedWakeCounter == 0)
			mPartialLock.release();
	}
}
