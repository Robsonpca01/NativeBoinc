
package sk.boinc.nativeboinc;

import sk.boinc.nativeboinc.util.ScreenOrientationHandler;
import android.app.Activity;
import android.os.Bundle;

public class AbstractBoincActivity extends Activity {
	
	private ScreenOrientationHandler mScreenOrientation;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mScreenOrientation = new ScreenOrientationHandler(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mScreenOrientation.setOrientation();
	}
	
	@Override
	protected void onDestroy() {
		mScreenOrientation = null;
		super.onDestroy();
	}
}
