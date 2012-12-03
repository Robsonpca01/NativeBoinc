

package sk.boinc.nativeboinc.util;

import android.os.Parcel;
import android.os.Parcelable;


public class ProjectItem implements Parcelable {

	public final static String TAG = "ProjectItem";
	
	private String mUrl;
	private String mName;
	private String mVersion;
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mUrl);
		dest.writeString(mName);
		dest.writeString(mVersion);
	}
	
	private ProjectItem(Parcel in) {
		mUrl = in.readString();
		mName = in.readString();
		mVersion = in.readString();
	}
	
	public ProjectItem(String name, String url) {
		mName = name;
		mUrl = url;
		mVersion = null;
	}
	
	public ProjectItem(String name, String url, String version) {
		mName = name;
		mUrl = url;
		mVersion = version;
	}
	
	public static final Parcelable.Creator<ProjectItem> CREATOR
		= new Parcelable.Creator<ProjectItem>() {
			@Override
			public ProjectItem createFromParcel(Parcel in) {
				return new ProjectItem(in);
			}
	
			@Override
			public ProjectItem[] newArray(int size) {
				return new ProjectItem[size];
			}
	};

	public String getName() {
		return mName;
	}

	public String getUrl() {
		return mUrl;
	}
	
	public String getVersion() {
		return mVersion;
	}
}
