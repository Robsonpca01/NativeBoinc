
package sk.boinc.nativeboinc;

import java.util.ArrayList;

import sk.boinc.mobileboinc.debug.Logging;
import sk.boinc.mobileboinc.installer.ClientDistrib;
import sk.boinc.mobileboinc.installer.InstallOp;
import sk.boinc.mobileboinc.installer.InstallerService;
import sk.boinc.mobileboinc.installer.InstallerUpdateListener;
import sk.boinc.mobileboinc.installer.ProjectDistrib;
import sk.boinc.nativeboinc.R;
import sk.boinc.nativeboinc.util.ProgressState;
import sk.boinc.nativeboinc.util.StandardDialogs;
import sk.boinc.nativeboinc.util.UpdateItem;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;


public class InstallStep1Activity extends ServiceBoincActivity implements
		InstallerUpdateListener {
	private final static String TAG = "InstallStep1Activity";

	private TextView mVersionToInstall = null;

	private BoincManagerApplication mApp = null;

	// if true, then client distrib shouldnot be updated
	private ClientDistrib mClientDistrib = null;
	private int mClientDistribProgressState = ProgressState.NOT_RUN;

	private Button mNextButton = null;

	@Override
	public int getInstallerChannelId() {
		return InstallerService.DEFAULT_CHANNEL_ID;
	}

	private static class SavedState {
		private final ClientDistrib mClientDistrib;
		private final int mClientDistribProgressState;

		public SavedState(InstallStep1Activity activity) {
			mClientDistrib = activity.mClientDistrib;
			mClientDistribProgressState = activity.mClientDistribProgressState;
		}

		public void restore(InstallStep1Activity activity) {
			activity.mClientDistrib = mClientDistrib;
			activity.mClientDistribProgressState = mClientDistribProgressState;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		SavedState savedState = (SavedState) getLastNonConfigurationInstance();
		if (savedState != null)
			savedState.restore(this);

		setUpService(false, false, false, false, true, true);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.install_step1);

		mVersionToInstall = (TextView) findViewById(R.id.versionToInstall);
		mNextButton = (Button) findViewById(R.id.installNext);

		Button cancelButton = (Button) findViewById(R.id.installCancel);

		if (mClientDistrib == null)
			mVersionToInstall.setText(getString(R.string.versionToInstall)
					+ ": ...");

		mApp = (BoincManagerApplication) getApplication();
		mApp.setInstallerStage(BoincManagerApplication.INSTALLER_CLIENT_STAGE); // set
																				// as
																				// run

		mNextButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mInstaller != null)
					installClient();
			}
		});
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mInstaller != null)
			updateActivityState();
	}

	@Override
	public void onBackPressed() {
		if (mInstaller != null)
			mInstaller.cancelSimpleOperation();
		finish();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new SavedState(this);
	}

	private void updateClientVersionText() {
		mVersionToInstall.setText(getString(R.string.versionToInstall) + ": "
				+ mClientDistrib.version);

	
		mNextButton.setEnabled(true);
	}

	private void updateActivityState() {
		setProgressBarIndeterminateVisibility(mInstaller.isWorking());

		if (mInstaller.handlePendingErrors(InstallOp.UpdateClientDistrib, this))
			return;

		if (mClientDistrib == null) {
			if (mClientDistribProgressState == ProgressState.IN_PROGRESS) {
				mClientDistrib = (ClientDistrib) mInstaller
						.getPendingOutput(InstallOp.UpdateClientDistrib);

				if (mClientDistrib != null)
					updateClientVersionText();
			} else if (mClientDistribProgressState == ProgressState.NOT_RUN) {
				mClientDistribProgressState = ProgressState.IN_PROGRESS;
				// ignore if doesnt run, because we reusing previous result
				mInstaller.updateClientDistrib();
			}
			// if finished but failed
		} else
			updateClientVersionText();
	}

	@Override
	protected void onInstallerConnected() {
		updateActivityState();
	}

	@Override
	protected void onInstallerDisconnected() {
		setProgressBarIndeterminateVisibility(false);
	}

	@Override
	public Dialog onCreateDialog(int dialogId, Bundle args) {
		return StandardDialogs.onCreateDialog(this, dialogId, args);
	}

	@Override
	public void onPrepareDialog(int dialogId, Dialog dialog, Bundle args) {
		StandardDialogs.onPrepareDialog(this, dialogId, dialog, args);
	}

	@Override
	public boolean onOperationError(InstallOp installOp, String distribName,
			String errorMessage) {
		if (installOp.equals(InstallOp.UpdateClientDistrib)
				&& mClientDistribProgressState == ProgressState.IN_PROGRESS) {
			mClientDistribProgressState = ProgressState.FAILED;
			StandardDialogs.showInstallErrorDialog(this, distribName,
					errorMessage);
			return true;
		}
		return false;
	}

	@Override
	public void currentProjectDistribList(
			ArrayList<ProjectDistrib> projectDistribs) {
		// do nothing
	}

	@Override
	public void currentClientDistrib(ClientDistrib clientDistrib) {
		if (Logging.DEBUG)
			Log.d(TAG, "clientDistrib");

		mClientDistrib = clientDistrib;
		mClientDistribProgressState = ProgressState.FINISHED;
		updateClientVersionText();
	}

	private void installClient() {
		if (mInstaller == null)
			return;

		mApp.setInstallerStage(BoincManagerApplication.INSTALLER_CLIENT_INSTALLING_STAGE);

		// força instalação sempre no SD card
		BoincManagerApplication.setBoincPlace(this, true);

		mInstaller.installClientAutomatically();
		finish();

		startActivity(new Intent(this, ProgressActivity.class));
	}

	@Override
	public void onChangeInstallerIsWorking(boolean isWorking) {
		setProgressBarIndeterminateVisibility(isWorking);
	}

	@Override
	public void binariesToUpdateOrInstall(UpdateItem[] updateItems) {
		// TODO Auto-generated method stub

	}

	@Override
	public void binariesToUpdateFromSDCard(String[] projectNames) {
		// TODO Auto-generated method stub

	}
}
