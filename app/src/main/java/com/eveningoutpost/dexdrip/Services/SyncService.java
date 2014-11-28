package com.eveningoutpost.dexdrip.Services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.CalibrationSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.RestCalls;
import com.eveningoutpost.dexdrip.UtilityModels.SensorSendQueue;

public class SyncService extends Service {
    int mStartMode;

    @Override
    public void onCreate() {
        Log.w("SYNC SERVICE:", "STARTING SERVICE");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PendingIntent pending = PendingIntent.getService(this, 0, new Intent(this, SyncService.class), 0);
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pending);

        startSleep();
        return mStartMode;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void attemptSend() {
        if (false) {
            for (SensorSendQueue job : SensorSendQueue.queue()) {
                RestCalls.sendSensor(job);
            }
            for (CalibrationSendQueue job : CalibrationSendQueue.queue()) {
                RestCalls.sendCalibration(job);
            }
            for (BgSendQueue job : BgSendQueue.queue()) {
                RestCalls.sendBgReading(job);
            }
        }
    }

    public void startSleep() {
        AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarm.set(
                alarm.ELAPSED_REALTIME_WAKEUP,
                System.currentTimeMillis() + (1000 * 30 * 5),
                PendingIntent.getService(this, 0, new Intent(this, SyncService.class), 0)
        );
    }
}
