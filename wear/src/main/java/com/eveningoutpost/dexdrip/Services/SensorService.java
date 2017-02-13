package com.eveningoutpost.dexdrip.Services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.PebbleMovement;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.google.android.gms.wearable.DataMap;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SensorService extends Service implements SensorEventListener {
    private static final String TAG = "jamorham SensorService";

    private final static int SENS_STEP_COUNTER = Sensor.TYPE_STEP_COUNTER;

    //max batch latency is specified in microseconds
    private static final int BATCH_LATENCY_10s = 10000000;

    //Steps counted in current session
    private int mSteps = 0;
    //Value of the step counter sensor when the listener was registered.
    //(Total steps are calculated from this value.)
    private int mCounterSteps = 0;
    //Steps counted by the step counter previously. Used to keep counter consistent across rotation
    //changes
    private int mPreviousCounterSteps = 0;

    SensorManager mSensorManager;
    private static long last_movement_timestamp = 0;
    String pref_last_movement_timestamp = "last_movement_timestamp";
    String pref_msteps = "msteps";
    private SharedPreferences sharedPrefs;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Log.d(TAG, "onCreate SensorService");
        resetCounters();
        startMeasurement();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMeasurement();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long t = System.currentTimeMillis();
        long timeAgo = t - last_movement_timestamp;

        int type = event.sensor.getType();
        Log.e(TAG, "onSensorChanged Sensor " + type + " name = " + event.sensor.getStringType());
        Log.e(TAG, "onSensorChanged accuracy = " + event.accuracy);
        Log.e(TAG, "onSensorChanged MaxDelay = " + event.sensor.getMaxDelay());
        Log.e(TAG, "onSensorChanged t = " + t + " text = " + JoH.dateTimeText(t));
        Log.e(TAG, "onSensorChanged last_movement_timestamp = " + last_movement_timestamp + " text = " + JoH.dateTimeText(last_movement_timestamp));
        Log.e(TAG, "onSensorChanged timeAgo = " + timeAgo);

        // Calculate the delay from when event was recorded until it was received here in ms
        // Event timestamp is recorded in us accuracy, but ms accuracy is sufficient here
        long delay = System.currentTimeMillis() - (event.timestamp / 1000000L);//Timestamp when sensor was registered
        Log.e(TAG, "onSensorChanged delay = " + delay + " JoH.DateTimeText(delay) = " + JoH.dateTimeText(delay) + " (delay + (event.timestamp / 1000000L)) = " + delay + (event.timestamp / 1000000L) + " text= " + JoH.dateTimeText(delay + (event.timestamp / 1000000L)));

        PebbleMovement last = PebbleMovement.last();
        boolean sameDay = last != null ? isSameDay(t, last.timestamp) : true;
        if (!sameDay) {
            initCounters();
            Log.e(TAG, "onSensorChanged initCounters initCounters mSteps = " + mSteps + " mCounterSteps = " + mCounterSteps + " mPreviousCounterSteps = " + mPreviousCounterSteps + " last_movement_timestamp = " + last_movement_timestamp);
        }
        if (mCounterSteps < 1) {
            // initial value
            mCounterSteps = (int) event.values[0];
        }
        // Calculate steps taken based on first counter value received.
        mSteps = (int) event.values[0] - mCounterSteps;
        // Add the number of steps previously taken, otherwise the counter would start at 0.
        // This is needed to keep the counter consistent across rotation changes.
        mSteps = mSteps + mPreviousCounterSteps;
        PersistentStore.setLong(pref_msteps, (long)mSteps);
        Log.e(TAG, "onSensorChanged Total step count: " + mSteps + " mCounterSteps: " + mCounterSteps + " mPreviousCounterSteps: " + mPreviousCounterSteps + " event.values[0]: " + event.values[0]);// + " Delay: " + delayString);

        if (last_movement_timestamp < t) {//KS BUG SW3 seems to set event.timestamp to time when sensor listener is registered
            Log.e(TAG, "onSensorChanged Movement for mSteps: " + mSteps + " event.values[0]: " + event.values[0] +
                    " recorded: " + JoH.dateTimeText(System.currentTimeMillis() - (event.timestamp / 1000000L)) +
                    " received: " + JoH.dateTimeText(t) + " last_movement_timestamp: " + JoH.dateTimeText(last_movement_timestamp)
            );
            if (timeAgo < 10000) {//skip if less than 1 minute interval since last step
                Log.e(TAG, "onSensorChanged Interval < 1 minute! Skip new movement record creation");
            }
            else {
                if (last_movement_timestamp == 0 || (sameDay && last != null && last.metric == mSteps)) {//skip initial movement or duplicate steps
                    Log.e(TAG, "onSensorChanged Initial sensor movement! Skip initial movement record, or duplicate record. last.metric=" + last.metric);
                }
                else {
                    final PebbleMovement pm = PebbleMovement.createEfficientRecord(t, mSteps);//event.timestamp * 1000, (int) event.values[0]
                    Log.e(TAG, "Saving Movement: " + pm.toS());
                }
                last_movement_timestamp = t;
                PersistentStore.setLong(pref_last_movement_timestamp, last_movement_timestamp);
            }
            Log.e(TAG, "onSensorChanged sendLocalMessage mSteps: " + mSteps + " t: " + JoH.dateTimeText(t) + " last_movement_timestamp: " + JoH.dateTimeText(last_movement_timestamp));
            sendLocalMessage(mSteps, t);
        }
        else {
            Log.e(TAG, "onSensorChanged last_movement_timestamp > t! Reset last_movement_timestamp to current time.");
            last_movement_timestamp = t;
            PersistentStore.setLong(pref_last_movement_timestamp, last_movement_timestamp);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private boolean isSameDay(long t, long last) {
        Calendar curCal = Calendar.getInstance();
        curCal.setTimeInMillis(t);
        Calendar lastCal = Calendar.getInstance();
        lastCal.setTimeInMillis(last);
        Log.e(TAG, "isSameDay Sensor curCal.DAY_OF_MONTH=" + curCal.get(Calendar.DAY_OF_MONTH) + " lastCal.DAY_OF_MONTH=" + lastCal.get(Calendar.DAY_OF_MONTH) + " t=" + JoH.dateTimeText(t) + " last.timestamp=" + JoH.dateTimeText(last) + " " + last);
        if (curCal.get(Calendar.DAY_OF_MONTH) == lastCal.get(Calendar.DAY_OF_MONTH) &&
                curCal.get(Calendar.YEAR) == lastCal.get(Calendar.YEAR) &&
                curCal.get(Calendar.MONTH) == lastCal.get(Calendar.MONTH) ) {
            return true;
        }
        return false;

    }

    protected void startMeasurement() {
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        //if (BuildConfig.DEBUG) {
        logAvailableSensors();
        //}
        mCounterSteps = 0;
        Log.i(TAG, "startMeasurement SensorService Event listener for step counter sensor register");

        Sensor stepCounterSensor = mSensorManager.getDefaultSensor(SENS_STEP_COUNTER);

        // Register the listener
        if (mSensorManager != null) {
            if (stepCounterSensor != null) {
                if (sharedPrefs.getBoolean("showSteps", false)) {
                    //mSensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
                    Log.e(TAG, "Event listener for step counter sensor registered with a max delay of " + BATCH_LATENCY_10s);
                    mSensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL, BATCH_LATENCY_10s);
                }
                else {
                    Log.e(TAG, "Event listener for step counter sensor registered with a max delay of " + SensorManager.SENSOR_DELAY_NORMAL);
                    mSensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
            } else {
                Log.d(TAG, "No Step Counter Sensor found");
            }
        }
    }

    private void stopMeasurement() {
        Log.i(TAG, "stopMeasurement");
        if (mSensorManager != null) {
            Log.i(TAG, "stopMeasurement STOP Event listener for step counter sensor register");
            mSensorManager.unregisterListener(this);
        }
    }

    private void resetCounters() {
        initCounters();
        mSteps = (int) PersistentStore.getLong(pref_msteps);
        last_movement_timestamp = (int) PersistentStore.getLong(pref_last_movement_timestamp);
        Log.e(TAG, "resetCounters Sensor Enter PersistentStore mSteps = " + mSteps + " mCounterSteps = " + mCounterSteps + " mPreviousCounterSteps = " + mPreviousCounterSteps + " last_movement_timestamp = " + last_movement_timestamp);

        PebbleMovement last = PebbleMovement.last();
        if (!isSameDay(System.currentTimeMillis(), last.timestamp)) {
            initCounters();
            Log.e(TAG, "resetCounters Sensor isSameDay initCounters mSteps = " + mSteps + " mCounterSteps = " + mCounterSteps + " mPreviousCounterSteps = " + mPreviousCounterSteps + " last_movement_timestamp = " + last_movement_timestamp);
        } else {
            mPreviousCounterSteps = mSteps;
            Log.e(TAG, "resetCounters Sensor NOT isSameDay PersistentStore mSteps = " + mSteps + " mCounterSteps = " + mCounterSteps + " mPreviousCounterSteps = " + mPreviousCounterSteps + " last_movement_timestamp = " + last_movement_timestamp);
        }
    }

    private void initCounters() {
        mSteps = 0;
        mCounterSteps = 0;
        mPreviousCounterSteps = 0;
    }

    private void sendLocalMessage(int steps, long timestamp) {
        DataMap dataMap = new DataMap();
        dataMap.putInt("steps", steps);
        dataMap.putLong("steps_timestamp", timestamp);
        Intent messageIntent = new Intent();
        messageIntent.setAction(Intent.ACTION_SEND);
        messageIntent.putExtra("steps", dataMap.toBundle());
        LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
    }

    //Log all available sensors to logcat
    private void logAvailableSensors() {
        final List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        Log.d(TAG, "=== LIST AVAILABLE SENSORS ===");
        Log.d(TAG, String.format(Locale.getDefault(), "|%-35s|%-38s|%-6s|", "SensorName", "StringType", "Type"));
        for (Sensor sensor : sensors) {
            Log.v(TAG, String.format(Locale.getDefault(), "|%-35s|%-38s|%-6s|", sensor.getName(), sensor.getStringType(), sensor.getType()));
        }
        Log.d(TAG, "=== LIST AVAILABLE SENSORS ===");
    }
}