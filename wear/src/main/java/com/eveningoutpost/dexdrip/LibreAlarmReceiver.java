
package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.models.SensorSanity;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Forecast;
import com.eveningoutpost.dexdrip.models.GlucoseData;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.models.ReadingData;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.CheckBridgeBattery;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.LibreTrendUtil;
import com.google.gson.Gson;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import com.eveningoutpost.dexdrip.utils.LibreTrendPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.LIBRE_MULTIPLIER;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import lombok.val;

/**
 * Created by jamorham on 04/09/2016.
 */
public class LibreAlarmReceiver extends BroadcastReceiver {


    private static final String TAG = "jamorham librereceiver";
    private static final boolean debug = false;
    private static final boolean d = true;
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

    private static boolean useGlucoseAsRaw() {
        return Pref.getString("calibrate_external_libre_2_algorithm_type","calibrate_raw").equals("calibrate_glucose");
    }

    private static void createBGfromGD(GlucoseData gd, boolean use_smoothed_data, boolean quick) {
        final double converted;
        if (useGlucoseAsRaw()) {
            // if treating converted value as raw
            if (gd.glucoseLevel > 0) {
                if (use_smoothed_data && gd.glucoseLevelSmoothed > 0) {
                    converted  = gd.glucoseLevelSmoothed * 1000;
                    Log.e(TAG, "Using smoothed value as raw " + converted + " instead of " + gd.glucoseLevel);
                } else {
                    converted = gd.glucoseLevel * 1000;
                }
            } else {
                converted = 12; // RF error message - might be something else like unconstrained spline
            }
        } else {
        if (gd.glucoseLevelRaw > 0) {
                if (use_smoothed_data && gd.glucoseLevelRawSmoothed > 0) {
                converted = convert_for_dex(gd.glucoseLevelRawSmoothed);
                    Log.d(TAG, "Using smoothed value " + converted + " instead of " + convert_for_dex(gd.glucoseLevelRaw) + gd);
            } else {
                converted = convert_for_dex(gd.glucoseLevelRaw);
            }
        } else {
            converted = 12; // RF error message - might be something else like unconstrained spline
        }
        }
        if (gd.realDate > 0) {
            //   Log.d(TAG, "Raw debug: " + JoH.dateTimeText(gd.realDate) + " raw: " + gd.glucoseLevelRaw + " converted: " + converted);
            if ((newest_cmp == -1) || (oldest_cmp == -1) || (gd.realDate < oldest_cmp) || (gd.realDate > newest_cmp)) {
                // if (BgReading.readingNearTimeStamp(gd.realDate) == null) {
                if ((gd.realDate < oldest) || (oldest == -1)) oldest = gd.realDate;
                if ((gd.realDate > newest) || (newest == -1)) newest = gd.realDate;

                if (BgReading.getForPreciseTimestamp(gd.realDate, DexCollectionType.getCurrentDeduplicationPeriod(), false) == null) {
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

                                final String data = bundle.getString("data");
                                final int bridge_battery = bundle.getInt("bridge_battery");
                                if (bridge_battery > 0) {
                                    Pref.setInt("bridge_battery", bridge_battery);
                                    CheckBridgeBattery.checkBridgeBattery();
                                }
                                try {

                                    final ReadingData.TransferObject object =
                                            new Gson().fromJson(data, ReadingData.TransferObject.class);
                                    if (object.data.raw_data == null) {
                                        Log.e(TAG, "Please update LibreAlarm to use OOP algorithm");
                                        JoH.static_toast_long(gs(R.string.please_update_librealarm_to_use_oop_algorithm));
                                        break;
                                    }
                                    NFCReaderX.HandleGoodReading("LibreAlarm", object.data.raw_data, JoH.tsl(), false, null, null);
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

    public static void processReadingDataTransferObject(ReadingData readingData, long CaptureDateTime, String tagid, boolean allowUpload, byte[] patchUid, byte[] patchInfo, boolean bg_val_exists) {
        Log.d(TAG, "Data that was recieved from librealarm is " + HexDump.dumpHexString(readingData.raw_data));
    	// Save raw block record (we start from block 0)
        LibreBlock libreBlock = LibreBlock.createAndSave(tagid, CaptureDateTime, readingData.raw_data, 0, allowUpload, patchUid, patchInfo);

        LibreTrendUtil libreTrendUtil = LibreTrendUtil.getInstance();
        // Get the data for the last 24 hours, as this affects the cache.
        List<LibreTrendPoint> libreTrendPoints = libreTrendUtil.getData(JoH.tsl() - Constants.DAY_IN_MS, JoH.tsl(), true);

        readingData.ClearErrors(libreTrendPoints);
        // This is not a perfect solution, but it should work well in almost all casses except restart.
        // (after restart we will only have data of one reading).
        readingData.copyBgVals(libreTrendPoints);

        boolean use_smoothed_data = Pref.getBooleanDefaultFalse("libre_use_smoothed_data");
        if (use_smoothed_data) {
            readingData.calculateSmoothDataImproved(libreTrendPoints, bg_val_exists);
            }
        if (Pref.getBooleanDefaultFalse("external_blukon_algorithm") != false) {
            Log.wtf(TAG, "Error external_blukon_algorithm should be false here");
        }
        boolean use_raw = !Pref.getString("calibrate_external_libre_2_algorithm_type", "calibrate_raw").equals("no_calibration");
        CalculateFromDataTransferObject(readingData, use_smoothed_data, use_raw);
    }
        
    public static void CalculateFromDataTransferObject(ReadingData readingData, boolean use_smoothed_data, boolean use_raw) {
        Log.i(TAG, "CalculateFromDataTransferObject called");
        // insert any recent data we can
        val segmentation_timeslice = DexCollectionType.getCurrentDeduplicationPeriod();
        final List<GlucoseData> mTrend = readingData.trend;
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
            if (mTrend.size() > 0) {

                // This function changes timeShiftNearest which is the latest value we have, so we are far enough from it.
                getTimeShift(mTrend);

                for (GlucoseData gd : mTrend) {
                    if (d) Log.d(TAG, "DEBUG: sensor time: " + gd.sensorTime);
                    if ((timeShiftNearest > 0) && ((timeShiftNearest - gd.realDate) < segmentation_timeslice) && (timeShiftNearest - gd.realDate != 0)) {
                        if (d)
                            Log.d(TAG, "Skipping record due to closeness to the most recent value: " + JoH.dateTimeText(gd.realDate));
                        continue;
                    }
                    if (use_raw) {
                        createBGfromGD(gd, use_smoothed_data, false); // not quick for recent
                    } else {
                        BgReading.bgReadingInsertFromInt(use_smoothed_data ? gd.glucoseLevelSmoothed : gd.glucoseLevel, gd.realDate, segmentation_timeslice, true);
                    }
                }
            } else {
                Log.e(TAG, "Trend data was empty!");
            }

            // munge and insert the history data if any is missing
            insertFromHistory(readingData.history, use_raw);
        } else {
            Log.d(TAG, "Trend data is null!");
        }
    }


    public static void insertFromHistory(final List<GlucoseData> mHistory, final boolean use_raw) {
        val timeslice = DexCollectionType.getCurrentDeduplicationPeriod();
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
                    BgReading.bgReadingInsertFromInt(gd.glucoseLevel, gd.realDate, timeslice, false);
                    }
                }

            val period = DexCollectionType.getCurrentSamplePeriod();

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
    
                    for (long ptime = startTime; ptime <= endTime; ptime += period) {
                            if (d)
                                Log.d(TAG, "Spline: " + JoH.dateTimeText((long) ptime) + " value: " + (int) polySplineF.value(ptime));
                            if (use_raw) {
                                // Here we do not use smoothed data, since data is already smoothed for the history
                                createBGfromGD(new GlucoseData((int) polySplineF.value(ptime), ptime), false, true);
                            } else {
                            BgReading.bgReadingInsertFromInt((int) polySplineF.value(ptime), ptime, timeslice, false);
                            }
                        }
                    } catch (org.apache.commons.math3.exception.NonMonotonicSequenceException e) {
                        Log.e(TAG, "NonMonotonicSequenceException: " + e);
                    }
                }
        } else {
            Log.e(TAG, "no  history data");
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
}

