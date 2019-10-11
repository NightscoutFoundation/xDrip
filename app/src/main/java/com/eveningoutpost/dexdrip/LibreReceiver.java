package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Libre2RawValue;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.eveningoutpost.dexdrip.Home.get_engineering_mode;
import static com.eveningoutpost.dexdrip.Models.BgReading.bgReadingInsertFromJson;
import static com.eveningoutpost.dexdrip.Models.Libre2Sensor.Libre2Sensors;

/**
 * Created by jamorham on 14/11/2016.
 */

public class LibreReceiver extends BroadcastReceiver {

    private static final String TAG = "xdrip libre_receiver";
    private static final boolean debug = false;
    private static final boolean d = false;
    private static SharedPreferences prefs;
    private static final Object lock = new Object();
    private static String libre_calc_doku="wait for next reading...";
    private static long last_reading=0;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if(DexCollectionType.getDexCollectionType() != DexCollectionType.LibreReceiver)
            return;
        new Thread() {
            @Override
            public void run() {
                PowerManager.WakeLock wl = JoH.getWakeLock("libre-receiver", 60000);
                synchronized (lock) {
                    try {

                        Log.d(TAG, "libre onReceiver: " + intent.getAction());
                        JoH.benchmark(null);
                        // check source
                        if (prefs == null)
                            prefs = PreferenceManager.getDefaultSharedPreferences(context);

                        final Bundle bundle = intent.getExtras();
                        //  BundleScrubber.scrub(bundle);
                        final String action = intent.getAction();



                        if (action == null) return;

                        switch (action) {
                            case Intents.LIBRE2_ACTIVATION:
                                Log.v(TAG, "Receiving LibreData activation");
                                saveSensorStartTime(intent.getBundleExtra("sensor"), intent.getBundleExtra("bleManager").getString("sensorSerial"));
                                break;

                            case Intents.LIBRE2_BG:
                                Libre2RawValue currentRawValue = processIntent(intent);
                                if (currentRawValue == null) return;
                                Log.v(TAG,"got bg reading: from sensor:"+currentRawValue.serial+" rawValue:"+currentRawValue.glucose+" at:"+currentRawValue.timestamp);
                                // period of 4.5 minutes to collect 5 readings
                                if(!BgReading.last_within_millis(45 * 6 * 1000 )) {
                                    List<Libre2RawValue> smoothingValues = Libre2RawValue.last20Minutes();
                                    smoothingValues.add(currentRawValue);
                                    processValues(currentRawValue, smoothingValues, context);
                                }
                                currentRawValue.save();

                                break;

                            default:
                                Log.e(TAG, "Unknown action! " + action);
                                break;
                        }
                    } finally {
                        JoH.benchmark("NSEmulator process");
                        JoH.releaseWakeLock(wl);
                    }
                } // lock
            }
        }.start();
    }

    private static Libre2RawValue processIntent(Intent intent) {
        Bundle sas = intent.getBundleExtra("sas");
        if (sas != null) saveSensorStartTime(sas.getBundle("currentSensor"),intent.getBundleExtra("bleManager").getString("sensorSerial"));
        if (!intent.hasExtra("glucose") || !intent.hasExtra("timestamp") || !intent.hasExtra("bleManager")) {
            Log.e(TAG,"Received faulty intent from LibreLink.");
            return null;
        }
        double glucose = intent.getDoubleExtra("glucose", 0);
        long timestamp = intent.getLongExtra("timestamp", 0);
        last_reading = timestamp;
        String serial = intent.getBundleExtra("bleManager").getString("sensorSerial");
        if (serial == null) {
            Log.e(TAG,"Received faulty intent from LibreLink.");
            return null;
        }
        Libre2RawValue rawValue = new Libre2RawValue();
        rawValue.timestamp = timestamp;
        rawValue.glucose = glucose;
        rawValue.serial = serial;
        return rawValue;
    }
    private static void processValues(Libre2RawValue currentValue, List<Libre2RawValue> smoothingValues, Context context) {
        if (Sensor.currentSensor() == null) {
            Sensor.create(currentValue.timestamp, currentValue.serial);

        }

        double value = calculateWeightedAverage(smoothingValues, currentValue.timestamp);

        BgReading.bgReadingInsertLibre2(value, currentValue.timestamp,currentValue.glucose);
    }

    private static void saveSensorStartTime(Bundle sensor, String serial) {
        if (sensor != null && sensor.containsKey("sensorStartTime")) {
            long sensorStartTime = sensor.getLong("sensorStartTime");

            Sensor last = Sensor.currentSensor();
            if(last!=null) {
                if (!last.uuid.equals(serial)) {
                    Sensor.stopSensor();
                    last = null;
                }
            }

            if(last==null) {
                Sensor.create(sensorStartTime,serial);
            }
        }
    }
    private static long SMOOTHING_DURATION = TimeUnit.MINUTES.toMillis(25);


    private static double calculateWeightedAverage(List<Libre2RawValue> rawValues, long now) {
        double sum = 0;
        double weightSum = 0;
        DecimalFormat longformat = new DecimalFormat( "#,###,###,##0.00" );

        libre_calc_doku="";
        for (Libre2RawValue rawValue : rawValues) {
            double weight = 1 - ((now - rawValue.timestamp) / (double) SMOOTHING_DURATION);
            sum += rawValue.glucose * weight;
            weightSum += weight;
            libre_calc_doku += DateFormat.format("kk:mm:ss :",rawValue.timestamp) + " w:" + longformat.format(weight) +" raw: " + rawValue.glucose  + "\n" ;
           }
        return Math.round(sum / weightSum);
    }


    public static List<StatusItem> megaStatus() {
        final List<StatusItem> l = new ArrayList<>();

        l.add(new StatusItem("Libre2 Sensor",Sensor.currentSensor().uuid + "\nStart: " +  DateFormat.format("dd.MM.yyyy kk:mm",Sensor.currentSensor().started_at) ));
        String lastReading ="";
        try {
            lastReading = DateFormat.format("dd.MM.yyyy kk:mm:ss", last_reading).toString();
            l.add(new StatusItem("Last Reading", lastReading));
        } catch (Exception e) {
            Log.e(TAG, "Error readlast: " + e);
        }
        if (get_engineering_mode()) {
            l.add(new StatusItem("Last Calc.", libre_calc_doku));
        }
        if (Pref.getBooleanDefaultFalse("Libre2_showSensors")) {
            l.add(new StatusItem("Sensors", Libre2Sensors()));
        }
        return l;
    }
}
