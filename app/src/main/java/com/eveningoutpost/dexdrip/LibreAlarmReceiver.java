
package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Forecast;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.google.gson.Gson;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by jamorham on 04/09/2016.
 */
public class LibreAlarmReceiver extends BroadcastReceiver {


    private static final String TAG = "jamorham librereceiver";
    private static final boolean debug = false;
    private static final boolean d = false;
    private static final boolean use_raw = true;
    private static SharedPreferences prefs;
    private static long oldest = -1;
    private static long newest = -1;
    private static long oldest_cmp = -1;
    private static long newest_cmp = -1;
    private static final Object lock = new Object();

    private static double convert_for_dex(int lib_raw_value)
    {
        return (lib_raw_value * 117.64705); // to match (raw/8.5)*1000
    }

    private static void createBGfromGD(GlucoseData gd) {
        final double converted;
        if (gd.glucoseLevelRaw > 0) {
            converted = convert_for_dex(gd.glucoseLevelRaw);
        } else {
            converted = 12; // RF error message - might be something else like unconstrained spline
        }
        if (gd.realDate>0) {
            Log.d(TAG, "Raw debug: " + JoH.dateTimeText(gd.realDate) + " raw: " + gd.glucoseLevelRaw + " converted: " + converted);
            if ((newest_cmp == -1) || (oldest_cmp == -1) || (gd.realDate<oldest_cmp) || (gd.realDate>newest_cmp)) {
                if (BgReading.readingNearTimeStamp(gd.realDate) == null) {
                    BgReading.create(converted, converted, xdrip.getAppContext(), gd.realDate);
                    if ((gd.realDate < oldest) || (oldest == -1)) oldest = gd.realDate;
                    if ((gd.realDate > newest) || (newest == -1)) newest = gd.realDate;
                } else {
                    Log.d(TAG, "Ignoring duplicate timestamp for: " + JoH.dateTimeText(gd.realDate));
                }
            } else {
                Log.d(TAG,"Already processed from date range: "+JoH.dateTimeText(gd.realDate));
            }
        } else {
            Log.e(TAG,"Fed a zero or negative date");
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Thread() {
            @Override
            public void run() {
                PowerManager.WakeLock wl = JoH.getWakeLock("librealarm-receiver", 60000);
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

                                final String data = bundle.getString("data");
                                final int bridge_battery = bundle.getInt("bridge_battery");
                                if (bridge_battery > 0)
                                    Home.setPreferencesInt("bridge_battery", bridge_battery);

                                try {
                                    ReadingData.TransferObject object =
                                            new Gson().fromJson(data, ReadingData.TransferObject.class);

                                    // insert any recent data we can
                                    final List<GlucoseData> mTrend = object.data.trend;
                                    if (mTrend != null) {
                                        Collections.sort(mTrend);
                                        for (GlucoseData gd : mTrend) {

                                            if (use_raw) {
                                                createBGfromGD(gd);
                                            } else {
                                                BgReading.bgReadingInsertFromInt(gd.glucoseLevel, gd.realDate, false);
                                            }
                                        }
                                    }
                                    // munge and insert the history data if any is missing
                                    final List<GlucoseData> mHistory = object.data.history;
                                    if ((mHistory != null) && (mHistory.size() > 1)) {
                                        Collections.sort(mHistory);

                                        final List<Double> polyxList = new ArrayList<Double>();
                                        final List<Double> polyyList = new ArrayList<Double>();
                                        for (GlucoseData gd : mHistory) {
                                            if (d)
                                                Log.d(TAG, "history : " + JoH.dateTimeText(gd.realDate) + " " + gd.glucose(true));
                                            polyxList.add((double) gd.realDate);
                                            if (use_raw) {
                                                polyyList.add((double) gd.glucoseLevelRaw);
                                                createBGfromGD(gd);
                                            } else {
                                                polyyList.add((double) gd.glucoseLevel);
                                                // add in the actual value
                                                BgReading.bgReadingInsertFromInt(gd.glucoseLevel, gd.realDate, false);
                                            }

                                        }

                                        //ConstrainedSplineInterpolator splineInterp = new ConstrainedSplineInterpolator();
                                        SplineInterpolator splineInterp = new SplineInterpolator();

                                        PolynomialSplineFunction polySplineF = splineInterp.interpolate(
                                                Forecast.PolyTrendLine.toPrimitiveFromList(polyxList),
                                                Forecast.PolyTrendLine.toPrimitiveFromList(polyyList));

                                        final long startTime = mHistory.get(0).realDate;
                                        final long endTime = mHistory.get(mHistory.size() - 1).realDate;

                                        for (long ptime = startTime; ptime <= endTime; ptime += 300000) {
                                            if (d)
                                                Log.d(TAG, "Spline: " + JoH.dateTimeText((long) ptime) + " value: " + (int) polySplineF.value(ptime));
                                            if (use_raw) {
                                                createBGfromGD(new GlucoseData((int) polySplineF.value(ptime), ptime));
                                            } else {
                                                BgReading.bgReadingInsertFromInt((int) polySplineF.value(ptime), ptime, false);
                                            }
                                        }

                                    } else {
                                        Log.e(TAG, "no librealarm history data");
                                    }

                                } catch (Exception e) {
                                    Log.wtf(TAG, "Could not process data structure from LibreAlarm: " + e.toString());
                                    JoH.static_toast_long("LibreAlarm data format appears incompatible!? protocol changed?");

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
            }}.start();
    }

    // Class definitions from LibreAlarm sharedata

    private static class GlucoseData implements Comparable<GlucoseData> {

        public long realDate;
        public String sensorId;
        public long sensorTime;
        public int glucoseLevel = -1;
        public int glucoseLevelRaw = -1;
        public long phoneDatabaseId;

        public GlucoseData() {
        }

        // jamorham added constructor
        public GlucoseData(int glucoseLevelRaw, long timestamp) {
            this.glucoseLevelRaw = glucoseLevelRaw;
            this.realDate = timestamp;
        }

        public String glucose(boolean mmol) {
            return glucose(glucoseLevel, mmol);
        }

        public static String glucose(int mgdl, boolean mmol) {
            return mmol ? new DecimalFormat("##.0").format(mgdl / 18f) : String.valueOf(mgdl);
        }

        @Override
        public int compareTo(GlucoseData another) {
            return (int) (realDate - another.realDate);
        }
    }

    private static class PredictionData extends GlucoseData {

        public enum Result {
            OK,
            ERROR_NO_NFC,
            ERROR_NFC_READ
        }

        public double trend = -1;
        public double confidence = -1;
        public Result errorCode;
        public int attempt;

        public PredictionData() {
        }

    }

    private static class ReadingData {

        public PredictionData prediction;
        public List<GlucoseData> trend;
        public List<GlucoseData> history;

        public ReadingData(PredictionData.Result result) {
            this.prediction = new PredictionData();
            this.prediction.realDate = System.currentTimeMillis();
            this.prediction.errorCode = result;
            this.trend = new ArrayList<>();
            this.history = new ArrayList<>();
        }

        public ReadingData(PredictionData prediction, List<GlucoseData> trend, List<GlucoseData> history) {
            this.prediction = prediction;
            this.trend = trend;
            this.history = history;
        }

        public ReadingData() {
        }

        public static class TransferObject {
            public long id;
            public ReadingData data;

            public TransferObject() {
            }

            public TransferObject(long id, ReadingData data) {
                this.id = id;
                this.data = data;
            }
        }

    }
}
