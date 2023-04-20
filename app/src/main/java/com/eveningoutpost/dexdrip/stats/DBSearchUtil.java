package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import com.activeandroid.Cache;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

/**
 * Created by adrian on 30/06/15.
 */
public class DBSearchUtil {

    public static final String CUTOFF = "38";


    public static int noReadingsAboveRange(Context context) {
        Bounds bounds = new Bounds().invoke();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean mgdl = "mgdl".equals(settings.getString("units", "mgdl"));

        double high = Double.parseDouble(settings.getString("highValue", "170"));
        if (!mgdl) {
            high *= Constants.MMOLL_TO_MGDL;
        }

        int count = new Select()
                .from(BgReading.class)
                .where("timestamp >= " + bounds.start)
                .where("timestamp <= " + bounds.stop)
                .where("calculated_value > " + CUTOFF)
                .where("calculated_value > " + high)
                .where("snyced == 0").count();
        Log.d("DrawStats", "High count: " + count);
        return count;
    }


    public static List<BgReadingStats> getReadings(boolean ordered) {
        try {
            Bounds bounds = new Bounds().invoke();

            String orderBy = ordered ? "calculated_value desc" : null;

            SQLiteDatabase db = Cache.openDatabase();
            Cursor cur = db.query("bgreadings", new String[]{"timestamp", "calculated_value"}, "timestamp >= ? AND timestamp <=  ? AND calculated_value > ? AND snyced == 0", new String[]{"" + bounds.start, "" + bounds.stop, CUTOFF}, null, null, orderBy);
            List<BgReadingStats> readings = new Vector<BgReadingStats>();
            BgReadingStats reading;
            if (cur.moveToFirst()) {
                do {
                    reading = new BgReadingStats();
                    reading.timestamp = (Long.parseLong(cur.getString(0)));
                    reading.calculated_value = (Double.parseDouble(cur.getString(1)));
                    readings.add(reading);
                } while (cur.moveToNext());
            }
            return readings;

        } catch (Exception e) {
            JoH.static_toast_long(e.getMessage());
            return null;
        }
    }

    public static List<BgReadingStats> getFilteredReadingsWithFallback(boolean ordered) {
        try {
            Bounds bounds = new Bounds().invoke();

            String orderBy = ordered ? "calculated_value desc" : null;

            SQLiteDatabase db = Cache.openDatabase();
            Cursor cur = db.query("bgreadings", new String[]{"timestamp", "calculated_value", "filtered_calculated_value"}, "timestamp >= ? AND timestamp <=  ? AND calculated_value > ? AND snyced == 0", new String[]{"" + bounds.start, "" + bounds.stop, CUTOFF}, null, null, orderBy);
            List<BgReadingStats> readings = new Vector<BgReadingStats>();
            BgReadingStats reading;
            if (cur.moveToFirst()) {
                do {
                    reading = new BgReadingStats();
                    reading.timestamp = (Long.parseLong(cur.getString(0)));

                    reading.calculated_value = (Double.parseDouble(cur.getString(2)));
                    if(reading.calculated_value == 0)
                        reading.calculated_value = (Double.parseDouble(cur.getString(1)));

                    readings.add(reading);
                } while (cur.moveToNext());
            }
            return readings;

        } catch (Exception e) {
            JoH.static_toast_long(e.getMessage());
            return null;
        }
    }


    public static int noReadingsInRange(Context context) {
        Bounds bounds = new Bounds().invoke();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean mgdl = "mgdl".equals(settings.getString("units", "mgdl"));

        double high = Double.parseDouble(settings.getString("highValue", "170"));
        double low = Double.parseDouble(settings.getString("lowValue", "70"));
        if (!mgdl) {
            high *= Constants.MMOLL_TO_MGDL;
            low *= Constants.MMOLL_TO_MGDL;

        }
        int count = new Select()
                .from(BgReading.class)
                .where("timestamp >= " + bounds.start)
                .where("timestamp <= " + bounds.stop)
                .where("calculated_value > " + CUTOFF)
                .where("calculated_value <= " + high)
                .where("calculated_value >= " + low)
                .where("snyced == 0")
                .count();
        Log.d("DrawStats", "In count: " + count);

        return count;
    }

    public static int noReadingsBelowRange(Context context) {
        Bounds bounds = new Bounds().invoke();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean mgdl = "mgdl".equals(settings.getString("units", "mgdl"));

        double low = Double.parseDouble(settings.getString("lowValue", "70"));
        if (!mgdl) {
            low *= Constants.MMOLL_TO_MGDL;

        }
        int count = new Select()
                .from(BgReading.class)
                .where("timestamp >= " + bounds.start)
                .where("timestamp <= " + bounds.stop)
                .where("calculated_value > " + CUTOFF)
                .where("calculated_value < " + low)
                .where("snyced == 0")
                .count();
        Log.d("DrawStats", "Low count: " + count);

        return count;
    }


    public static long getTodayTimestamp() {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTimeInMillis();
    }

    public static long getYesterdayTimestamp() {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.add(Calendar.DATE, -1);
        return date.getTimeInMillis();
    }

    public static long getXDaysTimestamp(int x) {
        Calendar date = new GregorianCalendar();
        date.add(Calendar.DATE, -x);
        return date.getTimeInMillis();
    }

    private static class Bounds {
        private long stop;
        private long start;

        public long getStop() {
            return stop;
        }

        public long getStart() {
            return start;
        }

        public Bounds invoke() {
            stop = System.currentTimeMillis();
            start = System.currentTimeMillis();

            switch (StatsActivity.state) {
                case StatsActivity.TODAY:
                    start = getTodayTimestamp();
                    break;
                case StatsActivity.YESTERDAY:
                    start = getYesterdayTimestamp();
                    stop = getTodayTimestamp();
                    break;
                case StatsActivity.D7:
                    start = getXDaysTimestamp(7);
                    break;
                case StatsActivity.D30:
                    start = getXDaysTimestamp(30);
                    break;
                case StatsActivity.D90:
                    start = getXDaysTimestamp(90);
                    break;
            }
            return this;
        }
    }
}