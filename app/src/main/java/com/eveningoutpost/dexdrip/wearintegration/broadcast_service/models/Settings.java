package com.eveningoutpost.dexdrip.wearintegration.broadcast_service.models;

import android.os.Parcel;
import android.os.Parcelable;

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

    private long graphStart;
    private long graphEnd;
    private String apkName;
    private boolean displayGraph;

    protected Settings(Parcel in) {
        apkName = in.readString();
        graphStart = in.readLong();
        graphEnd = in.readLong();
        displayGraph = in.readInt() == 1;
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
        parcel.writeInt(displayGraph ? 1 : 0);
    }
}
