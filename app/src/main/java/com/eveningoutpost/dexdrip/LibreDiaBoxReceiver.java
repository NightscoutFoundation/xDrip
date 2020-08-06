
package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Forecast;
import com.eveningoutpost.dexdrip.Models.GlucoseData;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.ReadingData;
import com.eveningoutpost.dexdrip.Models.SensorSanity;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.utils.CheckBridgeBattery;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.google.gson.Gson;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.eveningoutpost.dexdrip.xdrip.gs;

/**
 *
 */
public class LibreDiaBoxReceiver extends BroadcastReceiver {


    private static final String TAG = "jamorham LibreDiaboxReceiver";
    private static final boolean debug = false;
    private static final boolean d = false;
    private static final long segmentation_timeslice = (long) (Constants.MINUTE_IN_MS * 4.5);
    private static SharedPreferences prefs;
    private static long oldest = -1;
    private static long newest = -1;
    private static long oldest_cmp = -1;
    private static long newest_cmp = -1;
    private static final Object lock = new Object();
    private static long sensorAge = 0;
    private static long timeShiftNearest = -1;


    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Thread() {
            @Override
            public void run() {
                final PowerManager.WakeLock wl = JoH.getWakeLock("librealarm-receiver", 60000);
                synchronized (lock) {
                    try {

                        Log.e(TAG, "LibreReceiver onReceiver: " + intent.getAction());
                        JoH.benchmark(null);
                        // check source
                        if (prefs == null)
                            prefs = PreferenceManager.getDefaultSharedPreferences(context);

                        final Bundle bundle = intent.getExtras();
                        //  BundleScrubber.scrub(bundle);
                        final String action = intent.getAction();


                        if ((bundle != null) && (debug)) {
                            for (String key : bundle.keySet()) {
                                Object value = bundle.get(key);
                                if (value != null) {
                                    Log.d(TAG, String.format("%s %s (%s)", key,
                                            value.toString(), value.getClass().getName()));
                                }
                            }
                        }

                        switch (action) {
                            case Intents.LIBRE_DIABOX_TO_XDRIP_PLUS:
                                if (!DexCollectionType.hasLibre()) {
                                    DexCollectionType.setDexCollectionType(DexCollectionType.LibreDiaBox);
                                }
                                if (bundle == null) break;
                                Log.d(TAG, "Receiving LIBRE_ALARM broadcast");
                                oldest_cmp = oldest;
                                newest_cmp = newest;
                                Log.d(TAG, "At Start: Oldest : " + JoH.dateTimeText(oldest_cmp) + " Newest : " + JoH.dateTimeText(newest_cmp));

                                final String data = bundle.getString("data");
                                final int bridge_battery = bundle.getInt("bridge_battery");
                                if (bridge_battery > 0) {
                                    Pref.setInt("bridge_battery", bridge_battery);
                                    CheckBridgeBattery.checkBridgeBattery();
                                }
                                try {
                                    Log.e(TAG, "LibreReceiver onReceiver: " + data);
                                    final ReadingData.TransferObject object =
                                            new Gson().fromJson(data, ReadingData.TransferObject.class);
                                    object.data.CalculateSmothedData();
                                    processReadingDataTransferObject(object);
                                    Log.d(TAG, "At End: Oldest : " + JoH.dateTimeText(oldest_cmp) + " Newest : " + JoH.dateTimeText(newest_cmp));
                                } catch (Exception e) {
                                    Log.wtf(TAG, "Could not process data structure from LibreAlarm: " + e.toString());
                                    JoH.static_toast_long(gs(R.string.librealarm_data_format_appears_incompatible_protocol_changed_or_no_data));
                                }
                                break;

                            default:
                                Log.e(TAG, "Unknown action! " + action);
                                break;
                        }
                    } finally {
                        JoH.benchmark("LibreReceiver process");
                        JoH.releaseWakeLock(wl);
                    }
                } // lock
            }
        }.start();
    }

    public static void processReadingDataTransferObject(ReadingData.TransferObject object) {
        CalculateFromDataTransferObject(object);
    }

    public static void CalculateFromDataTransferObject(ReadingData.TransferObject object) {
        final List<GlucoseData> mTrend = object.data.trend;
        if (mTrend != null && mTrend.size() > 0) {
            Collections.sort(mTrend);
            final long thisSensorAge = mTrend.get(mTrend.size() - 1).sensorTime;
            sensorAge = Pref.getInt("nfc_sensor_age", 0);
            if (thisSensorAge > sensorAge || SensorSanity.allowTestingWithDeadSensor()) {
                sensorAge = thisSensorAge;
                Pref.setInt("nfc_sensor_age", (int) sensorAge);
                Pref.setBoolean("nfc_age_problem", false);
                Log.d(TAG, "Sensor age advanced to: " + thisSensorAge);
            } else if (thisSensorAge == sensorAge) {
                // This is only a problem if we don't have a recent reading. It could happen that we have a recent
                // reading, and then BT disconnects and connects after a few seconds, and we have a new reading (where 
                // sensor did not advance). This does not mean that a sensor is not advancing. Only if this is happening
                // for a few minutes, this is a problem.
                if (BgReading.getTimeSinceLastReading() > 11 * 60 * 1000) {
                    Log.wtf(TAG, "Sensor age has not advanced: " + sensorAge);
                    JoH.static_toast_long(gs(R.string.sensor_clock_has_not_advanced));
                    Pref.setBoolean("nfc_age_problem", true);
                }
                return; // do not try to insert again
            } else {
                Log.wtf(TAG, "Sensor age has gone backwards!!! " + sensorAge);
                JoH.static_toast_long(gs(R.string.sensor_age_has_gone_backwards));
                sensorAge = thisSensorAge;
                Pref.setInt("nfc_sensor_age", (int) sensorAge);
                Pref.setBoolean("nfc_age_problem", true);
            }
            if (d)
                Log.d(TAG, "Oldest cmp: " + JoH.dateTimeText(oldest_cmp) + " Newest cmp: " + JoH.dateTimeText(newest_cmp));
            long shiftx = 0;
            if (mTrend.size() > 0) {
                for (GlucoseData gd : mTrend) {
                    if (d) Log.d(TAG, "DEBUG: sensor time: " + gd.sensorTime);
                    if ((timeShiftNearest > 0) && ((timeShiftNearest - gd.realDate) < segmentation_timeslice) && (timeShiftNearest - gd.realDate != 0)) {
                        if (d)
                            Log.d(TAG, "Skipping record due to closeness: " + JoH.dateTimeText(gd.realDate));
                        continue;
                    }
                    double converted = gd.glucoseLevel * 1000;
                    BgReading.create(converted, converted, xdrip.getAppContext(), gd.realDate, false);
//                    BgReading.bgReadingInsertFromInt(gd.glucoseLevel, gd.realDate, true);
                }
            } else {
                Log.e(TAG, "Trend data was empty!");
            }

            // munge and insert the history data if any is missing
            final List<GlucoseData> mHistory = object.data.history;
            if ((mHistory != null) && (mHistory.size() > 1)) {
                Collections.sort(mHistory);
                final List<Double> polyxList = new ArrayList<Double>();
                final List<Double> polyyList = new ArrayList<Double>();
                for (GlucoseData gd : mHistory) {
                    if (d)
                        Log.d(TAG, "history : " + JoH.dateTimeText(gd.realDate) + " " + gd.glucose(false));
                    polyxList.add((double) gd.realDate);
                    polyyList.add((double) gd.glucoseLevel);

                    double converted = gd.glucoseLevel * 1000;
                    if (BgReading.readingNearTimeStamp(gd.realDate) == null) {
                        BgReading.create(converted, converted, xdrip.getAppContext(), gd.realDate, false);
                    } else {
                        UserError.Log.d(TAG, "Already a reading for minute offset: " + gd.realDate);
                    }
                }
                final SplineInterpolator splineInterp = new SplineInterpolator();
                if (polyxList.size() >= 3) {
                    try {
                        PolynomialSplineFunction polySplineF = splineInterp.interpolate(
                                Forecast.PolyTrendLine.toPrimitiveFromList(polyxList),
                                Forecast.PolyTrendLine.toPrimitiveFromList(polyyList));
                        final long startTime = mHistory.get(0).realDate;
                        final long endTime = mHistory.get(mHistory.size() - 1).realDate;
                        for (long ptime = startTime; ptime <= endTime; ptime += 300000) {
                            if (d)
                                Log.d(TAG, "Spline: " + JoH.dateTimeText((long) ptime) + " value: " + (int) polySplineF.value(ptime));

                            double converted = (int) polySplineF.value(ptime) * 1000;
                            if (BgReading.readingNearTimeStamp(ptime) == null) {
                                BgReading.create(converted, converted, xdrip.getAppContext(), ptime, false);
                            } else {
                                UserError.Log.d(TAG, "Already a reading for minute offset: " + ptime);
                            }
                        }
                    } catch (org.apache.commons.math3.exception.NonMonotonicSequenceException e) {
                        Log.e(TAG, "NonMonotonicSequenceException: " + e);
                    }
                }
            } else {
                Log.e(TAG, "no librealarm history data");
            }
        } else {
            Log.d(TAG, "Trend data is null!");
        }
    }


}

