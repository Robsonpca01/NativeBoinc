
package sk.boinc.nativeboinc;

import sk.boinc.nativeboinc.R;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class InstallFinishActivity extends AbstractBoincActivity {

	private BoincManagerApplication mApp = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.install_finish);
		
		mApp = (BoincManagerApplication)getApplication();
		mApp.unsetInstallerStage();
		
		Button finishButton = (Button)findViewById(R.id.installFinish);
		finishButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
}
