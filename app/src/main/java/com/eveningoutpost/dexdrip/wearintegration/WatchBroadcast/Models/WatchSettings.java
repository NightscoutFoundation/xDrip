package com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models;

import android.os.Parcel;
import android.os.Parcelable;

public class WatchSettings implements Parcelable {
    public static final Creator<WatchSettings> CREATOR = new Creator<WatchSettings>() {
        @Override
        public WatchSettings createFromParcel(Parcel in) {
            return new WatchSettings(in);
        }

        @Override
        public WatchSettings[] newArray(int size) {
            return new WatchSettings[size];
        }
    };

    private long graphStart;
    private long graphEnd;
    private String apkName;
    private boolean displayGraph;

    protected WatchSettings(Parcel in) {
        apkName = in.readString();
        graphStart = in.readLong();
        graphEnd = in.readLong();
        displayGraph = in.readBoolean();
    }

    public long getGraphStart() {
        return graphStart;
    }

    public void setGraphStart(long graphStart) {
        this.graphStart = graphStart;
    }

    public long getGraphEnd() {
        return graphEnd;
    }

    public void setGraphEnd(long graphEnd) {
        this.graphEnd = graphEnd;
    }

    public String getApkName() {
        return apkName;
    }

    public void setApkName(String apkName) {
        this.apkName = apkName;
    }

    public boolean isDisplayGraph() {
        return displayGraph;
    }

    public void setDisplayGraph(boolean displayGraph) {
        this.displayGraph = displayGraph;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(apkName);
        parcel.writeLong(graphStart);
        parcel.writeLong(graphEnd);
        parcel.writeBoolean(displayGraph);
    }
}
