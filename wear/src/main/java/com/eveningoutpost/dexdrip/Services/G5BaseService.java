package com.eveningoutpost.dexdrip.Services;

import android.app.Service;

import com.google.android.gms.wearable.DataMap;

/**
 * Created by jamorham on 21/09/2017.
 */

public abstract class G5BaseService extends Service {

    protected static String lastState = "Not running";
    protected static String lastStateWatch = "Not running";
    protected static long static_last_timestamp = 0;
    protected static long static_last_timestamp_watch = 0;

    public static void setWatchStatus(DataMap dataMap) {
        lastStateWatch = dataMap.getString("lastState", "");
        static_last_timestamp_watch = dataMap.getLong("timestamp", 0);
    }

    public static DataMap getWatchStatus() {
        DataMap dataMap = new DataMap();
        dataMap.putString("lastState", lastState);
        dataMap.putLong("timestamp", static_last_timestamp);
        return dataMap;
    }


}
