package com.eveningoutpost.dexdrip.utils.framework;


import android.app.Service;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.usererror.UserErrorLog;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;

// jamorham

public abstract class ForegroundService extends Service {

    // plenty of duplicated code here vs ForegroundServiceStarter - TODO?

    @Override
    public void onCreate() {
        super.onCreate();
        UserErrorLog.d("FOREGROUND-Service", "Current Service: " + this.getClass().getSimpleName());
        startInForeground();
    }


    protected void startInForeground() {
        new ForegroundServiceStarter(getApplicationContext(), this).start();
        foregroundStatus();
    }

    protected void stopInForeground() {
        // TODO refuse to stop on oreo+ ?
        this.stopForeground(true);
        foregroundStatus();
    }


    protected void foregroundStatus() {
        Inevitable.task("foreground-status", 2000, () -> UserErrorLog.d("FOREGROUND-Service", this.getClass().getSimpleName() + (JoH.isServiceRunningInForeground(this.getClass()) ? " is running in foreground" : " is not running in foreground")));
    }
}
