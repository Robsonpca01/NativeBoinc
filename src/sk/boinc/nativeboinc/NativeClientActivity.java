package sk.boinc.nativeboinc;

import java.io.IOException;

import sk.boinc.nativeboinc.debug.Logging;
import sk.boinc.nativeboinc.installer.AbstractInstallerListener;
import sk.boinc.nativeboinc.installer.InstallOp;
import sk.boinc.nativeboinc.installer.InstallerProgressListener;
import sk.boinc.nativeboinc.installer.InstallerService;
import sk.boinc.nativeboinc.nativeclient.AbstractNativeBoincListener;
import sk.boinc.nativeboinc.nativeclient.NativeBoincService;
import sk.boinc.nativeboinc.nativeclient.NativeBoincUtils;
import sk.boinc.nativeboinc.util.PreferenceName;
import sk.boinc.nativeboinc.util.ScreenOrientationHandler;
import sk.boinc.nativeboinc.util.StandardDialogs;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Window;

/**
 * @author Robson
 *
 */
public class NativeClientActivity extends PreferenceActivity implements AbstractNativeBoincListener,
		AbstractInstallerListener, InstallerProgressListener {

	private static final String TAG = "NativeClientActivity";
	
	private static final int ACTIVITY_ACCESS_LIST = 1;
	
	private static final int DIALOG_APPLY_AFTER_RESTART = 1;
	private static final int DIALOG_ENTER_DUMP_DIRECTORY = 2;
	private static final int DIALOG_REINSTALL_QUESTION = 3;
	private static final int DIALOG_ENTER_UPDATE_DIRECTORY = 4;
	private static final int DIALOG_DUMP_WARNING = 5;
	
	/* information for main activity (for reconnect) */
	public static final String RESULT_DATA_RESTARTED = "Restarted";
	
	private static final String DUMP_DIRECTORY_ARG = "DumpDir";
	
	private ScreenOrientationHandler mScreenOrientation;
	
	private InstallerService mInstaller = null;
	private boolean mDelayedInstallerListenerRegistration = false;
	
	private NativeBoincService mRunner = null;
	private boolean mDelayedRunnerListenerRegistration = false;
	
	private BoincManagerApplication mApp;
	
	private boolean mDoRestart = false;
	private boolean mAllowRemoteHosts;
	private boolean mAllowRemoteHostsDetermined = false;
	
	private String mOldHostname = null;
	
	@Override
	public int getInstallerChannelId() {
		return InstallerService.DEFAULT_CHANNEL_ID;
	}
	
	private ServiceConnection mInstallerConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (Logging.DEBUG) Log.d(TAG, "on Installer connected");
			mInstaller = ((InstallerService.LocalBinder)service).getService();
			
			if (mDelayedInstallerListenerRegistration) {
				mInstaller.addInstallerListener(NativeClientActivity.this);
				mDelayedInstallerListenerRegistration = false;
			}
			
			updateProgressBarState();
			// update preferences
			updatePreferencesEnabled();
			// update show error dialog
			updateServicesError();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			if (mInstaller != null)
				mInstaller.removeInstallerListener(NativeClientActivity.this);
			if (Logging.DEBUG) Log.d(TAG, "on Installer disconnected");
			mInstaller = null;
		}
	};
	
	private ServiceConnection mRunnerConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (Logging.DEBUG) Log.d(TAG, "on Runner connected");
			mRunner = ((NativeBoincService.LocalBinder)service).getService();
			
			if (mDelayedRunnerListenerRegistration) {
				mRunner.addNativeBoincListener(NativeClientActivity.this);
				mDelayedRunnerListenerRegistration = false;
			}
			
			updateProgressBarState();
			// update show error dialog
			updateServicesError();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			if (mRunner != null)
				mRunner.removeNativeBoincListener(NativeClientActivity.this);
			if (Logging.DEBUG) Log.d(TAG, "on Runner disconnected");
			mRunner = null;
		}
	};
	
	private void bindRunnerService() {
		if (Logging.DEBUG) Log.d(TAG, "bind runner");
		bindService(new Intent(this, NativeBoincService.class), mRunnerConnection,
				BIND_AUTO_CREATE);
	}
	
	private void unbindRunnerService() {
		if (mRunner != null)
			mRunner.removeNativeBoincListener(this);
		unbindService(mRunnerConnection);
		mRunner = null;
	}
	
	private void bindInstallerService() {
		if (Logging.DEBUG) Log.d(TAG, "bind installer");
		bindService(new Intent(this, InstallerService.class), mInstallerConnection,
				BIND_AUTO_CREATE);
	}
	
	private void unbindInstallerService() {
		if (mInstaller != null)
			mInstaller.removeInstallerListener(this);
		unbindService(mInstallerConnection);
		mInstaller = null;
	}
	
	
	private static class SavedState {
		private final String oldHostname;
		private final boolean doRestart;
		private final boolean allowRemoteHosts;
		private final boolean allowRemoteHostsDetermined;
		
		public SavedState(NativeClientActivity activity) {
			this.doRestart = activity.mDoRestart;
			this.oldHostname = activity.mOldHostname;
			this.allowRemoteHosts = activity.mAllowRemoteHosts;
			this.allowRemoteHostsDetermined = activity.mAllowRemoteHostsDetermined;
		}
		
		public void restore(NativeClientActivity activity) {
			activity.mDoRestart = this.doRestart;
			activity.mOldHostname = this.oldHostname;
			activity.mAllowRemoteHosts = this.allowRemoteHosts;
			activity.mAllowRemoteHostsDetermined = this.allowRemoteHostsDetermined;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		
		mApp = (BoincManagerApplication)getApplication();
		
		final SavedState savedState = (SavedState)getLastNonConfigurationInstance();
		if (savedState != null)
			savedState.restore(this);
		else { // if created
			/* fetch old hostname from boinc file */
			try {
				mOldHostname = NativeBoincUtils.getHostname(this);
			} catch(IOException ex) { }
			if (mOldHostname == null)
				mOldHostname = "";
		}
		
		bindRunnerService();
		bindInstallerService();
		
		mScreenOrientation = new ScreenOrientationHandler(this);
		addPreferencesFromResource(R.xml.nativeboinc);
		
		// Display latest news
		Preference pref = findPreference(PreferenceName.NATIVE_LATEST_NEWS);
			
		/* native autostart */
		ListPreference listPref = (ListPreference)findPreference(PreferenceName.NATIVE_AUTOSTART);
		
		listPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				ListPreference pref = (ListPreference)preference;
				int index = pref.findIndexOfValue((String)newValue);
				CharSequence[] allDescriptions = pref.getEntries();
				pref.setSummary(allDescriptions[index]);
				return true;
			}
		});
		
		/* hostname preference */
		EditTextPreference editPref = (EditTextPreference)findPreference(PreferenceName.NATIVE_HOSTNAME);
		
		editPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				EditTextPreference pref = (EditTextPreference)preference;
				String newHostName = (String)newValue;
				if (newHostName != null && newHostName.length() != 0)
					pref.setSummary(getString(R.string.nativeHostnameSummaryCurrent)+
							": "+newHostName);
				else
					pref.setSummary(R.string.nativeHostnameSummaryNone);
				
				try {
					NativeBoincUtils.setHostname(NativeClientActivity.this, newHostName);
				} catch(IOException ex) { }
				return true;
			}
		});
		
		/* allow remote client preference */
		CheckBoxPreference checkPref = (CheckBoxPreference)findPreference(
				PreferenceName.NATIVE_REMOTE_ACCESS);
		
		if (!mAllowRemoteHostsDetermined) {
			if (Logging.DEBUG) Log.d(TAG, "isAllowRemoteHosts (in start):"+checkPref.isChecked());
			mAllowRemoteHosts = checkPref.isChecked();
			mAllowRemoteHostsDetermined = true;
		}
	
		/* access password preference */
		editPref = (EditTextPreference)findPreference(PreferenceName.NATIVE_ACCESS_PASSWORD);
		
		editPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				String oldPassword = null;
				try {
					oldPassword = NativeBoincUtils.getAccessPassword(NativeClientActivity.this);
				} catch(IOException ex) { }
				
				String newPassword = (String)newValue;
				
				if (!newPassword.equals(oldPassword)) { // if not same password
					Log.d(TAG, "In changing password");
					try {
						NativeBoincUtils.setAccessPassword(NativeClientActivity.this, newPassword);
					} catch(IOException ex) { }
					
					mDoRestart = true;
				}
				return true;
			}
		});
		
		/* installed binaries preference */
		pref = (Preference)findPreference(PreferenceName.NATIVE_INSTALLED_BINARIES);
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(NativeClientActivity.this, InstalledBinariesActivity.class));
				return true;
			}
		});
		
		
		/* reinstall boinc */
		pref = (Preference)findPreference(PreferenceName.NATIVE_REINSTALL);
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (mInstaller != null && !mInstaller.isBeingReinstalled())
					showDialog(DIALOG_REINSTALL_QUESTION);
				return true;
			}
		});
		
		
		
		/* delete project bins */
		pref = (Preference)findPreference(PreferenceName.NATIVE_DELETE_PROJ_BINS);
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(NativeClientActivity.this, DeleteProjectBinsActivity.class));
				return true;
			}
		});
		
		
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new SavedState(this);
	}
	
	@Override
	protected void onPause() {
		if (mRunner != null) {
			if (Logging.DEBUG) Log.d(TAG, "Unregister runner listener");
			mRunner.removeNativeBoincListener(this);
			mDelayedRunnerListenerRegistration = false;
		}
		if (mInstaller != null) {
			if (Logging.DEBUG) Log.d(TAG, "Unregister installer listener");
			mInstaller.removeInstallerListener(this);
			mDelayedInstallerListenerRegistration = false;
		}
		super.onPause();
	}
	
	private void updateProgressBarState() {
		// display/hide progress
		boolean installerIsWorking = mInstaller != null &&
				mInstaller.isWorking();
		boolean runnerIsWorking = mRunner != null &&
				mRunner.serviceIsWorking();
		
		setProgressBarIndeterminateVisibility(installerIsWorking || runnerIsWorking);
	}
	
	private void updateServicesError() {
		if (mRunner != null && mInstaller != null) {
			mRunner.handlePendingErrorMessage(this);
			mInstaller.handlePendingErrors(null, this);
		}
	}
	
	private void updatePreferencesEnabled() {
		if (mInstaller == null) return;
		
		// update enabled/disabled
		Preference pref = (Preference)findPreference(PreferenceName.NATIVE_DUMP_BOINC_DIR);
		//TODO
		pref = (Preference)findPreference(PreferenceName.NATIVE_REINSTALL);
		pref.setEnabled(!mInstaller.isBeingReinstalled());
		pref = findPreference(PreferenceName.NATIVE_MOVE_INSTALLATION);
		
	}
	
	
	
	@Override
	protected void onResume() {
		super.onResume();
		/* native autostart */
		ListPreference listPref = (ListPreference)findPreference(PreferenceName.NATIVE_AUTOSTART);
		int index = listPref.findIndexOfValue((String)listPref.getValue());
		CharSequence[] allDescriptions = listPref.getEntries();
		listPref.setSummary(allDescriptions[index]);
		
		String hostName = null;
		/* fetch hostname from boinc file */
		try {
			hostName = NativeBoincUtils.getHostname(this);
		} catch(IOException ex) { }
		if (hostName == null)
			hostName = "";
		
		EditTextPreference editPref = (EditTextPreference)findPreference(PreferenceName.NATIVE_HOSTNAME);
		
		/* update host name */
		if (hostName != null)
			editPref.setText(hostName);
		
		String text = editPref.getText();
		/* updating preferences summaries */
		if (text != null && text.length() != 0)
			editPref.setSummary(getString(R.string.nativeHostnameSummaryCurrent)+
					": "+editPref.getText());
		else
			editPref.setSummary(R.string.nativeHostnameSummaryNone);
		
				
		/* add listener */
		if (mRunner != null) {
			if (Logging.DEBUG) Log.d(TAG, "Normal register runner listener");
			mRunner.addNativeBoincListener(this);
			mDelayedRunnerListenerRegistration = false;
		} else
			mDelayedRunnerListenerRegistration = true;
	
		if (mInstaller != null) {
			if (Logging.DEBUG) Log.d(TAG, "Normal register installer listener");
			mInstaller.addInstallerListener(this);
			mDelayedInstallerListenerRegistration = false;
		} else
			mDelayedInstallerListenerRegistration = true;

		mScreenOrientation.setOrientation();
		
		updatePreferencesEnabled();
		// progress bar update
		updateProgressBarState();
		// update error dialogs
		updateServicesError();
	}
	
	@Override
	protected void onDestroy() {
		mScreenOrientation = null;
		mApp.resetRestartAfterReinstall();
		unbindRunnerService();
		unbindInstallerService();
		super.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		if (isRestartRequired() && mRunner != null && mRunner.isRun()) {
			showDialog(DIALOG_APPLY_AFTER_RESTART);
		} else {
			if (mApp.restartedAfterReinstall()) {
				// inform, that client restarted after reinstall
				Intent data = new Intent();
				data.putExtra(RESULT_DATA_RESTARTED, true);
				setResult(RESULT_OK, data);
				finish();
			}
			finish();
		}
	}
	
	@Override
	public Dialog onCreateDialog(int dialogId, Bundle args) {
		Dialog dialog = StandardDialogs.onCreateDialog(this, dialogId, args);
		if (dialog != null)
			return dialog;
		
		switch(dialogId) {
		
		case DIALOG_APPLY_AFTER_RESTART: {
			return new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.warning)
				.setMessage(R.string.applyAfterRestart)
				.setPositiveButton(R.string.restart, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (mRunner != null) {
							mDoRestart = true;
							mRunner.restartClient();
							// finish activity with notification
							Intent data = new Intent();
							data.putExtra(RESULT_DATA_RESTARTED, true);
							setResult(RESULT_OK, data);
							finish();
						}
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// finish activity
						finish();
					}
				})
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						// finish activity
						finish();
					}
				})
				.create();
		}
		case DIALOG_REINSTALL_QUESTION:
			return new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.warning)
				.setMessage(R.string.reinstallQuestion)
				.setPositiveButton(R.string.yesText, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mInstaller.reinstallBoinc();
						mDoRestart = false;
						startActivity(new Intent(NativeClientActivity.this, ProgressActivity.class)); 
					}
				})
				.setNegativeButton(R.string.noText, null)
				.create();
			case DIALOG_DUMP_WARNING:
			return new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.warning)
				.setMessage("")	// this is trick for alert dialog
				.setPositiveButton(R.string.yesText, null)
				.setNegativeButton(R.string.noText, null)
				.create();
		}
		return null;
	}
	

	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTIVITY_ACCESS_LIST && resultCode == RESULT_OK) {
			if (data != null && data.getBooleanExtra(AccessListActivity.RESULT_RESTARTED, false)) {
				if (Logging.DEBUG) Log.d(TAG, "on acces list finish: client restarted");
				mDoRestart = true;
			}
		}
	}
	
	/* check whether restart is required */
	private boolean isRestartRequired() {
		CheckBoxPreference checkPref = (CheckBoxPreference)findPreference(PreferenceName.NATIVE_REMOTE_ACCESS);
		
		EditTextPreference hostnamePref = (EditTextPreference)findPreference(PreferenceName.NATIVE_HOSTNAME);
		
		// if do restart or new allowRemoteHost is different value
		return mDoRestart || (mAllowRemoteHosts != checkPref.isChecked()) ||
				!(mOldHostname.equals(hostnamePref.getText()));
	}

	@Override
	public boolean onNativeBoincClientError(String message) {
		StandardDialogs.showErrorDialog(this, message);
		return true;
	}

	@Override
	public void onChangeRunnerIsWorking(boolean isWorking) {
		if ((mInstaller != null && mInstaller.isWorking()) || isWorking)
			setProgressBarIndeterminateVisibility(true);
		else if ((mInstaller == null || !mInstaller.isWorking()) && !isWorking)
			setProgressBarIndeterminateVisibility(false);
	}

	@Override
	public void onChangeInstallerIsWorking(boolean isWorking) {
		if ((mRunner != null && mRunner.serviceIsWorking()) || isWorking)
			setProgressBarIndeterminateVisibility(true);
		else if ((mRunner == null || !mRunner.serviceIsWorking()) && !isWorking)
			setProgressBarIndeterminateVisibility(false);
	}

	private void changePreferencesEnabled(String distribName) {
		if (distribName.equals(InstallerService.BOINC_DUMP_ITEM_NAME)) {
			Preference pref = (Preference)findPreference(PreferenceName.NATIVE_DUMP_BOINC_DIR);
			pref.setEnabled(true); // enable it
		} else if (distribName.equals(InstallerService.BOINC_REINSTALL_ITEM_NAME)) {
			Preference pref = (Preference)findPreference(PreferenceName.NATIVE_REINSTALL);
			pref.setEnabled(true); // enable it
		} 
	}
	
	@Override
	public boolean onOperationError(InstallOp installOp, String distribName, String errorMessage) {
		if (!installOp.equals(InstallOp.ProgressOperation)) { // only simple operation
			// if global install error
			StandardDialogs.showInstallErrorDialog(this, distribName, errorMessage);
			return true;
		} else
			changePreferencesEnabled(distribName);
		return false;
	}
	
	@Override
	public void onOperation(String distribName, String opDescription) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOperationProgress(String distribName, String opDescription,
			int progress) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onOperationCancel(InstallOp installOp, String distribName) {
		if (installOp.equals(InstallOp.ProgressOperation)) // only simple operation
			changePreferencesEnabled(distribName);
	}

	@Override
	public void onOperationFinish(InstallOp installOp, String distribName) {
		if (installOp.equals(InstallOp.ProgressOperation)) // only simple operation
			changePreferencesEnabled(distribName);
	}
}
