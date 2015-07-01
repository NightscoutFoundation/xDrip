package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Sensor;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by adrian on 30/06/15.
 */
public class DBSearchUtil {


    public static List<BgReading> readingsAfterTimestamp(long timestamp) {
        Sensor sensor = Sensor.currentSensor();
        return new Select()
                .from(BgReading.class)
                .where("timestamp >= " + timestamp)
                .where("calculated_value != 0")
                .orderBy("timestamp desc")
                .execute();
    }


    public static List<BgReading> sortedList(Context context) {
        long stop = System.currentTimeMillis();
        long start = System.currentTimeMillis();

        switch (StatsActivity.state){
            case StatsActivity.TODAY:
                start = getTodayTimestamp();
                break;
            case StatsActivity.YESTERDAY:
                start = getYesterdayTimestamp();
                stop = getTodayTimestamp();
                break;
            case StatsActivity.D7:
                start= getXDaysTimestamp(7);
                break;
            case StatsActivity.D30:
                start= getXDaysTimestamp(30);
                break;
            case StatsActivity.D90:
                start= getXDaysTimestamp(90);
                break;
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int high = Integer.parseInt(settings.getString("highValue", "170"));
        int low = Integer.parseInt(settings.getString("lowValue", "70"));
        return new Select()
                .from(BgReading.class)
                .where("timestamp >= " + start)
                .where("timestamp <= " + stop)
                .where("calculated_value != 0")
                .orderBy("calculated_value").execute();
    }




    public static int noReadingsAboveRange(Context context) {
        long stop = System.currentTimeMillis();
        long start = System.currentTimeMillis();

        switch (StatsActivity.state){
            case StatsActivity.TODAY:
                start = getTodayTimestamp();
                break;
            case StatsActivity.YESTERDAY:
                start = getYesterdayTimestamp();
                stop = getTodayTimestamp();
                break;
            case StatsActivity.D7:
                start= getXDaysTimestamp(7);
                break;
            case StatsActivity.D30:
                start= getXDaysTimestamp(30);
                break;
            case StatsActivity.D90:
                start= getXDaysTimestamp(90);
                break;
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int high = Integer.parseInt(settings.getString("highValue", "170"));
        int low = Integer.parseInt(settings.getString("lowValue", "70"));
        int count =  new Select()
                .from(BgReading.class)
                .where("timestamp >= " + start)
                .where("timestamp <= " + stop)
                .where("calculated_value != 0")
                .where("calculated_value > " + high).count();
        Log.d("DrawStats", "High count: " + count);
        return count;
    }

    public static int noReadingsInRange(Context context) {
        long stop = System.currentTimeMillis();
        long start = System.currentTimeMillis();

        switch (StatsActivity.state){
            case StatsActivity.TODAY:
                start = getTodayTimestamp();
                break;
            case StatsActivity.YESTERDAY:
                start = getYesterdayTimestamp();
                stop = getTodayTimestamp();
                break;
            case StatsActivity.D7:
                start= getXDaysTimestamp(7);
                break;
            case StatsActivity.D30:
                start= getXDaysTimestamp(30);
                break;
            case StatsActivity.D90:
                start= getXDaysTimestamp(90);
                break;
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int high = Integer.parseInt(settings.getString("highValue", "170"));
        int low = Integer.parseInt(settings.getString("lowValue", "70"));
        int count =  new Select()
                .from(BgReading.class)
                .where("timestamp >= " + start)
                .where("timestamp <= " + stop)
                .where("calculated_value != 0")
                .where("calculated_value <= " + high)
                .where("calculated_value >= " + low)
                .count();
        Log.d("DrawStats", "In count: " + count);

        return count;
    }

    public static int noReadingsBelowRange(Context context) {
        long stop = System.currentTimeMillis();
        long start = System.currentTimeMillis();

        switch (StatsActivity.state){
            case StatsActivity.TODAY:
                start = getTodayTimestamp();
                break;
            case StatsActivity.YESTERDAY:
                start = getYesterdayTimestamp();
                stop = getTodayTimestamp();
                break;
            case StatsActivity.D7:
                start= getXDaysTimestamp(7);
                break;
            case StatsActivity.D30:
                start= getXDaysTimestamp(30);
                break;
            case StatsActivity.D90:
                start= getXDaysTimestamp(90);
                break;
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int high = Integer.parseInt(settings.getString("highValue", "170"));
        int low = Integer.parseInt(settings.getString("lowValue", "70"));
        int count = new Select()
                .from(BgReading.class)
                .where("timestamp >= " + start)
                .where("timestamp <= " + stop)
                .where("calculated_value != 0")
                .where("calculated_value < " + low)
                .count();
        Log.d("DrawStats", "Low count: " + count);

        return count;
    }




    public static List<BgReading> readingsBetweenTimestamps(long start, long stop) {
        return new Select()
                .from(BgReading.class)
                .where("timestamp >= " + start)
                .where("timestamp < " + stop)
                .where("calculated_value != 0")
                .orderBy("timestamp desc")
                .execute();
    }


    public static long getTodayTimestamp(){
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTimeInMillis();
    }

    public static long getYesterdayTimestamp(){
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.add(Calendar.DATE, -1);
        return date.getTimeInMillis();
    }

    public static long getXDaysTimestamp(int x){
        Calendar date = new GregorianCalendar();
        date.add(Calendar.DATE, -x);
        return date.getTimeInMillis();
    }


    public static List<BgReading> readingsToday() {
        // find beginning of today
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        long start = date.getTimeInMillis();

        List<BgReading> readings = readingsAfterTimestamp(start);
        Log.d("CreateStats", "found today: " + readings.size());

        return readings;
    }

    public static List<BgReading> lastXDays(int x) {
        // find beginning of today
        Calendar date = new GregorianCalendar();
        date.add(Calendar.DATE, -x);
        long start = date.getTimeInMillis();
        List<BgReading> readings = readingsAfterTimestamp(start);
        Log.d("CreateStats", "found last " + x + " days: " + readings.size());
        return readings;
    }


    public static List<BgReading> readingsYesterday() {
        // find beginning of today
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        long stop = date.getTimeInMillis();

        //yesterday:
        date.add(Calendar.DATE, -1);
        long start = date.getTimeInMillis();

        List<BgReading> readings = readingsBetweenTimestamps(start, stop);
        Log.d("CreateStats", "found yesteray: " + readings.size());

        return readings;
    }





}
