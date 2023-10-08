package com.eveningoutpost.dexdrip.profileeditor;

import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.roundDouble;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.profileeditor.BasalProfile.getActiveRateName;

import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import java.util.ArrayList;
import java.util.List;

import lombok.val;

public class BasalRepository {

    private static final int WHOLE_DAY_IN_MINUTES = 60 * 24;
    static long lastUpdated = 0;
    static final List<Float> rates = new ArrayList<>();


    static void populateRateAsNeeded() {
        if (rates.size() == 0 || msSince(lastUpdated) > Constants.HOUR_IN_MS) {
            synchronized (rates) {
                rates.clear();
                try {
                    rates.addAll(BasalProfile.load(getActiveRateName()));
                } catch (NullPointerException e) {
                    //
                }
                lastUpdated = tsl();
                if (rates.size() == 0) {
                    // failed to load
                    rates.add(0f);
                }
            }
        }
    }

    public static void clearRates() {
        rates.clear();
        lastUpdated = 0;
    }

    public static void dummyRatesForTesting() {
        clearRates();
        rates.add(2f);
        lastUpdated = tsl();
    }

    static int minutesPerSegment() {
        val size = rates.size();
        if (size == 0) return WHOLE_DAY_IN_MINUTES;
        return WHOLE_DAY_IN_MINUTES / size;
    }

    static double getRateByMinuteOfDay(final int minute) {
        if (rates.size() == 0
                || minute < 0
                || minute >= WHOLE_DAY_IN_MINUTES) {
            return 0d;
        }
        return roundDouble((double) rates.get(minute / minutesPerSegment()), 2);
    }

    static double getRateByTimeStamp(final long when) {
        return getRateByMinuteOfDay(ProfileItem.timeStampToMin(when));
    }

    public static double getActiveRate(final long when) {
        populateRateAsNeeded();
        return getRateByTimeStamp(when);
    }
}
