package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Treatments;

import java.util.List;

public class InsulinPenManager {

    static final String PEN_ACTIVE      = "insulin_pen_active";
    static final String PEN_START_TIME  = "insulin_pen_start_time";
    static final String PEN_TOTAL_UNITS = "insulin_pen_total_units";
    static final String PEN_LOW_WARNED  = "insulin_pen_low_warned";

    public static final int    DEFAULT_UNITS  = 300;
    public static final double LOW_THRESHOLD  = 10.0;

    public static boolean isActive() {
        return Pref.getBooleanDefaultFalse(PEN_ACTIVE);
    }

    public static int getTotalUnits() {
        return Pref.getInt(PEN_TOTAL_UNITS, DEFAULT_UNITS);
    }

    public static long getStartTime() {
        return Pref.getLong(PEN_START_TIME, 0);
    }

    public static double getUnitsUsed() {
        final long startTime = getStartTime();
        if (startTime == 0) return 0;
        final List<Treatments> list = Treatments.latestForGraph(100000, startTime, JoH.tsl());
        double used = 0;
        if (list != null) {
            for (Treatments t : list) {
                used += t.insulin;
            }
        }
        return used;
    }

    public static double getRemainingUnits() {
        return Math.max(0, getTotalUnits() - getUnitsUsed());
    }

    public static void startNewPen(int totalUnits) {
        Pref.setBoolean(PEN_ACTIVE, true);
        Pref.setLong(PEN_START_TIME, JoH.tsl());
        Pref.setInt(PEN_TOTAL_UNITS, totalUnits);
        Pref.setBoolean(PEN_LOW_WARNED, false);
        Treatments.create_note("New Pen started (" + totalUnits + " IU)", JoH.tsl());
    }

    public static void endPen() {
        if (!isActive()) return;
        Treatments.create_note("Pen ended", JoH.tsl());
        Pref.setBoolean(PEN_ACTIVE, false);
    }

    /**
     * Call after every treatment approval. Returns a warning string to show
     * the user if the pen is low or empty, null otherwise.
     */
    public static String checkAfterTreatment() {
        if (!isActive()) return null;
        final double remaining = getRemainingUnits();
        if (remaining <= 0) {
            endPen();
            return "Pen empty \u2014 pen ended";
        }
        if (remaining < LOW_THRESHOLD && !Pref.getBooleanDefaultFalse(PEN_LOW_WARNED)) {
            Pref.setBoolean(PEN_LOW_WARNED, true);
            return String.format("Pen low: %.0f IU remaining", remaining);
        }
        return null;
    }

    /** Returns "Pen: 182 IU" when a pen is active, null otherwise. */
    public static String getStatusString() {
        if (!isActive()) return null;
        return String.format("Pen: %.0f IU", getRemainingUnits());
    }
}
