

package sk.boinc.nativeboinc;

import java.util.ArrayList;
import java.util.HashSet;

import sk.boinc.nativeboinc.installer.ClientDistrib;
import sk.boinc.nativeboinc.installer.InstallOp;
import sk.boinc.nativeboinc.installer.InstallerService;
import sk.boinc.nativeboinc.installer.InstallerUpdateListener;
import sk.boinc.nativeboinc.installer.ProjectDistrib;
import sk.boinc.nativeboinc.util.FileUtils;
import sk.boinc.nativeboinc.util.ProgressState;
import sk.boinc.nativeboinc.util.StandardDialogs;
import sk.boinc.nativeboinc.util.UpdateItem;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;


public class UpdateFromSDCardActivity extends ServiceBoincActivity implements
		InstallerUpdateListener {

	public static final String UPDATE_DIR = "UpdateDir";

	private String[] mUpdateDistribList = null;
	private HashSet<String> mSelectedItems = new HashSet<String>();

	private int mUpdateListProgressState = ProgressState.NOT_RUN;
	private InstallOp mUpdateListOp = null;

	@Override
	public int getInstallerChannelId() {
		return InstallerService.DEFAULT_CHANNEL_ID;
	}

	private class ItemOnClickListener implements View.OnClickListener {
		private String mDistribName;

		public ItemOnClickListener(String distribName) {
			mDistribName = distribName;
		}

		public void setUp(String distribName) {
			mDistribName = distribName;
		}

		@Override
		public void onClick(View view) {
			if (mSelectedItems.contains(mDistribName))
				mSelectedItems.remove(mDistribName);
			else
				mSelectedItems.add(mDistribName);
			setConfirmButtonEnabled();
		}
	}

	private class UpdateListAdapter extends BaseAdapter {

		private Context mContext;

		public UpdateListAdapter(Context context) {
			mContext = context;
		}

		@Override
		public int getCount() {
			if (mUpdateDistribList == null)
				return 0;
			return mUpdateDistribList.length;
		}

		@Override
		public Object getItem(int position) {
			return mUpdateDistribList[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View inView, ViewGroup parent) {
			View view = inView;
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(mContext);
				view = inflater.inflate(R.layout.checkable_list_item_1, null);
			}

			TextView nameText = (TextView) view
					.findViewById(android.R.id.text1);
			final CheckBox checkBox = (CheckBox) view
					.findViewById(android.R.id.checkbox);

			final String distribName = mUpdateDistribList[position];

			nameText.setText(distribName);

			checkBox.setChecked(mSelectedItems.contains(distribName));

			ItemOnClickListener itemOnClickListener = (ItemOnClickListener) view
					.getTag();
			if (itemOnClickListener == null) {
				itemOnClickListener = new ItemOnClickListener(distribName);
				view.setTag(itemOnClickListener);
			} else
				itemOnClickListener.setUp(distribName);

			checkBox.setOnClickListener(itemOnClickListener);
			return view;
		}
	}

	private Button mConfirmButton = null;

	private TextView mAvailableText = null;
	private ListView mBinariesList = null;
	private UpdateListAdapter mUpdateListAdapter = null;

	private String mExternalPath = null;
	private String mUpdateDirPath = null;

	private static class SavedState {
		private final String updateDirPath;
		private String[] updateDistribList;
		private HashSet<String> selectedItems;
		private int updateListProgressState;

		public SavedState(UpdateFromSDCardActivity activity) {
			updateDirPath = activity.mUpdateDirPath;
			selectedItems = activity.mSelectedItems;
			updateDistribList = activity.mUpdateDistribList;
			updateListProgressState = activity.mUpdateListProgressState;
		}

		public void restore(UpdateFromSDCardActivity activity) {
			activity.mUpdateDirPath = updateDirPath;
			activity.mSelectedItems = selectedItems;
			activity.mUpdateDistribList = updateDistribList;
			activity.mUpdateListProgressState = updateListProgressState;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		// setup update dir path
		mExternalPath = Environment.getExternalStorageDirectory().toString();
		mUpdateDirPath = FileUtils.joinBaseAndPath(mExternalPath,
				getIntent().getStringExtra(UPDATE_DIR));

		if (!mUpdateDirPath.endsWith("/")) // add last slash
			mUpdateDirPath += "/";
		
		// set up install op operation
		mUpdateListOp = InstallOp.GetBinariesFromSDCard(mUpdateDirPath);
		
		setUpService(false, false, false, false, true, true);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.update);
		
		final SavedState savedState = (SavedState)getLastNonConfigurationInstance();
		if (savedState != null)
			savedState.restore(this);
		
		mAvailableText = (TextView)findViewById(R.id.updateAvailableText);
		mBinariesList = (ListView)findViewById(R.id.updateBinariesList);
		
		mUpdateListAdapter = new UpdateListAdapter(this);
		ListView listView = (ListView)findViewById(R.id.updateBinariesList);
		listView.setAdapter(mUpdateListAdapter);
		
		mBinariesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				CheckBox checkBox = (CheckBox)view.findViewById(android.R.id.checkbox);
				String distribName = mUpdateDistribList[position];
				if (mSelectedItems.contains(distribName)) {
					mSelectedItems.remove(distribName);
					checkBox.setChecked(false);
				} else {
					mSelectedItems.add(distribName);
					checkBox.setChecked(true);
				}
					
				setConfirmButtonEnabled();
			}
		});
		
		mConfirmButton = (Button)findViewById(R.id.updateOk);
		mConfirmButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mInstaller == null)
					return;
				// runs update/installation
				mInstaller.updateDistribsFromSDCard(mUpdateDirPath, mSelectedItems.toArray(new String[0]));
				finish();
				startActivity(new Intent(UpdateFromSDCardActivity.this, ProgressActivity.class));
			}
		});
		
		Button cancelButton = (Button)findViewById(R.id.updateCancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new SavedState(this);
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

	private void updateActivityState() {
		setProgressBarIndeterminateVisibility(mInstaller.isWorking());

		if (mInstaller.handlePendingErrors(mUpdateListOp, this))
			return;

		if (mUpdateDistribList == null) {
			if (mUpdateListProgressState == ProgressState.IN_PROGRESS) {
				mUpdateDistribList = (String[])mInstaller.getPendingOutput(mUpdateListOp);

				if (mUpdateDistribList != null)
					updateDistribList();
			} else if (mUpdateListProgressState == ProgressState.NOT_RUN) {
				// igjnore if not ran, because we will used previous results
				mUpdateListProgressState = ProgressState.IN_PROGRESS;
				mInstaller.getBinariesToUpdateFromSDCard(mUpdateDirPath);
			}
			// if finished but failed
		} else
			updateDistribList();
	}

	@Override
	protected Dialog onCreateDialog(int dialogId, Bundle args) {
		return StandardDialogs.onCreateDialog(this, dialogId, args);
	}

	@Override
	public void onPrepareDialog(int dialogId, Dialog dialog, Bundle args) {
		StandardDialogs.onPrepareDialog(this, dialogId, dialog, args);
	}

	@Override
	public void onInstallerConnected() {
		updateActivityState();
	}

	private void setConfirmButtonEnabled() {
		mConfirmButton.setEnabled(!mSelectedItems.isEmpty());
	}

	@Override
	public void onChangeInstallerIsWorking(boolean isWorking) {
		setProgressBarIndeterminateVisibility(isWorking);
	}

	@Override
	public boolean onOperationError(InstallOp installOp, String distribName, String errorMessage) {
		if (installOp.equals(mUpdateListOp) && mUpdateListProgressState == ProgressState.IN_PROGRESS) {
			mUpdateListProgressState = ProgressState.FAILED;
			StandardDialogs.showInstallErrorDialog(this, distribName, errorMessage);
			return true;
		}
		return false;
	}

	@Override
	public void currentProjectDistribList(
			ArrayList<ProjectDistrib> projectDistribs) {
		// TODO Auto-generated method stub

	}

	@Override
	public void currentClientDistrib(ClientDistrib clientDistrib) {
		// TODO Auto-generated method stub

	}

	@Override
	public void binariesToUpdateOrInstall(UpdateItem[] updateItems) {
		// TODO Auto-generated method stub

	}

	@Override
	public void binariesToUpdateFromSDCard(String[] projectNames) {
		mUpdateDistribList = projectNames;
		updateDistribList();
	}

	private void updateDistribList() {
		if (mUpdateDistribList == null || mUpdateDistribList.length == 0)
			mAvailableText.setText(R.string.updateNoNew);
		else
			mAvailableText.setText(R.string.updateNewAvailable);

		mUpdateListAdapter.notifyDataSetChanged();
	}
}
