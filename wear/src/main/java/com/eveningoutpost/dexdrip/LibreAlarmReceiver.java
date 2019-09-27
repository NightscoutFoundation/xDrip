
package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.SensorSanity;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Forecast;
import com.eveningoutpost.dexdrip.Models.GlucoseData;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.LibreBlock;
import com.eveningoutpost.dexdrip.Models.LibreOOPAlgorithm;
import com.eveningoutpost.dexdrip.Models.ReadingData;
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

import static com.eveningoutpost.dexdrip.UtilityModels.Constants.LIBRE_MULTIPLIER;
import static com.eveningoutpost.dexdrip.xdrip.gs;

/**
 * Created by jamorham on 04/09/2016.
 */
public class LibreAlarmReceiver extends BroadcastReceiver {


    private static final String TAG = "jamorham librereceiver";
    private static final boolean debug = false;
    private static final boolean d = true;
    private static final boolean use_raw_ = true;
    private static final long segmentation_timeslice = (long)(Constants.MINUTE_IN_MS * 4.5);
    private static SharedPreferences prefs;
    private static long oldest = -1;
    private static long newest = -1;
    private static long oldest_cmp = -1;
    private static long newest_cmp = -1;
    private static final Object lock = new Object();
    private static long sensorAge = 0;
    private static long timeShiftNearest = -1;

    public static void clearSensorStats() {
        Pref.setInt("nfc_sensor_age", 0); // reset for nfc sensors
        sensorAge = 0;
    }

    private static double convert_for_dex(int lib_raw_value) {
        return (lib_raw_value * LIBRE_MULTIPLIER); // to match (raw/8.5)*1000
    }

