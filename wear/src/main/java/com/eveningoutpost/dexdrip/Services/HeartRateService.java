package com.eveningoutpost.dexdrip.Services;

import android.Manifest;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;

import com.eveningoutpost.dexdrip.ListenerService;
import com.eveningoutpost.dexdrip.Models.HeartRate;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.SensorPermissionActivity;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.android.gms.wearable.DataMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jamorham on 12/01/2018.
 *
 * Intent service to measure heart rate using wear sensor
 *
 * beware when modifying this that each time the intent fires it is a new instance
 * so things to persist between firings need to be static or maintained independently
 */

public class HeartRateService extends IntentService {

    public static final String TAG = "HeartRateService";

    private static final long TIME_WAIT_FOR_READING = Constants.SECOND_IN_MS * 30;
    private static final long READING_PERIOD = Constants.SECOND_IN_MS * 300; // TODO review or allow configurable period
    private static final int ELEVATED_BPM = 100;

    private static final int BATCH_LATENCY_1s = 1000000;
    private static PendingIntent pendingIntent;
    private static long start_measuring_time = 0;
    private static long wakeup_time = 0;
    private static boolean mSensorPermissionApproved;//KS
    private static SensorManager mSensorManager;


    // event receiver when we get notified of a heart rate measurement
    private static final SensorEventListener mHeartRateListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("heartrate-event", 60000);
            try {
                final int type = event.sensor.getType();

                if (type == android.hardware.Sensor.TYPE_HEART_RATE) {
                    try {
                        float mHeartRateFloat = event.values[0];

                        final int HeartRateBpm = Math.round(mHeartRateFloat);
                        if (HeartRateBpm > 30) {
                            android.util.Log.d(TAG, "HeartRate: " + HeartRateBpm + " " + event.sensor.getName() + " " + event.accuracy + " " + event.timestamp);
                            HeartRate.create(JoH.tsl(), HeartRateBpm, event.accuracy);
                            if (JoH.ratelimit("reschedule heart rate", 3)) {
                                stopHeartRateMeasurement();
                                final boolean elevated = (HeartRateBpm > ELEVATED_BPM);
                                scheduleWakeUp(elevated ? READING_PERIOD / 2 : READING_PERIOD, "got reading - await next, elevated: " + elevated);
                                if (JoH.ratelimit("heart-resync", 20)) {
                                    ListenerService.requestData(xdrip.getAppContext()); // force sync
                                }
                                //
                            }
                        } else {
                            if (start_measuring_time > 0) {
                                UserError.Log.d(TAG, "Heartrate: too low to accept: " + HeartRateBpm);
                                //scheduleWakeUp(10000, "bad reading - schedule timeout"); // don't need to schedule as we will have a timeout monitor pending
                            } else {
                                UserError.Log.d(TAG, "We are supposed to be shutdown so not scheduling anything: rate: " + HeartRateBpm);
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "HeartRate Exception: ", e);
                    }
                } else {
                    UserError.Log.e(TAG, "onSensorChanged: heart unknown sensor type! " + type);
                }
            } finally {
                JoH.releaseWakeLock(wl);
            }
        }

