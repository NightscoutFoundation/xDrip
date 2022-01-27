package com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;

public class WatchBroadcast {
    private WatchSettings settings;
    private long statCacheTime = 0;
    private int statCacheHoursVal = 0;
    private Bundle statBundle;

    public WatchBroadcast(WatchSettings settings) {
        this.settings = settings;
    }

    public Bundle getStatBundle() {
        return statBundle;
    }

    public void setStatCache(Bundle statBundle, int statCacheHoursVal) {
        this.statBundle = statBundle;
        this.statCacheHoursVal = statCacheHoursVal;
        this.statCacheTime = JoH.tsl();
    }

    public WatchSettings getSettings() {
        return settings;
    }

    public void setSettings(WatchSettings settings) {
        this.settings = settings;
    }

    public long getStatCacheTime() {
        return statCacheTime;
    }

    public boolean isStatCacheValid(int statHoursVal) {
       return JoH.msSince(statCacheTime) < Constants.MINUTE_IN_MS && statHoursVal == statCacheHoursVal && statBundle != null;
    }
}
