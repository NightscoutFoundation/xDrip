package com.eveningoutpost.dexdrip.utils.time;

// jamorham

import android.annotation.SuppressLint;
import android.text.format.DateFormat;
import android.util.Pair;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.eveningoutpost.dexdrip.models.JoH.tolerantParseInt;

public class TimeRangeUtils {

    public static boolean areWeInTimePeriod(final String prefix) {
        final Pair<Integer, Integer> pair = getStartStopFromPref(prefix);
        return withinStartStopSeconds(pair.first, pair.second, secondsSinceMidnight());
    }

    public static int secondsSinceMidnight() {
        final Calendar calendar = Calendar.getInstance();
        return (calendar.get(Calendar.HOUR_OF_DAY) * 3600) + (calendar.get(Calendar.MINUTE) * 60);
    }

    public static boolean withinStartStopSeconds(final int start, final int stop, final int now) {
        if (start == stop) return true; // always on
        if (start < stop) {
            // included middle
            return now >= start && now <= stop;
        } else {
            // excluded middle wrapping midnight
            return now <= start || now >= stop;
        }
    }

    public static Pair<Integer, Integer> getStartStopFromPref(final String prefix) {
        return new Pair<>(
                tolerantParseInt(Pref.getString(prefix + "_start_time", "0"), 0),
                tolerantParseInt(Pref.getString(prefix + "_stop_time", "0"), 0));
    }

    public static String getNiceStartStopString(final String prefix) {
        final Pair<Integer, Integer> pair = getStartStopFromPref(prefix);
        if (pair.first.equals(pair.second)) {
            return "All day";
        } else {
            return String.format("%s to %s", niceTimeOfDay(pair.first), niceTimeOfDay(pair.second));
        }
    }

    public static String niceTimeOfDay(final int ssm) {
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat sdf = new SimpleDateFormat(DateFormat.is24HourFormat(xdrip.getAppContext()) ? "HH:mm" : "h:mm a");
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(JoH.tsl());
        calendar.set(Calendar.HOUR_OF_DAY, ssm / 3600);
        calendar.set(Calendar.MINUTE, (ssm % 3600) / 60);
        return sdf.format(calendar.getTimeInMillis());
    }

}
