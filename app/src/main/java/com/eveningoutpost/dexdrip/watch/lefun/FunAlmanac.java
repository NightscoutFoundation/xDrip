package com.eveningoutpost.dexdrip.watch.lefun;

// jamorham

import com.eveningoutpost.dexdrip.models.JoH;

import java.util.Calendar;

import lombok.AllArgsConstructor;

public class FunAlmanac {

    private static final String TAG = "LeFun Almanac";

    public static Reply getRepresentation(double value) {

        android.util.Log.d(TAG, "called with: " + value);

        final Calendar c = Calendar.getInstance();

        boolean allowZeroDayHack = true;
        boolean preserveDayOfWeek = true; // keep same or represent trend

        c.setTimeInMillis(JoH.tsl());

        int currentDayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        boolean zeroDay = false;
        boolean zeroMonth = false;

        int macro, micro;

        if (value <= 13) {

            // java's annoying imprecision with double type means that statements like this
            // ((int) ((10.1 - 10) * 10))) would be expected to yield '1' but instead yield '0'
            // So we get BigDecimal involved for such a trivial calculation...

            macro = (int) value;
            micro = (int) (JoH.roundDouble(value - macro, 1) * 10);

            android.util.Log.d(TAG, "Result: " + macro + " " + micro);

            if (micro == 0) {
                micro = 1;
                if (allowZeroDayHack) {
                    currentDayOfWeek = advanceDay(currentDayOfWeek); // offset so we can backtrack
                    zeroDay = true;

                    if (macro == 0) {
                        zeroMonth = true;

                        for (int i = 0; i < 3; i++) {
                            currentDayOfWeek = advanceDay(currentDayOfWeek); // end of year backtrack
                        }
                    }
                }
            }


        } else {
            macro = 1;
            micro = (int) value;
            if (allowZeroDayHack) {
                zeroMonth = true;
                for (int i = 0; i < 5; i++) {
                    currentDayOfWeek = advanceDay(currentDayOfWeek); // end of year backtrack
                }
            }
        }

        c.set(Calendar.MONTH, macro - 1); // month starts at 0 in calendar
        c.set(Calendar.DAY_OF_MONTH, micro);

        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        while (dayOfWeek != currentDayOfWeek) {
            c.set(Calendar.YEAR, c.get(Calendar.YEAR) + 1);
            dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        }

        return new Reply(c.getTimeInMillis(), zeroMonth, zeroDay, value);
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
        public boolean zeroMonth;
        public boolean zeroDay;
        public double input;
    }
}