    private static void createBGfromGD(GlucoseData gd, boolean use_smoothed_data, boolean quick) {
        final double converted;
        if (gd.glucoseLevelRaw > 0) {
            if(use_smoothed_data) {
                converted = convert_for_dex(gd.glucoseLevelRawSmoothed);
                Log.e(TAG,"Using smoothed value " + converted + " instead of " + convert_for_dex(gd.glucoseLevelRaw) );
            } else {
                converted = convert_for_dex(gd.glucoseLevelRaw);
            }
        } else {
            converted = 12; // RF error message - might be something else like unconstrained spline
        }
        if (gd.realDate > 0) {
            //   Log.d(TAG, "Raw debug: " + JoH.dateTimeText(gd.realDate) + " raw: " + gd.glucoseLevelRaw + " converted: " + converted);
            if ((newest_cmp == -1) || (oldest_cmp == -1) || (gd.realDate < oldest_cmp) || (gd.realDate > newest_cmp)) {
                // if (BgReading.readingNearTimeStamp(gd.realDate) == null) {
                if ((gd.realDate < oldest) || (oldest == -1)) oldest = gd.realDate;
                if ((gd.realDate > newest) || (newest == -1)) newest = gd.realDate;

                if (BgReading.getForPreciseTimestamp(gd.realDate, segmentation_timeslice, false) == null) {
                    Log.d(TAG, "Creating bgreading at: " + JoH.dateTimeText(gd.realDate));
                    BgReading.create(converted, converted, xdrip.getAppContext(), gd.realDate, quick); // quick lite insert
                } else {
                    if (d)
                        Log.d(TAG, "Ignoring duplicate timestamp for: " + JoH.dateTimeText(gd.realDate));
                }
            } else {
                if (d)
                    Log.d(TAG, "Already processed from date range: " + JoH.dateTimeText(gd.realDate));
            }
        } else {
            Log.e(TAG, "Fed a zero or negative date");
        }
        if (d)
            Log.d(TAG, "Oldest : " + JoH.dateTimeText(oldest_cmp) + " Newest : " + JoH.dateTimeText(newest_cmp));
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Thread() {
            @Override
            public void run() {
                final PowerManager.WakeLock wl = JoH.getWakeLock("librealarm-receiver", 60000);
                synchronized (lock) {
                    try {

                        Log.d(TAG, "LibreReceiver onReceiver: " + intent.getAction());
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
                            case Intents.LIBRE_ALARM_TO_XDRIP_PLUS:

                                // If we are not currently in a mode supporting libre then switch
                                if (!DexCollectionType.hasLibre()) {
                                    DexCollectionType.setDexCollectionType(DexCollectionType.LibreAlarm);
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
                                    final ReadingData.TransferObject object =
                                            new Gson().fromJson(data, ReadingData.TransferObject.class);
                                    object.data.CalculateSmothedData();
                                    processReadingDataTransferObject(object, JoH.tsl(), "LibreAlarm", false);
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

    public static void processReadingDataTransferObject(ReadingData.TransferObject object, long CaptureDateTime, String tagid, boolean allowUpload) {
    	Log.i(TAG, "Data that was recieved from librealarm is " + HexDump.dumpHexString(object.data.raw_data));
    	// Save raw block record (we start from block 0)
        LibreBlock.createAndSave(tagid, CaptureDateTime, object.data.raw_data, 0, allowUpload);

        if(Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            if(object.data.raw_data == null) {
                Log.e(TAG, "Please update LibreAlarm to use OOP algorithm");
                JoH.static_toast_long(gs(R.string.please_update_librealarm_to_use_oop_algorithm));
                return;
            }
            LibreOOPAlgorithm.SendData(object.data.raw_data, CaptureDateTime);
            return;
        }
        CalculateFromDataTransferObject(object, use_raw_);
    }
        
    public static void CalculateFromDataTransferObject(ReadingData.TransferObject object, boolean use_raw) {
    	boolean use_smoothed_data = Pref.getBooleanDefaultFalse("libre_use_smoothed_data");
        // insert any recent data we can
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
                Log.wtf(TAG, "Sensor age has not advanced: " + sensorAge);
                JoH.static_toast_long(gs(R.string.sensor_clock_has_not_advanced));
                Pref.setBoolean("nfc_age_problem", true);
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

                shiftx = getTimeShift(mTrend);
                if (shiftx != 0) Log.d(TAG, "Lag Timeshift: " + shiftx);
                //applyTimeShift(mTrend, shiftx);

                for (GlucoseData gd : mTrend) {
                    if (d) Log.d(TAG, "DEBUG: sensor time: " + gd.sensorTime);
                    if ((timeShiftNearest > 0) && ((timeShiftNearest - gd.realDate) < segmentation_timeslice) && (timeShiftNearest - gd.realDate != 0)) {
                        if (d)
                            Log.d(TAG, "Skipping record due to closeness: " + JoH.dateTimeText(gd.realDate));
                        continue;
                    }
                    if (use_raw) {
                        createBGfromGD(gd, use_smoothed_data, false); // not quick for recent
                    } else {
                        BgReading.bgReadingInsertFromInt(gd.glucoseLevel, gd.realDate, true);
                    }
                }
            } else {
                Log.e(TAG, "Trend data was empty!");
            }

            // munge and insert the history data if any is missing
            final List<GlucoseData> mHistory = object.data.history;
            if ((mHistory != null) && (mHistory.size() > 1)) {
                Collections.sort(mHistory);
                //applyTimeShift(mTrend, shiftx);
                final List<Double> polyxList = new ArrayList<Double>();
                final List<Double> polyyList = new ArrayList<Double>();
                for (GlucoseData gd : mHistory) {
                    if (d)
                        Log.d(TAG, "history : " + JoH.dateTimeText(gd.realDate) + " " + gd.glucose(false));
                    polyxList.add((double) gd.realDate);
                    if (use_raw) {
                        polyyList.add((double) gd.glucoseLevelRaw);
                        // For history, data is already averaged, no need for us to use smoothed data
                        createBGfromGD(gd, false, true);
                    } else {
                        polyyList.add((double) gd.glucoseLevel);
                        // add in the actual value
                        BgReading.bgReadingInsertFromInt(gd.glucoseLevel, gd.realDate, false);
                    }
                }

                //ConstrainedSplineInterpolator splineInterp = new ConstrainedSplineInterpolator();
                final SplineInterpolator splineInterp = new SplineInterpolator();

                if(polyxList.size() >= 3) {
                    // The need to have at least 3 points is a demand from the interpolate function.
                    try {
                        PolynomialSplineFunction polySplineF = splineInterp.interpolate(
                                Forecast.PolyTrendLine.toPrimitiveFromList(polyxList),
                                Forecast.PolyTrendLine.toPrimitiveFromList(polyyList));
    
                        final long startTime = mHistory.get(0).realDate;
                        final long endTime = mHistory.get(mHistory.size() - 1).realDate;
    
                        for (long ptime = startTime; ptime <= endTime; ptime += 300000) {
                            if (d)
                                Log.d(TAG, "Spline: " + JoH.dateTimeText((long) ptime) + " value: " + (int) polySplineF.value(ptime));
                            if (use_raw) {
                                // Here we do not use smoothed data, since data is already smoothed for the history
                                createBGfromGD(new GlucoseData((int) polySplineF.value(ptime), ptime), false, true);
                            } else {
                                BgReading.bgReadingInsertFromInt((int) polySplineF.value(ptime), ptime, false);
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

    private static long getTimeShift(List<GlucoseData> gds) {
        long nearest = -1;
        for (GlucoseData gd : gds) {
            if (gd.realDate > nearest) nearest = gd.realDate;
        }
        timeShiftNearest = nearest;
        if (nearest > 0) {
            final long since = JoH.msSince(nearest);
            if ((since > 0) && (since < Constants.MINUTE_IN_MS * 5)) {
                return since;
            }
        }
        return 0;
    }

    private static void applyTimeShift(List<GlucoseData> gds, long timeshift) {
        if (timeshift == 0) return;
        for (GlucoseData gd : gds) {
            gd.realDate = gd.realDate + timeshift;
        }
    }
}

