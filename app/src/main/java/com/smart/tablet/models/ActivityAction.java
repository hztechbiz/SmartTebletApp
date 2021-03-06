package com.smart.tablet.models;

import android.os.Parcel;
import android.os.Parcelable;

public class ActivityAction implements Parcelable {
    public static final Creator<com.smart.tablet.models.ActivityAction> CREATOR = new Creator<com.smart.tablet.models.ActivityAction>() {
        @Override
        public com.smart.tablet.models.ActivityAction createFromParcel(Parcel source) {
            return new com.smart.tablet.models.ActivityAction(source);
        }

        @Override
        public com.smart.tablet.models.ActivityAction[] newArray(int size) {
            return new com.smart.tablet.models.ActivityAction[size];
        }
    };
    private int key;
    private String data;

    public ActivityAction(int key, String data) {
        this.key = key;
        this.data = data;
    }

    protected ActivityAction(Parcel in) {
        this.key = in.readInt();
        this.data = in.readString();
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.key);
        dest.writeString(this.data);
    }
}
