package com.eveningoutpost.dexdrip.watch.miband;

import com.eveningoutpost.dexdrip.Models.JoH;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import lombok.AllArgsConstructor;

public class FunAlmanac {

    private static final String TAG = "LeFun Almanac";

    public static Reply getRepresentation(double value, String arrowName) {

        android.util.Log.d(TAG, "Bg representation: " + value + " arrow: " + arrowName );

        final Calendar c = Calendar.getInstance();
        int currentDayOfWeek;
        boolean preserveDayOfWeek = true; // keep same or represent trend
        c.setTimeInMillis(JoH.tsl());
        if (preserveDayOfWeek) {
            switch(arrowName)
            {
                case "DoubleDown":
                    currentDayOfWeek = Calendar.MONDAY;
                    break;
                case "SingleDown":
                    currentDayOfWeek = Calendar.TUESDAY;
                    break;
                case "FortyFiveDown":
                    currentDayOfWeek = Calendar.WEDNESDAY;
                    break;
                case "Flat":
                    currentDayOfWeek = Calendar.THURSDAY;
                    break;
                case "FortyFiveUp":
                    currentDayOfWeek = Calendar.FRIDAY;
                    break;
                case "SingleUp":
                    currentDayOfWeek = Calendar.SATURDAY;
                    break;
                case "DoubleUp":
                    currentDayOfWeek = Calendar.SUNDAY;
                    break;
                default:
                    currentDayOfWeek = Calendar.THURSDAY;
            }
        }
        else currentDayOfWeek = c.get(Calendar.DAY_OF_WEEK);

        int macro, micro;

        macro = (int) value;
        micro = (int) (JoH.roundDouble(value - macro, 1) * 10);
        android.util.Log.d(TAG, "Result: " + macro + " " + micro);

        if (macro > 18 ) macro = 18; //limitation in the custom watchface can count up to 18
        if (micro == 0) micro = 10; //10 month will be displyed as 0 on the custom watchface

        c.set(Calendar.DAY_OF_MONTH, macro+1); //day 1 represent 0
        c.set(Calendar.MONTH, micro-1); // month starts at 0 in calendar

        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        while (dayOfWeek != currentDayOfWeek) {
            c.set(Calendar.YEAR, c.get(Calendar.YEAR) + 1);
            dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        }
        String time = new SimpleDateFormat("dd-M-yyyy EEEE hh:mm:ss").format(c.getTime());
        android.util.Log.d(TAG, "Time result: " + time);
        return new Reply(c.getTimeInMillis(), value);
    }

    private static int advanceDay(int day) {
        day++;
        if (day > Calendar.SATURDAY) {
            day = Calendar.SUNDAY;
        }
        return day;
    }

    @AllArgsConstructor
    public static class Reply {
        public long timestamp;
        public double input;
    }
}