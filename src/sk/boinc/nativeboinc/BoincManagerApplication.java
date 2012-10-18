
package sk.boinc.nativeboinc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.boinc.nativeboinc.debug.Logging;
import sk.boinc.nativeboinc.nativeclient.NativeBoincStateListener;
import sk.boinc.nativeboinc.nativeclient.NativeBoincService;
import sk.boinc.nativeboinc.nativeclient.NativeBoincUtils;
import sk.boinc.nativeboinc.service.ConnectionManagerService;
import sk.boinc.nativeboinc.util.ActivityVisibilityTracker;
import sk.boinc.nativeboinc.util.NativeClientAutostart;
import sk.boinc.nativeboinc.util.PreferenceName;
import sk.boinc.nativeboinc.widget.RefreshWidgetHandler;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Global application point which can be used by any activity.<p>
 * It handles some common stuff:
 * <ul>
 * <li>sets the default values on preferences
 * <li>provides application-wide constants
 * <li>provides basic information about application
 * </ul>
 */
public class BoincManagerApplication extends Application implements NativeBoincStateListener {
	
	private static final String TAG = "BoincManagerApplication";

	public static final String GLOBAL_ID = "sk.boinc.androboinc";
	public static final int DEFAULT_PORT = 31416;

	private static final int READ_BUF_SIZE = 2048;
	private static final int LICENSE_TEXT_SIZE = 37351;

	private char[] mReadBuffer = new char[READ_BUF_SIZE];
	private StringBuilder mStringBuilder = new StringBuilder(LICENSE_TEXT_SIZE);
	
	public final static String UPDATE_PROGRESS = "UPDATE_PROGRESS";
	public final static String UPDATE_TASKS = "UPDATE_TASKS";
	
	public final static int INSTALLER_NO_STAGE = 0;
	public final static int INSTALLER_CLIENT_STAGE = 1;
	public final static int INSTALLER_CLIENT_INSTALLING_STAGE = 2;
	public final static int INSTALLER_PROJECT_STAGE = 3;
	public final static int INSTALLER_PROJECT_INSTALLING_STAGE = 4;
	public final static int INSTALLER_FINISH_STAGE = 5;
	private int mInstallerStage = INSTALLER_NO_STAGE;
	
	private NativeBoincService mRunner = null;
	
	private NotificationController mNotificationController = null;
	
	private boolean mDoStartClientAfterBind = false;
	
	/* activity visibility tracker */
	public static class MyActivityVisibilityTracker extends ActivityVisibilityTracker {
		public ConnectionManagerService mConnectionManager = null;
		
		public void registerConnectionManager(ConnectionManagerService service) {
			mConnectionManager = service;
		}
		
		public void unregisterConnectionManager() {
			mConnectionManager = null;
		}
		
		public void onShowActivity() {
			if (mConnectionManager != null)
				mConnectionManager.acquireLockScreenOn();
		}
		
		public void onHideActivity() {
			if (mConnectionManager != null)
				mConnectionManager.releaseLockScreenOn();
		}
	};
	
	private MyActivityVisibilityTracker mVisibilityTracker = null;
	
	// restart after reinstall handling
	private Object mRestartAfterReinstallSync = new Object();
	private boolean mRunRestartAfterReinstall = false;
	private boolean mRestartedAfterReinstall = false;
	
	private boolean mFirstStartingAtAppStartup = false;
	
	private SharedPreferences mGlobalPrefs = null;
	
	private static final int SECOND_TRY_START_DELAY = 10000;
	
	private class SecondStartTryHandler extends Handler {
		public void tryStartAgain() {
			synchronized(BoincManagerApplication.this) {
				postDelayed(new Runnable() {
					@Override
					public void run() {
						// second try to start
						autostartClient();
					}
				}, SECOND_TRY_START_DELAY);
			}
		}
	}
	
	private SecondStartTryHandler mSecondTryStartHandler = null;
	
	private Handler mStandardHandler = null;
	
	/* max number of tries */
	private static final int MAX_AUTOSTART_TRIES_N = 3;
	
	private int mAutostartTrialNumber = 0;
	
	private RefreshWidgetHandler mRefreshWidgetHandler = null;
	
	private ServiceConnection mRunnerServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mRunner = ((NativeBoincService.LocalBinder)service).getService();
			if (Logging.DEBUG) Log.d(TAG, "runner.onServiceConnected()");
			// service automatically adds application object to listeners
			mRunner.addNativeBoincListener(BoincManagerApplication.this);
			mRunner.addNativeBoincListener(mRefreshWidgetHandler);
			mRunner.addMonitorListener(mRefreshWidgetHandler);
			
