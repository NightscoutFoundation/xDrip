package com.eveningoutpost.dexdrip;

import static com.eveningoutpost.dexdrip.Home.get_engineering_mode;
import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.models.Libre2Sensor.Libre2Sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.GlucoseData;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Libre2RawValue;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utilitymodels.Unitized;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.val;

/**
 * Created by jamorham on 14/11/2016.
 */

public class LibreReceiver extends BroadcastReceiver {

    private static final String TAG = LibreReceiver.class.getSimpleName();
    private static final boolean d = false;
    private static final Object lock = new Object();
    private static volatile String libre_calc_doku = "wait for next reading...";
    private static volatile String bluetoothAddress = "";
    private static volatile String connectionState = "";
    private static volatile long last_reading = 0;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (DexCollectionType.getDexCollectionType() != DexCollectionType.LibreReceiver)
            return;
        new Thread() {
            @Override
            public void run() {
                PowerManager.WakeLock wl = JoH.getWakeLock("libre-receiver", 60000);
                synchronized (lock) {
                    try {

                        Log.d(TAG, "libre onReceiver: " + intent.getAction());
                        JoH.benchmark(null);

                        final String action = intent.getAction();

                        if (action == null) return;

                        switch (action) {
                            case Intents.LIBRE2_ACTIVATION:
                                Log.v(TAG, "Receiving LibreData activation");
                                try {
                                    saveSensorStartTime(intent.getBundleExtra("sensor"), intent.getBundleExtra("bleManager").getString("sensorSerial"));
                                } catch (NullPointerException e) {
                                    Log.e(TAG, "Null pointer in LIBRE2_ACTIVATION: " + e);
                                }
                                Sensor.createDefaultIfMissing();
                                break;

                            case Intents.LIBRE2_SCAN:
                                Log.v(TAG, "Receiving LibreData scan");
                                Sensor.createDefaultIfMissing();

                                try {
                                    val timeslice = DexCollectionType.getCurrentDeduplicationPeriod();
                                    val data = intent.getBundleExtra("sas").getBundle("realTimeGlucoseReadings");
                                    for (String key : data.keySet()) {
                                        val item = data.getBundle(key);
                                        val glucose = item.getDouble("glucoseValue");
                                        val timestamp = item.getLong("timestamp");
                                        if (d) UserError.Log.d(TAG, "Real time item: " + JoH.dateTimeText(timestamp) + " value: " + Unitized.unitized_string_static(glucose));
                                        BgReading.bgReadingInsertFromInt((int) Math.round(glucose), timestamp, timeslice, false);
                                    }

                                } catch (Exception e) {
                                    UserError.Log.e(TAG, "Got exception processing realtime: " + e);
                                }

                                try {
                                    val data = intent.getBundleExtra("sas").getBundle("historicGlucoseReadings");
                                    val gd = new ArrayList<GlucoseData>(data.size());
                                    for (String key : data.keySet()) {
                                        val item = data.getBundle(key);
                                        val glucose = item.getDouble("glucoseValue");
                                        val timestamp = item.getLong("timestamp");
                                        if (d) UserError.Log.d(TAG, "Historical item: " + JoH.dateTimeText(timestamp) + " value: " + Unitized.unitized_string_static(glucose));
                                        val g = new GlucoseData((int) Math.round(glucose), timestamp);
                                        g.glucoseLevel = g.glucoseLevelRaw;
                                        gd.add(g);
                                    }
                                    LibreAlarmReceiver.insertFromHistory(gd, false);

                                } catch (Exception e) {
                                    UserError.Log.e(TAG, "Got exception processing history: " + e);
                                }

                                Home.staticRefreshBGChartsOnIdle();
                                break;

                            case Intents.LIBRE2_CONNECTION:
                                JoH.dumpBundle(intent.getExtras(), TAG);
                                try {
                                    bluetoothAddress = intent.getBundleExtra("bleManager").getString("sensorAddress");
                                } catch (Exception e) {
                                    UserError.Log.e(TAG,"Exception parsing libre2connection sensorAddress: "+e);
                                }
                                try {
                                    connectionState = intent.getStringExtra("connectionState");
                                } catch (Exception e) {
                                    UserError.Log.e(TAG,"Exception parsing libre2connection connectionState: "+e);
                                }
                                break;

                            case Intents.LIBRE2_BG:
                                Libre2RawValue currentRawValue = processIntent(intent);
                                //JoH.dumpBundle(intent.getExtras(), TAG);

                                if (currentRawValue == null) return;

                                Log.v(TAG, "got bg reading: from sensor:" + currentRawValue.serial + " rawValue:" + currentRawValue.glucose + " at:" + currentRawValue.timestamp);
                                // period of 4.5 minutes to collect 5 readings
                                if (!BgReading.last_within_millis(DexCollectionType.getCurrentDeduplicationPeriod())) {
                                    long smoothing_minutes = Pref.getStringToInt("libre_filter_length", 25);
                                    long dataFetchInterval;
                                    if ( smoothing_minutes == 25L )
                                        dataFetchInterval = 20L;
                                    else
                                        dataFetchInterval = smoothing_minutes;
                                    List<Libre2RawValue> smoothingValues = Libre2RawValue.weightedAverageInterval(dataFetchInterval);
                                    smoothingValues.add(currentRawValue);
                                    processValues(currentRawValue, smoothingValues, smoothing_minutes, context);
                                }
                                currentRawValue.save();
                                clearNFCsensorAge();
                                break;

                            default:
                                Log.e(TAG, "Unknown action! " + action);
                                JoH.dumpBundle(intent.getExtras(), TAG);
                                break;
                        }
                    } finally {
                        JoH.benchmark(TAG);
                        JoH.releaseWakeLock(wl);
                    }
                } // lock
            }
        }.start();
    }

    private static void clearNFCsensorAge() {
        val PREF_KEY = "nfc_sensor_age";
        if (Pref.getInt(PREF_KEY, 0) != 0) {
            Pref.setInt(PREF_KEY, 0); // clear any nfc related sensor age cached from another collector
        }
    }

    private static Libre2RawValue processIntent(Intent intent) {
        Bundle sas = intent.getBundleExtra("sas");
        try {
            if (sas != null)
                saveSensorStartTime(sas.getBundle("currentSensor"), intent.getBundleExtra("bleManager").getString("sensorSerial"));
        } catch (NullPointerException e) {
            Log.e(TAG, "Null pointer exception in processIntent: " + e);
        }
        if (!intent.hasExtra("glucose") || !intent.hasExtra("timestamp") || !intent.hasExtra("bleManager")) {
            Log.e(TAG, "Received faulty intent from LibreLink.");
            return null;
        }
        double glucose = intent.getDoubleExtra("glucose", 0);
        long timestamp = intent.getLongExtra("timestamp", 0);
        last_reading = timestamp;
        String serial = intent.getBundleExtra("bleManager").getString("sensorSerial");
        if (serial == null) {
            Log.e(TAG, "Received faulty intent from LibreLink.");
            return null;
        }
        Libre2RawValue rawValue = new Libre2RawValue();
        rawValue.timestamp = timestamp;
        rawValue.glucose = glucose;
        rawValue.serial = serial;
        return rawValue;
    }

    private static void processValues(Libre2RawValue currentValue, List<Libre2RawValue> smoothingValues, long smoothing_minutes, Context context) {
        if (Sensor.currentSensor() == null) {
            Sensor.create(currentValue.timestamp, currentValue.serial);

        }

        double value = calculateWeightedAverage(smoothingValues, currentValue.timestamp, TimeUnit.MINUTES.toMillis(smoothing_minutes));
        BgReading.bgReadingInsertLibre2(value, currentValue.timestamp, currentValue.glucose);
    }

    private static void saveSensorStartTime(Bundle sensor, String serial) {
        if (sensor != null && sensor.containsKey("sensorStartTime")) {
            long sensorStartTime = sensor.getLong("sensorStartTime");

            Sensor last = Sensor.currentSensor();
            if (last != null) {
                if (!last.uuid.equals(serial)) {
                    Sensor.stopSensor();
                    last = null;
                }
            }

            if (last == null) {
                Sensor.create(sensorStartTime, serial);
            }
        }
    }

    private static double calculateWeightedAverage(List<Libre2RawValue> rawValues, long now, long smoothing_duration) {
        double sum = 0;
        double weightSum = 0;
        DecimalFormat longformat = new DecimalFormat("#,###,###,##0.00");

        libre_calc_doku = "";
        for (Libre2RawValue rawValue : rawValues) {
            double weight = 1 - ((now - rawValue.timestamp) / (double) smoothing_duration);
            sum += rawValue.glucose * weight;
            weightSum += weight;
            libre_calc_doku += DateFormat.format("kk:mm:ss :", rawValue.timestamp) + " w:" + longformat.format(weight) + " raw: " + rawValue.glucose + "\n";
        }
        return Math.round(sum / weightSum);
    }

    public static List<StatusItem> megaStatus() {
        final List<StatusItem> l = new ArrayList<>();
        final Sensor sensor = Sensor.currentSensor();
        if (sensor != null) {
            l.add(new StatusItem("Libre2 Sensor", sensor.uuid + "\nStart: " + DateFormat.format("dd.MM.yyyy kk:mm", sensor.started_at)));
        }
        if (last_reading > 0) {
            String lastReading = "";
            try {
                lastReading = DateFormat.format("dd.MM.yyyy kk:mm:ss", last_reading).toString();
                l.add(new StatusItem(xdrip.gs(R.string.last_reading), lastReading));
            } catch (Exception e) {
                Log.e(TAG, "Error readlast: " + e);
            }
        }
        if (!emptyString(connectionState)){
            l.add(new StatusItem("Bluetooth Link", connectionState));
        }
        if (get_engineering_mode()) {
            l.add(new StatusItem("Last Calc.", libre_calc_doku));
           if (!emptyString(bluetoothAddress)) {
               l.add(new StatusItem("Bluetooth Mac", bluetoothAddress));
           }
        }
        if (Pref.getBooleanDefaultFalse("Libre2_showSensors")) {
            l.add(new StatusItem("Sensors", Libre2Sensors()));
        }
        return l;
    }
}
