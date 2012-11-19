

package sk.boinc.nativeboinc.util;

import android.os.Parcel;
import android.os.Parcelable;


public class BAMAccount implements Parcelable {
	public final static String TAG = "BAMAccount";
	
	public String mName;
	public String mUrl;
	public String mPassword;
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) { 
		dest.writeString(mName);
		dest.writeString(mUrl);
		dest.writeString(mPassword);
	}
	
	private BAMAccount(Parcel in) {
		mName = in.readString();
		mUrl = in.readString();
		mPassword = in.readString();
	}
	
	public static final Parcelable.Creator<BAMAccount> CREATOR
		= new Parcelable.Creator<BAMAccount>() {
			@Override
			public BAMAccount createFromParcel(Parcel in) {
				return new BAMAccount(in);
			}
	
			@Override
			public BAMAccount[] newArray(int size) {
				return new BAMAccount[size];
			}
	};
	
	public BAMAccount(String name, String url, String password) {
		mName = name;
		mUrl = url;
		mPassword = password;
	}
	
	public final String getName() {
		return mName;
	}

	public final String getUrl() {
		return mUrl;
	}

	public final String getPassword() {
		return mPassword;
	}
}
