
package sk.boinc.nativeboinc;

import sk.boinc.mobileboinc.clientconnection.TaskInfo;
import sk.boinc.nativeboinc.R;
import sk.boinc.nativeboinc.util.TaskItem;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.TextView;


public class TaskInfoDialogActivity extends Activity {
	private static final int DIALOG_TASK_INFO = 1;
	
	public static final String ARG_TASK_INFO = "TaskInfo";
	
	private TaskInfo mTaskInfo = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TaskItem item = (TaskItem)getIntent().getParcelableExtra(ARG_TASK_INFO);
		mTaskInfo = item.taskInfo;
		
		showDialog(DIALOG_TASK_INFO);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
		if (Build.VERSION.SDK_INT < 11 || screenSize != 4)
			// only when is not honeycomb or screenSize is not xlarge
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_TASK_INFO) {
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setView(LayoutInflater.from(this).inflate(R.layout.dialog, null))
			.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			})
			.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			})
			.create();
		}
		return null;
	}
	
	@Override
	protected void onPrepareDialog(int dialogId, Dialog dialog) {
		if (dialogId == DIALOG_TASK_INFO) {
			TextView text = (TextView)dialog.findViewById(R.id.dialogText);
			if (mTaskInfo != null)
				text.setText(Html.fromHtml(prepareTaskDetails(mTaskInfo)));
		}
	}
	
	private String prepareTaskDetails(TaskInfo task) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(getString(R.string.taskDetailedInfoCommon, 
				TextUtils.htmlEncode(task.taskName),
				TextUtils.htmlEncode(task.project),
				TextUtils.htmlEncode(task.application),
				task.received_time,
				task.progress,
				task.elapsed,
				task.toCompletion,
				task.deadline,
				task.rsc_fpops_est,
				task.rsc_memory_bound));
		if (task.virtMemSize != null) {
			sb.append(getString(R.string.taskDetailedInfoRun, 
					task.virtMemSize,
					task.workSetSize,
					task.cpuTime,
					task.chckpntTime));
			if (task.pid != 0)
				sb.append(getString(R.string.taskDetailedInfoRunPid,
						task.pid));
			if (task.directory != null)
				sb.append(getString(R.string.taskDetailedInfoRunDir,
						task.directory));
		}
		if (task.resources != null) {
			sb.append(getString(R.string.taskDetailedInfoRes, task.resources));
		}
		sb.append(getString(R.string.taskDetailedInfoEnd, task.state));
		//if (Logging.DEBUG) Log.d(TAG, "mSb.length()=" + mSb.length() + ", mSb.capacity()=" + mSb.capacity());
		return sb.toString();
	}
}
