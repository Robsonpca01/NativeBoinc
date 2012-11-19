
package sk.boinc.mobileboinc.nativeclient;

import edu.berkeley.boinc.nativeboinc.ExtendedRpcClient;
import sk.boinc.mobileboinc.debug.Logging;
import android.content.Context;
import android.os.ConditionVariable;
import android.os.Looper;
import android.util.Log;

public class NativeBoincWorkerThread extends Thread {
	private final static String TAG = "NativeBoincWorkerThread";
	
	private NativeBoincService.ListenerHandler mListenerHandler;
	private ConditionVariable mLock;
	private Context mContext;
	private NativeBoincWorkerHandler mHandler;
	private ExtendedRpcClient mRpcClient;
	
	public NativeBoincWorkerThread(NativeBoincService.ListenerHandler listenerHandler, final Context context,
			final ConditionVariable lock, final ExtendedRpcClient rpcClient) {
		mListenerHandler = listenerHandler;
		mLock = lock;
		mContext = context;
		mRpcClient = rpcClient;
		setDaemon(true);
	}
	
	@Override
	public void run() {
		if (Logging.DEBUG) Log.d(TAG, "run() - Started " + Thread.currentThread().toString());
		Looper.prepare();
		
		mHandler = new NativeBoincWorkerHandler(mContext, mListenerHandler, mRpcClient);
		
		if (mLock != null) {
			mLock.open();
			mLock = null;
		}
		
		// cleanup variable dependencies
		mListenerHandler = null;
		mContext = null;
		
		
		Looper.loop();
		
		if (mLock != null) {
			mLock.open();
			mLock = null;
		}

		mRpcClient = null;
		mHandler.destroy();
		mHandler = null;
		if (Logging.DEBUG) Log.d(TAG, "run() - Finished" + Thread.currentThread().toString());
	}
	
	public void getGlobalProgress(final int channelId) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mHandler.getGlobalProgress(channelId);
			}
		});
	}
	
	public void getTasks(final int channelId) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mHandler.getTasks(channelId);
			}
		});
	}
	
	public void getProjects(final int channelId) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mHandler.getProjects(channelId);
			}
		});
	}
	
	public void shutdownClient() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mHandler.shutdownClient();
			}
		});
	}
	
	public void updateProjectApps(final int channelId, final String projectUrl) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mHandler.updateProjectApps(channelId, projectUrl);
			}
		});
	}
	
	public void stopThread() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (Logging.DEBUG) Log.d(TAG, "Quit message received, stopping " + Thread.currentThread().toString());
				Looper.myLooper().quit();
			}
		});
	}
}