			if (!mRunner.isRun()) {
				if (mDoStartClientAfterBind && !mRunner.isRun()) {
					if (Logging.DEBUG) Log.d(TAG, "Start client after bind runner");
					mDoStartClientAfterBind = false;
					mRunner.startClient(false);
				}
			} else {
				// trigger listener
				onClientStart();
			}
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			if (mRunner != null) {
				mRunner.removeNativeBoincListener(mRefreshWidgetHandler);
				mRunner.removeMonitorListener(mRefreshWidgetHandler);
				mRunner.removeNativeBoincListener(BoincManagerApplication.this);
			}
			mRunner = null;
			if (Logging.DEBUG) Log.d(TAG, "runner.onServiceDisconnected()");
		}
	};
	
	private void doBindRunnerService() {
		bindService(new Intent(BoincManagerApplication.this, NativeBoincService.class),
				mRunnerServiceConnection, Context.BIND_AUTO_CREATE);
	}
	
	private void doUnbindRunnerService() {
		if (Logging.DEBUG) Log.d(TAG, "Undind runner service");
		unbindService(mRunnerServiceConnection);
		mRunner.removeNativeBoincListener(mRefreshWidgetHandler);
		mRunner.removeMonitorListener(mRefreshWidgetHandler);
		mRunner.removeNativeBoincListener(this);
		mRunner = null;
	}
	
	public void bindRunnerService() {
		if (mRunner == null)
			doBindRunnerService();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		if (Logging.DEBUG) Log.d(TAG, "onCreate()");
		PreferenceManager.setDefaultValues(this, R.xml.manage_client, false);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		NativeBoincUtils.killZombieClient(BoincManagerApplication.this);
		
		mNotificationController = new NotificationController(this);
		
		mGlobalPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mInstallerStage = mGlobalPrefs.getInt(PreferenceName.INSTALLER_STAGE, INSTALLER_NO_STAGE);
		
		mSecondTryStartHandler = new SecondStartTryHandler();
		mRefreshWidgetHandler = new RefreshWidgetHandler(this);
		mVisibilityTracker = new MyActivityVisibilityTracker();
		
		mStandardHandler = new Handler();
		
		// register refreshWidgetHandler as preferences change listener
		mGlobalPrefs.registerOnSharedPreferenceChangeListener(mRefreshWidgetHandler);
		
		// initialize news service
	
		if (NativeClientAutostart.isAutostartsAtAppStartup(mGlobalPrefs))
			autostartClient();
	}
	
	public void autostartClient() {
		if (Logging.DEBUG) Log.d(TAG, "Autostart client");
		if (!mFirstStartingAtAppStartup) {
			mFirstStartingAtAppStartup = true;
			
			mAutostartTrialNumber++;
			if (Logging.DEBUG) Log.d(TAG, "Autostart trial "+mAutostartTrialNumber);
			
			if (mAutostartTrialNumber <= MAX_AUTOSTART_TRIES_N) {
				if (mRunner == null)
					bindRunnerAndStart();
				else // if bound only starts client
					mRunner.startClient(false);
			}
		}
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		if (Logging.DEBUG) Log.d(TAG, "onTerminate() - finished");
	}

	@Override
	public void onLowMemory() {
		if (Logging.DEBUG) Log.d(TAG, "onLowMemory()");
		// Let's free what we do not need essentially
		mStringBuilder = null; // So garbage collector will free the memory
		mReadBuffer = null;
		super.onLowMemory();
	}
	
	/* get autorefresh handler */
	public RefreshWidgetHandler getRefreshWidgetHandler() {
		return mRefreshWidgetHandler;
	}

	/**
	 * Finds whether this is the application was upgraded recently.
	 * It also marks the status in preferences, so even if first call of this
	 * method after upgrade returns true, all subsequent calls will return false.
	 * 
	 * @return true if this is first call of this method after application upgrade, 
	 *         false if this application version was already run previously
	 */
	public boolean getJustUpgradedStatus() {
		SharedPreferences globalPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		int upgradeInfoShownVersion = globalPrefs.getInt(PreferenceName.UPGRADE_INFO_SHOWN_VERSION, 0);
		int currentVersion = 0;
		try {
			currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		}
		catch (NameNotFoundException e) {
			if (Logging.ERROR) Log.e(TAG, "Cannot retrieve application version");
			return false;
		}
		if (Logging.DEBUG) Log.d(TAG, "currentVersion=" + currentVersion + ", upgradeInfoShownVersion=" + upgradeInfoShownVersion);
		if (currentVersion == upgradeInfoShownVersion) {
			// NOT upgraded, we shown info already
			return false;
		}
		// Just upgraded; mark the status in preferences
		if (Logging.DEBUG) Log.d(TAG, "First run after upgrade: currentVersion=" + currentVersion + ", upgradeInfoShownVersion=" + upgradeInfoShownVersion);
		SharedPreferences.Editor editor = globalPrefs.edit();
		editor.putInt(PreferenceName.UPGRADE_INFO_SHOWN_VERSION, currentVersion).commit();
		return true;
	}

	public String getApplicationVersion() {
		if (mStringBuilder == null) mStringBuilder = new StringBuilder(32);
		mStringBuilder.setLength(0);
		mStringBuilder.append(getString(R.string.app_name));
		mStringBuilder.append(" v");
		try {
			mStringBuilder.append(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		}
		catch (NameNotFoundException e) {
			if (Logging.ERROR) Log.e(TAG, "Cannot retrieve application version");
			mStringBuilder.setLength(mStringBuilder.length() - 2); // Truncate " v" set above
		}		
		return mStringBuilder.toString();
	}

	public void setAboutText(TextView text) {
		text.setText(getString(R.string.aboutText, getApplicationVersion()));
		String httpURL = "http://";
		// Link to BOINC.SK page
		Pattern boincskText = Pattern.compile("BOINC\\.SK");
		TransformFilter boincskTransformer = new TransformFilter() {
			@Override
			public String transformUrl(Matcher match, String url) {
				return url.toLowerCase() + "/";
			}
		};
		Linkify.addLinks(text, boincskText, httpURL, null, boincskTransformer);
		// Link to GPLv3 license
		Pattern gplText = Pattern.compile("GPLv3");
		TransformFilter gplTransformer = new TransformFilter() {
			@Override
			public String transformUrl(Matcher match, String url) {
				return "www.gnu.org/licenses/gpl-3.0.txt";
			}
		};
		Linkify.addLinks(text, gplText, httpURL, null, gplTransformer);
	}

	public void setLicenseText(TextView text) {
		text.setText(Html.fromHtml(readRawText(R.raw.license)));
		Linkify.addLinks(text, Linkify.ALL);
	}


	
	public void setHtmlText(TextView text, String content) {
		mStringBuilder.setLength(0);
		mStringBuilder.append("<html><body>");
		mStringBuilder.append(content);
		mStringBuilder.append("</body></html>");
		text.setText(Html.fromHtml(mStringBuilder.toString()));
	}
	
	public void setHtmlText(TextView text, String header, String content) {
		mStringBuilder.setLength(0);
		mStringBuilder.append("<html><body>");
		mStringBuilder.append(header);
		mStringBuilder.append(":");
		mStringBuilder.append(content);
		mStringBuilder.append("</body></html>");
		text.setText(Html.fromHtml(mStringBuilder.toString()));
	}

	public String readRawText(final int resource) {
		InputStream inputStream = null;
		if (mReadBuffer == null) mReadBuffer = new char[READ_BUF_SIZE];
		if (mStringBuilder == null) mStringBuilder = new StringBuilder(LICENSE_TEXT_SIZE);
		mStringBuilder.ensureCapacity(LICENSE_TEXT_SIZE);
		mStringBuilder.setLength(0);
		try {
			inputStream = getResources().openRawResource(resource);
			Reader reader = new InputStreamReader(inputStream, "UTF-8");
			int bytesRead;
			do {
				bytesRead = reader.read(mReadBuffer);
				if (bytesRead == -1) break;
				mStringBuilder.append(mReadBuffer, 0, bytesRead);
			} while (true);
			inputStream.close();
		}
		catch (IOException e) {
			e.printStackTrace();
			if (Logging.ERROR) Log.e(TAG, "Error when reading raw resource " + resource);
		}
		return mStringBuilder.toString();
	}
	
	public boolean isInstallerRun() {
		return mInstallerStage != INSTALLER_NO_STAGE;
	}
	
	public int getInstallerStage() {
		return mInstallerStage;
	}
	
	public void setInstallerStage(int stage) {
		mInstallerStage = stage;
		SharedPreferences globalPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (Logging.DEBUG) Log.d(TAG, "Set installer stage:"+stage);
		
		int installerStageToSave = mInstallerStage;
		/* fix stage for restarted manager */
		if (mInstallerStage == INSTALLER_CLIENT_INSTALLING_STAGE)
			installerStageToSave = INSTALLER_CLIENT_STAGE;
		else if (mInstallerStage == INSTALLER_PROJECT_INSTALLING_STAGE)
			installerStageToSave = INSTALLER_PROJECT_STAGE;
		
		globalPrefs.edit().putInt(PreferenceName.INSTALLER_STAGE, installerStageToSave).commit();
	}
	
	public void backToPreviousInstallerStage() {
		if (mInstallerStage == INSTALLER_CLIENT_INSTALLING_STAGE)
			mInstallerStage = INSTALLER_CLIENT_STAGE;
		else if (mInstallerStage == INSTALLER_PROJECT_INSTALLING_STAGE)
			mInstallerStage = INSTALLER_PROJECT_STAGE;
	}
	
	public void unsetInstallerStage() {
		mInstallerStage = INSTALLER_NO_STAGE;
		SharedPreferences globalPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		globalPrefs.edit().putInt(PreferenceName.INSTALLER_STAGE, mInstallerStage).commit();
	}
	
	public void runInstallerActivity(Context context) {
		switch (mInstallerStage) {
		case INSTALLER_CLIENT_STAGE:
			context.startActivity(new Intent(context, InstallStep1Activity.class));
			break;
		case INSTALLER_CLIENT_INSTALLING_STAGE:
		case INSTALLER_PROJECT_INSTALLING_STAGE:
			context.startActivity(new Intent(context, ProgressActivity.class));
			break;
		case INSTALLER_PROJECT_STAGE:
			context.startActivity(new Intent(context, InstallStep2Activity.class));
			break;
		}
	}
	
	/****
	 * runner support - 
	 * */
	
	public void bindRunnerAndStart() {
		mDoStartClientAfterBind = true;
		doBindRunnerService();
	}
	
	public NativeBoincService getRunnerService() {
		return mRunner;
	}
	
	public boolean isBoincClientRun() {
		return mRunner != null && mRunner.isRun();
	}
	
	public boolean isFirstStartingAtAppStartup() {
		return mFirstStartingAtAppStartup;
	}

	@Override
	public void onClientStart() {
		if (Logging.DEBUG) Log.d(TAG, "On client start");
		mFirstStartingAtAppStartup = false; // after starting
	}
	
	@Override
	public void onClientStop(int exitCode, boolean stoppedByManager) {
		// unbind service
		doUnbindRunnerService();
	}

	@Override
	public void onChangeRunnerIsWorking(boolean isWorking) {
	}
	
	@Override
	public boolean onNativeBoincClientError(String message) {
		if (mFirstStartingAtAppStartup && mAutostartTrialNumber < MAX_AUTOSTART_TRIES_N) {
			// when first start at startup
			mFirstStartingAtAppStartup = false; // if starting failed
			// try again if at booting
			mSecondTryStartHandler.tryStartAgain();
		} else
			doUnbindRunnerService();
		
		mFirstStartingAtAppStartup = false;
		
		// TODO: handle native boinc error
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
		return false;
	}
	
	/*
	 * notifications controller
	 */
	public NotificationController getNotificationController() {
		return mNotificationController;
	}

	/*
	 * restart after reinstall handling
	 */
	public boolean restartedAfterReinstall() {
		synchronized (mRestartAfterReinstallSync) {
			return mRestartedAfterReinstall;
		}
	}
	
	public void setRunRestartAfterReinstall() {
		if (Logging.DEBUG) Log.d(TAG, "SetRunRestartAfterInstall");
		synchronized(mRestartAfterReinstallSync) {
			mRunRestartAfterReinstall = true;
		}
	}
	
	public void setRestartedAfterReinstall() {
		if (Logging.DEBUG) Log.d(TAG, "SetRestartedAfterInstall");
		synchronized(mRestartAfterReinstallSync) {
			if (mRunRestartAfterReinstall)
				mRestartedAfterReinstall = true;
			mRunRestartAfterReinstall = false;
		}
	}
	
	public void resetRestartAfterReinstall() {
		if (Logging.DEBUG) Log.d(TAG, "resetRunRestartAfterInstall");
		synchronized(mRestartAfterReinstallSync) {
			mRunRestartAfterReinstall = false;
			mRestartedAfterReinstall = false;
		}
	}
	
	/**
	 * visivbility control
	 */
	public MyActivityVisibilityTracker getVisibilityTracker() {
		return mVisibilityTracker;
	}
	

	/*
	 * standard handler
	 */
	public Handler getStandardHandler() {
		return mStandardHandler;
	}
	
	/**
	 * installation on the SDCard (place info)
	 */
	public final static boolean isSDCardInstallation(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(PreferenceName.INSTALL_PLACE, false);
	}
	
	public final static String getBoincDirectory(Context context, boolean sdcard) {
		if (!sdcard)
			return context.getFilesDir().getAbsolutePath()+"/boinc";
		else
			return Environment.getExternalStorageDirectory().getAbsolutePath()+
					"/Android/data/sk.boinc.nativeboinc/files/boinc";
	}
	
	public final static String getBoincDirectory(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return getBoincDirectory(context, prefs.getBoolean(PreferenceName.INSTALL_PLACE, false));
	}
	
	public final static void setBoincPlace(Context context, boolean inSDCard) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().putBoolean(PreferenceName.INSTALL_PLACE, inSDCard)
			.commit();
	}
}
