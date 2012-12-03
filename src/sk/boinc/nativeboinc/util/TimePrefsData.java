
package sk.boinc.nativeboinc.util;

import edu.berkeley.boinc.lite.TimePreferences;
import android.os.Parcel;
import android.os.Parcelable;

public class TimePrefsData implements Parcelable {

	public TimePreferences timePrefs = new TimePreferences();
	
	public static final Parcelable.Creator<TimePrefsData> CREATOR
		= new Parcelable.Creator<TimePrefsData>() {
			@Override
			public TimePrefsData createFromParcel(Parcel in) {
				return new TimePrefsData(in);
			}
	
			@Override
			public TimePrefsData[] newArray(int size) {
				return new TimePrefsData[size];
			}
	};
	
	public TimePrefsData(TimePreferences timePrefs) {
		this.timePrefs = timePrefs;
	}
	
	private TimePrefsData(Parcel src) {
		timePrefs.start_hour = src.readDouble();
		timePrefs.end_hour = src.readDouble();
		
		TimePreferences.TimeSpan[] weekPrefs = timePrefs.week_prefs;
		for (int i = 0; i < 7; i++) {
			byte exists = src.readByte();
			if (exists == 1) {
				weekPrefs[i] = new TimePreferences.TimeSpan();
				weekPrefs[i].start_hour = src.readDouble();
				weekPrefs[i].end_hour = src.readDouble();
			} else
				weekPrefs[i] = null;
		}
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeDouble(timePrefs.start_hour);
		dest.writeDouble(timePrefs.end_hour);
		
		for (TimePreferences.TimeSpan timeSpan: timePrefs.week_prefs) {
			dest.writeByte((byte)(timeSpan != null ? 1 : 0));
			if (timeSpan != null) {
				dest.writeDouble(timeSpan.start_hour);
				dest.writeDouble(timeSpan.end_hour);
			}
		}
	}
}
