package com.eveningoutpost.dexdrip.stats;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.activeandroid.Cache;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by adrian on 23/01/16.
 */
public class StatsResult {

    private final int in;
    private final int below;
    private final int above;
    private double total_carbs = -1;
    private double total_insulin = -1;
    private double stdev = -1;
    private int total_steps = -1;
    private final double avg;
    private final boolean mgdl;
    private final long from;
    private final long to;
    private long possibleCaptures;
    public static final String CUTOFF = "39";//from DBSearchUtil


    public StatsResult(SharedPreferences settings, boolean sliding24Hours) {
        this(settings, sliding24Hours, System.currentTimeMillis());
    }

    public static long getTodayTimestamp() {

        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTimeInMillis();
    }

    public StatsResult(SharedPreferences settings, boolean sliding24Hours, long to) {
        this(settings, sliding24Hours?to-(24*60*60*1000):getTodayTimestamp(), to);
    }


    public StatsResult(SharedPreferences settings, long from, long to){
        this.from = from;
        this.to = to;

        mgdl = "mgdl".equals(settings.getString("units", "mgdl"));

        double high = Double.parseDouble(settings.getString("highValue", "170"));
        double low = Double.parseDouble(settings.getString("lowValue", "70"));
        if (!mgdl) {
            high *= Constants.MMOLL_TO_MGDL;
            low *= Constants.MMOLL_TO_MGDL;
        }
        SQLiteDatabase db = Cache.openDatabase();

        Cursor cursor= db.rawQuery("select count(*) from bgreadings  where timestamp >= " + from + " AND timestamp <= " + to + " AND calculated_value >= " + low + " AND calculated_value <= " + high + " AND snyced == 0", null);
        cursor.moveToFirst();
        in = cursor.getInt(0);
        cursor.close();

        cursor= db.rawQuery("select count(*) from bgreadings  where timestamp >= " + from + " AND timestamp <= " + to + " AND calculated_value > " + CUTOFF + " AND calculated_value < " + low + " AND snyced == 0", null);
        cursor.moveToFirst();
        below = cursor.getInt(0);
        cursor.close();

        cursor= db.rawQuery("select count(*) from bgreadings  where timestamp >= " + from + " AND timestamp <= " + to + " AND calculated_value > " + high + " AND snyced == 0", null);
        cursor.moveToFirst();
        above = cursor.getInt(0);
        cursor.close();

        if(getTotalReadings() > 0){
            cursor= db.rawQuery("select avg(calculated_value) from bgreadings  where timestamp >= " + from + " AND timestamp <= " + to + " AND calculated_value > " + CUTOFF + " AND snyced == 0", null);
            cursor.moveToFirst();
            avg = cursor.getDouble(0);
            cursor.close();
        } else {
            avg = 0;
        }

        possibleCaptures = (to - from) / (5*60*1000);
        //while already in the next 5 minutes, a package could already have arrived.
        if ((to - from) % (5*60*1000) != 0) possibleCaptures += 1;

    }



    public int getAbove() {
        return above;
    }

    public double getAvg() {
        return avg;
    }

    public int getBelow() {
        return below;
    }

    public int getIn() {
        return in;
    }

    public double getTotal_carbs() {
        if (total_carbs < 0) {
            Cursor cursor = Cache.openDatabase().rawQuery("select sum(carbs) from treatments  where timestamp >= " + from + " AND timestamp <= " + to, null);
            cursor.moveToFirst();
            total_carbs = cursor.getInt(0);
            cursor.close();
        }
        return total_carbs;
    }

    public void calc_StdDev() {
        if (stdev < 0) {
            if(getTotalReadings() > 0){
                Cursor cursor= Cache.openDatabase().rawQuery("select ((count(*)*(sum(calculated_value * calculated_value)) - (sum(calculated_value)*sum(calculated_value)) )/((count(*)-1)*(count(*))) ) from bgreadings  where timestamp >= " + from + " AND timestamp <= " + to + " AND calculated_value > " + CUTOFF + " AND snyced == 0", null);
                cursor.moveToFirst();
                stdev = cursor.getDouble(0);
                stdev = Math.sqrt(stdev);
                cursor.close();
            } else {
                stdev = 0;
            }
        }
    }

