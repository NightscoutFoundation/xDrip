package com.eveningoutpost.dexdrip.Services.broadcastservice.models;

import android.os.Parcel;
import android.os.Parcelable;

import lombok.Getter;
import lombok.Setter;

public class Settings implements Parcelable {
    public static final Creator<Settings> CREATOR = new Creator<Settings>() {
        @Override
        public Settings createFromParcel(Parcel in) {
            return new Settings(in);
        }

        @Override
        public Settings[] newArray(int size) {
            return new Settings[size];
        }
    };

    /**
     * Defines graph start offset in ms, if not defined, the offset would be Constants.HOUR_IN_MS *2.
     * Would be used only if enabled displayGraph
     */
    @Getter
    @Setter
    private long graphStart;

    /**
     * Defines graph end offset in ms, if not defined, the offset would be current time.
     * Would be used only if enabled displayGraph
     */
    @Getter
    @Setter
    private long graphEnd;

    /**
     * Recipient application name
     */
    @Getter
    @Setter
    private String apkName;

    /**
     * If enabled, will send a graph lines data to recipient
     */
    @Getter
    @Setter
    private boolean displayGraph;

    public Settings(Parcel in) {
        apkName = in.readString();
        graphStart = in.readLong();
        graphEnd = in.readLong();
        displayGraph = in.readInt() == 1;
    }

    public Settings() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(apkName);
        parcel.writeLong(graphStart);
        parcel.writeLong(graphEnd);
        parcel.writeInt(displayGraph ? 1 : 0);
    }
}
