package com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models;

import android.os.Parcel;
import android.os.Parcelable;

public class WatchSettings implements Parcelable {
    public long getGraphSince() {
        return graphSince;
    }

    public void setGraphSince(long graphSince) {
        this.graphSince = graphSince;
    }

    private long graphSince;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(graphSince);
    }
}
