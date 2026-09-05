package com.eveningoutpost.dexdrip.cgm.dex;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

// Scheduling logic for the optional G7 "soak & handoff" transmitter switch.
//
// The pure helpers (isDue / clampDelay / switchTimeFor) take their inputs as
// parameters - including the current time - so they can be unit tested without
// Android. The Pref-backed methods own the three preferences that the settings
// summary, the "New Sensor Ready" notification and the actual switch all read,
// so those three call sites stay in agreement about when a changeover is due.
public final class SoakSchedule {

    public static final String NEXT_ID = "dex_txid_next";
    public static final String NEXT_TIME = "dex_txid_next_time";
    public static final String DELAY = "dex_txid_delay";

    // Fire slightly early so a switch isn't missed in the gap between reading cycles.
    public static final long GRACE_MS = 60000L;

    public static final int DEFAULT_DELAY_MINUTES = 30;

    // Returned by clampDelay when the delay cannot fit inside the remaining sensor life.
    public static final int CANNOT_SCHEDULE = -1;

    private SoakSchedule() {
    }

    // ---- pure helpers (no Android, no Pref, time injected) ----------------

    public static boolean isDue(final String nextId, final long switchTime, final long now) {
        return nextId != null && !nextId.isEmpty() && switchTime > 0 && now >= (switchTime - GRACE_MS);
    }

    public static long switchTimeFor(final long now, final int delayMinutes) {
        return now + delayMinutes * 60000L;
    }

    // Clamp a requested delay to [1 .. remaining sensor life].
    // remainingSensorMs <= 0 means "unknown / no cap".
    // Returns CANNOT_SCHEDULE when there is less than a minute of sensor life left,
    // so a switch is refused rather than being fired immediately.
    public static int clampDelay(int minutes, final long remainingSensorMs) {
        if (minutes < 1) minutes = 1;
        if (remainingSensorMs > 0) {
            final int maxMinutes = (int) (remainingSensorMs / 60000L);
            if (maxMinutes < 1) return CANNOT_SCHEDULE;
            if (minutes > maxMinutes) minutes = maxMinutes;
        }
        return minutes;
    }

    // ---- Pref-backed state, shared by all call sites ----------------------

    public static String pendingId() {
        return Pref.getStringDefaultBlank(NEXT_ID);
    }

    public static long switchTime() {
        return Pref.getLong(NEXT_TIME, 0);
    }

    // Last delay the user chose, or 0 if none stored (used to show the summary).
    public static int storedDelayMinutes() {
        return JoH.tolerantParseInt(Pref.getString(DELAY, ""), 0);
    }

    // Delay to pre-fill the dialog with: the last one used, else the default.
    public static int delayMinutesOrDefault() {
        return JoH.tolerantParseInt(Pref.getString(DELAY, ""), DEFAULT_DELAY_MINUTES);
    }

    public static boolean isPending() {
        return !pendingId().isEmpty();
    }

    public static boolean isDue(final long now) {
        return isDue(pendingId(), switchTime(), now);
    }

    // Store a fully validated schedule. Returns the computed switch time.
    public static long queue(final String id, final int delayMinutes, final long now) {
        final long switchTime = switchTimeFor(now, delayMinutes);
        Pref.setString(NEXT_ID, id);
        Pref.setString(DELAY, String.valueOf(delayMinutes));
        Pref.setLong(NEXT_TIME, switchTime);
        return switchTime;
    }

    // Turn off a pending switch but keep the last delay as the remembered default.
    // Used after a successful switch and when the active id is changed manually.
    public static void deactivate() {
        Pref.setString(NEXT_ID, "");
        Pref.setLong(NEXT_TIME, 0);
    }

    // Full reset, including the remembered delay. Used when the user explicitly
    // blanks the "New Transmitter ID" field to cancel.
    public static void clearAll() {
        deactivate();
        Pref.setString(DELAY, "");
    }
}
