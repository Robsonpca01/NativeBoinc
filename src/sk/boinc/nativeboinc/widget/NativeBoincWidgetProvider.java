/* 
 * NativeBOINC - Native BOINC Client with Manager
 * Copyright (C) 2011, Mateusz Szpakowski
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package sk.boinc.nativeboinc.widget;

import sk.boinc.nativeboinc.BoincManagerActivity;
import sk.boinc.nativeboinc.BoincManagerApplication;
import sk.boinc.nativeboinc.R;
import sk.boinc.nativeboinc.ScreenLockActivity;
import sk.boinc.nativeboinc.ShutdownDialogActivity;
import sk.boinc.nativeboinc.debug.Logging;
import sk.boinc.nativeboinc.nativeclient.NativeBoincService;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * @author mat
 *
 */
public class NativeBoincWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "NativeBoincWidgetProvider"; 
	
	public static final String NATIVE_BOINC_WIDGET_PREPARE_UPDATE = "sk.boinc.nativeboinc.widget.WIDGET_PREPARE_UPDATE";
	public static final String NATIVE_BOINC_WIDGET_UPDATE = "sk.boinc.nativeboinc.widget.WIDGET_UPDATE";
	public static final String NATIVE_BOINC_CLIENT_START_STOP = "sk.boinc.nativeboinc.widget.CLIENT_START_STOP";
	
	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		
		if (Logging.DEBUG) Log.d(TAG, "on enabled widget");
		BoincManagerApplication appContext = (BoincManagerApplication)context.getApplicationContext();
		
		/* first update */
		Intent intent = new Intent(NATIVE_BOINC_WIDGET_UPDATE);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, 
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		try {
			if (Logging.DEBUG) Log.d(TAG, "Send update intent");
			pendingIntent.send();
		} catch (Exception ex) { }
		
		// if runner is run
		NativeBoincService runner = appContext.getRunnerService();
		if (runner != null && runner.isRun()) {
			// then send prepare update
			intent = new Intent(NATIVE_BOINC_WIDGET_PREPARE_UPDATE);
			pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, 
					PendingIntent.FLAG_UPDATE_CURRENT);
			
			try {
				if (Logging.DEBUG) Log.d(TAG, "Send prepare update intent");
				pendingIntent.send();
			} catch (Exception ex) { }
		}
		
		// trigger autorefresher
		appContext.getRefreshWidgetHandler().manuallyAttachAutoRefresher();
	}
	
	@Override
	public void onDisabled(Context context) {
		if (Logging.DEBUG) Log.d(TAG, "on disabled widget");
		BoincManagerApplication appContext = (BoincManagerApplication)context.getApplicationContext();
		
		// detach autorefresher
		appContext.getRefreshWidgetHandler().manuallyDetachAutoRefresher();
	}
	
	/* check widgets */
	public final static boolean isWidgetEnabled(Context context) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		
		ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
				NativeBoincWidgetProvider.class.getName());
		int[] widgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
		
		return (widgetIds != null && widgetIds.length != 0);
	}
	
	@Override
	public void onReceive(Context context, Intent inputIntent) {
		super.onReceive(context, inputIntent);
		
		if (inputIntent == null || inputIntent.getAction() == null)
			return;
		
		BoincManagerApplication appContext = (BoincManagerApplication)context.getApplicationContext();
		
		if (!isWidgetEnabled(appContext))
			return;
		
		if (inputIntent.getAction().equals(NATIVE_BOINC_WIDGET_PREPARE_UPDATE)) {
			if (Logging.DEBUG) Log.d(TAG, "Widget on prepare update from receive");
			
			final NativeBoincService runner = appContext.getRunnerService();
			
			if (runner != null)
				runner.getGlobalProgress(RefreshWidgetHandler.WIDGET_REFRESHER_ID);
			
		} else if (inputIntent.getAction().equals(NATIVE_BOINC_WIDGET_UPDATE)) {
			if (Logging.DEBUG) Log.d(TAG, "Widget on update from receive");
			
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			
			/* start manager button */
			Intent intent = new Intent(appContext, BoincManagerActivity.class);
			intent.putExtra(BoincManagerActivity.PARAM_CONNECT_NATIVE_CLIENT, true);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent pendingIntent = PendingIntent.getActivity(appContext, 0, intent, 0);
			views.setOnClickPendingIntent(R.id.widgetManager, pendingIntent);
			
			/* start manager button */
			intent = new Intent(appContext, ScreenLockActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			pendingIntent = PendingIntent.getActivity(appContext, 0, intent, 0);
			views.setOnClickPendingIntent(R.id.widgetLock, pendingIntent);
			
			intent = new Intent(NATIVE_BOINC_CLIENT_START_STOP);
			pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.widgetStart, pendingIntent);
			views.setOnClickPendingIntent(R.id.widgetStop, pendingIntent);
			
			/* update progress bar */
			final NativeBoincService runner = appContext.getRunnerService();
			double progress = inputIntent.getDoubleExtra(BoincManagerApplication.UPDATE_PROGRESS, -1.0);
			boolean isRun = runner != null && runner.isRun();
			
			if (progress >= 0.0) {
				views.setViewVisibility(R.id.widgetProgress, View.VISIBLE);
				views.setTextViewText(R.id.widgetProgressText, String.format("%.3f%%", progress));
				views.setProgressBar(R.id.widgetProgressRunning, 100, (int)progress, false);
				
			} else {
				views.setViewVisibility(R.id.widgetProgress, View.GONE);
			}
			
			if (isRun) {
				views.setViewVisibility(R.id.widgetStart, View.GONE);
				views.setViewVisibility(R.id.widgetStop, View.VISIBLE);
			} else {
				views.setViewVisibility(R.id.widgetStart, View.VISIBLE);
				views.setViewVisibility(R.id.widgetStop, View.GONE);
			}
			
			ComponentName thisAppWidget = new ComponentName(context.getPackageName(), getClass().getName());
			int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
			appWidgetManager.updateAppWidget(ids, views);
			
		} else if (inputIntent.getAction().equals(NATIVE_BOINC_CLIENT_START_STOP)) {
			if (Logging.DEBUG) Log.d(TAG, "Client start/stop from widget receive");
			
			final NativeBoincService runner = appContext.getRunnerService();
			if (runner != null) {
				if (runner.isRun()) {
					Intent intent = new Intent(appContext, ShutdownDialogActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					PendingIntent pendingIntent = PendingIntent.getActivity(appContext, 0, intent, 0);
					try {
						pendingIntent.send();
					} catch(CanceledException ex) { }
				} else
					runner.startClient(false);
			} else {
				appContext.bindRunnerAndStart();
			}
		}
	}
}
