package com.eveningoutpost.dexdrip.watch.miband;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Unitized;

import java.util.Calendar;

import lombok.AllArgsConstructor;

import static com.eveningoutpost.dexdrip.Models.JoH.roundDouble;
import static com.eveningoutpost.dexdrip.UtilityModels.Unitized.mmolConvert;

public class FunAlmanac {

    private static final String TAG = "miband almanac";

    public static Reply getRepresentation(double bgValue, String arrowName, boolean usingMgDl) {
        final Calendar c = Calendar.getInstance();
        int currentDayOfWeek;
        boolean preserveDayOfWeek = true; // keep same or represent trend
        c.setTimeInMillis(JoH.tsl());
        if (preserveDayOfWeek) {
            switch (arrowName) {
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
        } else currentDayOfWeek = c.get(Calendar.DAY_OF_WEEK);

        int macro = 0, micro = 0;
        double value = bgValue;
        if (usingMgDl) {
            if (value > 299) value = 299;
            else if (value < 10) value = 10;
            macro = (int) value / 10;
            micro = (int) value % 10;
        } else {
            value = roundDouble(mmolConvert(value), 1);
            if (value >= 18.9) value = 18.9;
            macro = (int) value;
            micro = (int) (JoH.roundDouble(value - macro, 1) * 10);
            macro++;
        }
        if (micro == 0) micro = 10; //10th month will be displayed as 0 on the custom watchface
        micro--;
        c.set(Calendar.DAY_OF_MONTH, macro); //day 1 represent 0
        c.set(Calendar.MONTH, micro);
        int max = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        while ((dayOfWeek != currentDayOfWeek) || ((max < 29))) {
            c.set(Calendar.YEAR, c.get(Calendar.YEAR) + 1);
            c.set(Calendar.DAY_OF_MONTH, macro);
            c.set(Calendar.MONTH, micro);
            max = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        }
        String textVal = Double.toString(value) + " " + Unitized.unit(usingMgDl) + ", " + arrowName;
        return new Reply(c.getTimeInMillis(), textVal);
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
        public String input;
    }
}