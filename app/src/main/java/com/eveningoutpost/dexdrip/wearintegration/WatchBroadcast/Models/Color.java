package com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models;

import android.os.Parcel;
import android.os.Parcelable;

public class Color implements Parcelable {
    private String name;
    private int color;

    public Color(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public Color(Parcel parcel) {
        name = parcel.readString();
        color = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeInt(color);
    }

    public static final Creator<Color> CREATOR = new Creator<Color>() {

        @Override
        public Color createFromParcel(Parcel source) {
            return new Color(source);
        }

        @Override
        public Color[] newArray(int size) {
            return new Color[size];
        }
    };
}
