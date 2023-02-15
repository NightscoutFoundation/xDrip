/*
package com.eveningoutpost.dexdrip.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.JoH;

*/
/**
 * Created by jamorham on 11/03/2017.
 *//*

public class ProximitySensorService extends Service implements SensorEventListener {

    private final static String TAG = "RemindersProximity";

    private SensorManager mSensorManager;
    private Sensor mProximity;
    public static boolean proximity = true; // default to near

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        // wakelock
        Log.d(TAG, "onCreate()");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mProximity != null) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("proximity-service", 15000);
            Log.d(TAG, "Registering listener");
            mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);

        } else {
            Log.d(TAG, "No proximity sensor");
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            mSensorManager.unregisterListener(this);
        } catch (Exception e) {
            Log.d(TAG, "Exception unregisering listener: " + e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            Log.d(TAG, "Sensor: " + event.values[0] + " " + mProximity.getMaximumRange());
            if (event.values[0] <= (Math.min(mProximity.getMaximumRange() / 2, 10))) {
                proximity = true; // near
            } else {
                proximity = false; // far
            }
            Log.d(TAG, "Proxmity set to: " + proximity);
            // stop service? release wakelock?
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}*/