    public double getRatio() {
        return getTotal_carbs() / getTotal_insulin();
        //return -1;
    }

    public double getTotal_insulin() {
        if (total_insulin < 0) {
            Cursor cursor = Cache.openDatabase().rawQuery("select sum(insulin) from treatments  where timestamp >= " + from + " AND timestamp <= " + to, null);
            cursor.moveToFirst();
            total_insulin = cursor.getDouble(0);
            cursor.close();
        }
        return total_insulin;
    }

    public int getTotal_steps() {
        if (total_steps < 0) {
            Cursor cursor = Cache.openDatabase().rawQuery("select sum(t.metric)\n" +
                    "from PebbleMovement t\n" +
                    "inner join (\n" +
                    "    select metric, max(timestamp) as MaxDate\n" +
                    "    from PebbleMovement\n" +
                    "    group by date(timestamp/1000,'unixepoch','localtime') \n" +
                    ") tm on t.metric = tm.metric and t.timestamp = tm.MaxDate  where timestamp >= " + from + " AND timestamp <= " + to, null);
            cursor.moveToFirst();
            total_steps = cursor.getInt(0);
            cursor.close();
        }
        return total_steps;
    }

    public int getTotalReadings(){
        return in + above + below;
    }

    public long getPossibleCaptures() {return possibleCaptures;}

    public String getInPercentage(){
        return "in:" +  ((getTotalReadings()>0)? (100 - getHighPercentageInt() - getLowPercentageInt()) + "%":"-%");
    }

    public int getLowPercentageInt() { return (int) (below * 100.0 / getTotalReadings() + 0.5);}

    public String getLowPercentage(){
        return "lo:" +  ((getTotalReadings()>0)? getLowPercentageInt() + "%":"-%");
    }

    public int getHighPercentageInt() { return (int) (above * 100.0 / getTotalReadings() + 0.5);}

    public String getHighPercentage(){
        return "hi:" +  ((getTotalReadings()>0)? getHighPercentageInt() + "%":"-%");
    }

    public String getA1cDCCT(){
        if(getTotalReadings()==0) return "A1c:?%";
        return "A1c:" + (Math.round(10 * (avg + 46.7) / 28.7) / 10d) + "%";
    }

    public String getA1cIFCC() {
        return getA1cIFCC(false);
    }

        public String getA1cIFCC(boolean shortVersion){
        if(getTotalReadings()==0) return "A1c:?";
        return (shortVersion?"":"A1c:") + ((int) Math.round(((avg + 46.7) / 28.7 - 2.15) * 10.929));
    }

    public String getAverageUnitised(){
        if(getTotalReadings()==0) return "Avg:?";
        if(mgdl) return "Avg:" + Math.round(avg);
        return "Avg:" + (new DecimalFormat("#.0")).format(avg*Constants.MGDL_TO_MMOLL);
    }

    public String getStdevUnitised(){
        calc_StdDev();
        if(getTotalReadings()==0) return "sd:?";
        if(mgdl) return "sd:" + (Math.round(stdev * 10) / 10d);
        return "sd:" + (new DecimalFormat("#.0")).format((Math.round(stdev * Constants.MGDL_TO_MMOLL * 100) / 100d));
    }

    public int getCapturePercentage() {
        return (int) Math.round(getTotalReadings() * 100d / possibleCaptures);
    }

    public String getCapturePercentage(boolean extended) {
        String result = "Cap:" + ((possibleCaptures > 0) ? Math.round(getTotalReadings() * 100d / possibleCaptures) + "%" : "-%");

        if (extended) {
            result += " (" + getTotalReadings() + "/" + getPossibleCaptures() + ")";
        }

        return result;
    }

}