        @Override
        public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
        }
    };

    public HeartRateService() {
        super("HeartRateService");
    }

    public static void scheduleWakeUp(long future, final String info) {
        if (Pref.getBoolean("use_wear_heartrate", true)) {
            if (future < 0) future = 5000;
            UserError.Log.d(TAG, "Scheduling wakeup @ " + JoH.dateTimeText(JoH.tsl() + future) + " (" + info + ")");
            if (pendingIntent == null)
                pendingIntent = PendingIntent.getService(xdrip.getAppContext(), 0, new Intent(xdrip.getAppContext(), HeartRateService.class), 0);
            wakeup_time = JoH.tsl() + future;
            JoH.wakeUpIntent(xdrip.getAppContext(), future, pendingIntent);
        } else {
            UserError.Log.d(TAG, "Service disabled so not scheduling a wakeup");
            if (start_measuring_time > 0) {
                // shut down if running
                stopHeartRateMeasurement();
            }
        }
    }

    private static synchronized void stopHeartRateMeasurement() {
        UserError.Log.i(TAG, "stopHeartRateMeasurement");
        start_measuring_time = 0;
        try {
            if (mSensorManager != null) {
                UserError.Log.i(TAG, "stopHeartRateMeasurement STOP Event listener for heart rate sensor register");
                mSensorManager.unregisterListener(mHeartRateListener);

            } else {
                UserError.Log.d(TAG, "Sensor Manager was null in stop!");
            }
        } catch (Exception e) {
            UserError.Log.i(TAG, "StopHeartMeasurement exception: " + e);
        }
    }

    public static synchronized DataMap getWearHeartSensorData(int count, long last_send_time, int min_count) {
        UserError.Log.d(TAG, "getWearHeartSensorData last_send_time:" + JoH.dateTimeText(last_send_time));

        HeartRate last_log = HeartRate.last();
        if (last_log != null) {
            UserError.Log.d(TAG, "getWearHeartSensorData last_log.timestamp:" + JoH.dateTimeText((long) last_log.timestamp));
        } else {
            UserError.Log.d(TAG, "getWearHeartSensorData HeartRate.last() = null:");
        }

        if (last_log != null && last_send_time <= last_log.timestamp) {//startTime
            long last_send_success = last_send_time;
            UserError.Log.d(TAG, "getWearHeartSensorData last_send_time < last_bg.timestamp:" + JoH.dateTimeText((long) last_log.timestamp));
            List<HeartRate> logs = HeartRate.latestForGraph(count, last_send_time);
            if (!logs.isEmpty() && logs.size() > min_count) {
                DataMap entries = dataMap(last_log);
                final ArrayList<DataMap> dataMaps = new ArrayList<>(logs.size());
                for (HeartRate log : logs) {
                    dataMaps.add(dataMap(log));
                    last_send_success = (long) log.timestamp;
                }
                entries.putLong("time", JoH.tsl()); // MOST IMPORTANT LINE FOR TIMESTAMP
                entries.putDataMapArrayList("entries", dataMaps);
                UserError.Log.i(TAG, "getWearHeartSensorData SYNCED up to " + JoH.dateTimeText(last_send_success) + " count = " + logs.size());
                return entries;
            } else
                UserError.Log.i(TAG, "getWearHeartSensorData SYNCED up to " + JoH.dateTimeText(last_send_success) + " count = 0");
        }
        return null;
    }

    // this is pretty inefficient - maybe eventually replace with protobuf
    private static DataMap dataMap(HeartRate log) {
        final DataMap dataMap = new DataMap();
        final String json = log.toS();
        dataMap.putString("entry", json);
        return dataMap;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // service created
        final PowerManager.WakeLock wl = JoH.getWakeLock("heartrate-service", 60000);
        try {
            checkWhatToDo();
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private void checkWhatToDo() {
        if (start_measuring_time == 0) {
            if (!restartHeartRateMeasurement()) {
                UserError.Log.d(TAG, "No heart rate sensor or disabled");
                return;
            }
            scheduleWakeUp(TIME_WAIT_FOR_READING + 1, "Started reading, recheck after timeout period");
        } else {
            if (JoH.msSince(start_measuring_time) > TIME_WAIT_FOR_READING) {
                stopHeartRateMeasurement();
                scheduleWakeUp(READING_PERIOD, "Failed to get last reading, retrying later");
            } else {
                scheduleWakeUp(20 * Constants.SECOND_IN_MS, "Not sure what to do, trying again in a bit");
            }
        }
    }

    private synchronized boolean restartHeartRateMeasurement() {
        UserError.Log.d(TAG, "restartHeartRateMeasurement");
        stopHeartRateMeasurement();
        return startHeartRateMeasurement();
    }

    private synchronized boolean startHeartRateMeasurement() {
        UserError.Log.d(TAG, "startHeartRateMeasurement");
        if (Pref.getBoolean("use_wear_heartrate", true)) {
            if (mSensorManager == null)
                mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
            if (mSensorManager != null) {
                android.hardware.Sensor mHeartRateSensor = mSensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_HEART_RATE);
                if (mHeartRateSensor != null) {
                    if (checkSensorPermissions()) {
                        UserError.Log.d(TAG, "Enabling sensor");
                        start_measuring_time = JoH.tsl();
                        mSensorManager.registerListener(mHeartRateListener, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL, BATCH_LATENCY_1s);
                    } else {
                        UserError.Log.d(TAG, "No permission for heartrate");
                    }

                } else {
                    UserError.Log.d(TAG, "startMeasurement No Heart Rate Sensor found");
                    return false;
                }
            }
        } else {
            UserError.Log.d(TAG, "Heart rate feature disabled so not attempting to start");
            return false;
        }
        return true;
    }

    // wear requires runtime permission for this sensor
    private boolean checkSensorPermissions() {//KS
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        mSensorPermissionApproved =
                ActivityCompat.checkSelfPermission(
                        getApplicationContext(),
                        Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED;
        UserError.Log.d(TAG, "checkSensorPermission  mSensorPermissionApproved:" + mSensorPermissionApproved);

        // Display Activity to get user permission
        if (!mSensorPermissionApproved) {
            if (JoH.ratelimit("sensor_permission", 20)) {
                Intent permissionIntent = new Intent(getApplicationContext(), SensorPermissionActivity.class);
                permissionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(permissionIntent);
            }
        }

        return mSensorPermissionApproved;
    }

}


