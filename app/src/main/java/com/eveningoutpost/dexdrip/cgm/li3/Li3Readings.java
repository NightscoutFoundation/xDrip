package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.models.JoH.dateTimeText;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import xrwd.li3.Li3SensorState;
import xrwd.li3.data.HistoricalPage;
import xrwd.li3.data.PatchStatus;
import xrwd.li3.data.RealtimeReading;
import xrwd.li3.util.Hex;

/**
 * Turns the Li3's realtime and historic values into xDrip readings. A reading's time is the sensor start plus
 * its life count in minutes. Realtime values are the sensor's own mg/dL, stored every minute with
 * {@code libre_one_minute} on and every five minutes otherwise, and smoothed only when the user asks. History fills
 * gaps, the minutes ahead of the last realtime reading included, and never replaces a reading already stored.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class Li3Readings {

    private static final String TAG = "Li3";
    private static final String SOURCE = "Li3";

    // LO and HI arrive as 39 and 501, which xDrip shows as LOW and HIGH
    private static final int MIN_MGDL = 39;
    private static final int MAX_MGDL = 501;
    private static final long KEEP_RECENT_MS = 60 * MINUTE_IN_MS;
    /** A realtime reading is the sensor's current minute; this allows for the start time being off either way. */
    private static final long MAX_CLOCK_OFFSET_MS = 15 * MINUTE_IN_MS;
    /** The refusal of a reading the sensor's own data quality or sensor condition flags mark, nothing else. */
    private static final String REFUSED_QUALITY = "not displayable";

    private final Li3SensorState state;
    /** Recent accepted realtime values as {timestamp, mg/dL}, oldest first, for smoothing. */
    private final ArrayDeque<long[]> recent = new ArrayDeque<>();

    private PatchStatus lastStatus;
    /** Why the last realtime reading was refused, or null when it was accepted. */
    private String lastRefusal;
    private String lastFastData;
    private int backfilled;

    public Li3Readings(Li3SensorState state) {
        this.state = state;
    }

    /**
     * Stores a realtime reading when it is valid, new and on the chosen interval. A reading refused for its value or
     * its time has its life count remembered, so history does not store that minute later; one refused only for its
     * quality flags does not, and history can still bring that minute back.
     *
     * @return true when a reading was stored
     */
    public synchronized boolean onRealtime(RealtimeReading reading, long nowMs) {
        int lifeCount = reading.lifeCount();
        lastFastData = Hex.of(reading.fastData());
        UserError.Log.d(TAG, "Realtime life count " + lifeCount + " fastData " + lastFastData);
        Integer last = state.lastLifeCount();
        if (last != null && lifeCount <= last) {
            UserError.Log.d(TAG, "Li3 reading at life count " + lifeCount + " not after " + last);
            return false;
        }
        String reason = refusal(lifeCount, reading.mgdl(), nowMs);
        if (reason == null && timestamp(lifeCount) < nowMs - MAX_CLOCK_OFFSET_MS) {
            reason = "time is more than 15 minutes ago, start time looks wrong";
        }
        // only a refusal for the quality flags alone leaves the minute to history; a wrong value or time would be
        // stored by history the same way, so those are remembered
        boolean historyCanFill = reason == null;
        if (reason == null && !reading.displayable()) {
            reason = REFUSED_QUALITY;
        }
        if (reason != null) {
            if (!historyCanFill) {
                state.addRejectedLifeCount(lifeCount);
            }
            Li3Store.updateState(state);
            String message = "Li3 reading at life count " + lifeCount + " not stored: " + reason;
            if (reason.equals(lastRefusal)) {
                UserError.Log.d(TAG, message); // the event log already has this reason, from the first in this run
            } else {
                UserError.Log.uel(TAG, message);
            }
            lastRefusal = reason;
            return false;
        }
        lastRefusal = null;
        int mgdl = reading.mgdl();
        long timestamp = storedTimestamp(lifeCount, nowMs);
        state.updateLastGlucose(lifeCount, mgdl);
        if (!Li3Store.updateState(state)) {
            UserError.Log.e(TAG, "Li3 " + state.serial() + " is no longer the stored sensor, reading not stored");
            return false;
        }
        double value = Pref.getBooleanDefaultFalse(Li3Store.PREF_SMOOTHING) ? smoothed(timestamp, mgdl) : mgdl;
        remember(timestamp, mgdl);
        if (!Pref.getBooleanDefaultFalse("libre_one_minute") && lifeCount % 5 != 0) {
            UserError.Log.d(TAG, "Life count " + lifeCount + " not stored: not on the 5-minute interval");
            return false;
        }
        boolean isNew = BgReading.getForPreciseTimestamp(timestamp, MINUTE_IN_MS) == null;
        BgReading bgr = BgReading.bgReadingInsertFromGluPro(value, timestamp, SOURCE);
        if (bgr == null) {
            UserError.Log.d(TAG, "Life count " + lifeCount + " not stored: xDrip refused the reading");
            return false;
        }
        if (isNew) {
            bgr.find_slope(); // the shared insert sets no slope, so the trend arrow would stay flat
        }
        UserError.Log.d(TAG, "Life count " + lifeCount + " stored: " + JoH.qs(value, 0) + " mg/dL");
        return true;
    }

    /**
     * Stores the history entries that fill a gap: valid, not refused before for a reason history cannot improve, and
     * with no reading within a minute of their time. Entries ahead of the last realtime reading are stored too: those
     * minutes are exactly the ones no realtime reading was stored for, and history is the only source that has them.
     * Their time is checked against the clock only for being ahead, as a realtime reading's is, so a wrong start
     * time cannot put a page far into the future; unlike realtime, a time far in the past is not refused.
     *
     * @return how many were stored
     */
    public synchronized int onHistory(List<HistoricalPage.Entry> entries, long nowMs) {
        if (!Li3Store.isStored(state)) {
            UserError.Log.e(TAG, "Li3 " + state.serial() + " is no longer the stored sensor, history not stored");
            return 0;
        }
        int stored = 0;
        for (HistoricalPage.Entry entry : entries) {
            String reason = state.rejectedLifeCounts().contains(entry.lifeCount) ? "refused before"
                    : refusal(entry.lifeCount, entry.mgdl, nowMs);
            if (reason != null) {
                UserError.Log.d(TAG, "History at life count " + entry.lifeCount + " skipped: " + reason);
                continue;
            }
            long timestamp = storedTimestamp(entry.lifeCount, nowMs);
            // any sensor's reading counts, so a source switch does not store a minute twice
            if (BgReading.getForPreciseTimestamp(timestamp, MINUTE_IN_MS, false) != null) {
                continue;
            }
            if (BgReading.bgReadingInsertFromGluPro(entry.mgdl, timestamp, SOURCE) != null) {
                stored++;
            }
        }
        if (!entries.isEmpty()) {
            UserError.Log.d(TAG, "History: " + entries.size() + " entries, life counts " + entries.get(0).lifeCount
                    + " to " + entries.get(entries.size() - 1).lifeCount + ", " + stored + " stored");
            Li3Store.updateState(state);
        }
        if (stored > 0) {
            backfilled += stored;
            UserError.Log.uel(TAG, "Li3 backfilled " + stored + " readings");
        }
        return stored;
    }

    /** Keeps the sensor's latest status for the status rows, and saves the state the connection may have changed. */
    public synchronized void onStatus(PatchStatus status) {
        lastStatus = status;
        Li3Store.updateState(state); // the connection may have asked for or given up on history on this frame
    }

    /** @return why the last realtime reading was refused, or null when it was accepted */
    public synchronized String lastRefusal() {
        return lastRefusal;
    }

    /** Status rows for this sensor. Never shows the PIN or kAuth. */
    public synchronized List<StatusItem> megaStatus() {
        List<StatusItem> si = new ArrayList<>();
        si.add(new StatusItem(gs(R.string.li3_sensor), state.serial()));
        Integer wear = state.wearMinutes();
        if (wear != null && wear > 0) {
            // The longer sensor is worn 15 days, the shorter 14; the generation field is 1 on a US 15-day sensor too
            si.add(new StatusItem(gs(R.string.li3_product), wear > 14 * 24 * 60 ? "Li3, 15 days" : "Li3, 14 days"));
        }
        if (lastStatus != null) {
            PatchStatus.Attention attention = lastStatus.attention();
            si.add(new StatusItem(gs(R.string.li3_sensor_state), lastStatus.state()
                    + (attention == PatchStatus.Attention.NONE ? "" : " (" + attention + ")"),
                    attention == PatchStatus.Attention.NONE ? StatusItem.Highlight.NORMAL : StatusItem.Highlight.BAD));
        }
        Integer last = state.lastLifeCount();
        if (last != null) {
            si.add(new StatusItem(gs(R.string.li3_last_life_count), last + " (" + dateTimeText(timestamp(last))
                    + ", " + state.lastMgdl() + " mg/dL)"));
        }
        si.add(new StatusItem(gs(R.string.li3_backfilled), Integer.toString(backfilled)));
        if (lastFastData != null) {
            si.add(new StatusItem(gs(R.string.li3_fast_data), lastFastData));
        }
        return si;
    }

    /** Why a value at this life count is not stored, or null when it can be. */
    private String refusal(int lifeCount, Integer mgdl, long nowMs) {
        if (mgdl == null || mgdl < MIN_MGDL || mgdl > MAX_MGDL) {
            return "value " + mgdl + " outside " + MIN_MGDL + "-" + MAX_MGDL;
        }
        if (!state.lifecycle().usable(lifeCount)) {
            return state.lifecycle().phase(lifeCount).name().toLowerCase(Locale.ROOT);
        }
        if (state.activationMs() <= 0) {
            return "start time unknown";
        }
        if (timestamp(lifeCount) > nowMs + MAX_CLOCK_OFFSET_MS) {
            return "time is more than 15 minutes ahead, start time looks wrong";
        }
        return null;
    }

    private long timestamp(int lifeCount) {
        return state.activationMs() + lifeCount * MINUTE_IN_MS;
    }

    /** The time a value is stored at: never after now, so a start time a little ahead does not show a future value. */
    private long storedTimestamp(int lifeCount, long nowMs) {
        return Math.min(timestamp(lifeCount), nowMs);
    }

    private void remember(long timestamp, int mgdl) {
        recent.addLast(new long[]{timestamp, mgdl});
        Iterator<long[]> it = recent.iterator();
        while (it.hasNext() && it.next()[0] < timestamp - KEEP_RECENT_MS) {
            it.remove();
        }
    }

    /** LibreReceiver's weighted average: over {@code libre_filter_length} minutes, 20 when it is the default 25. */
    private double smoothed(long timestamp, int mgdl) {
        long minutes = Math.max(1, Pref.getStringToInt("libre_filter_length", 25));
        long window = (minutes == 25 ? 20 : minutes) * MINUTE_IN_MS;
        double duration = minutes * MINUTE_IN_MS;
        double sum = mgdl;
        double weights = 1;
        for (long[] value : recent) {
            if (value[0] >= timestamp - window && value[0] < timestamp) {
                double weight = 1 - (timestamp - value[0]) / duration;
                sum += value[1] * weight;
                weights += weight;
            }
        }
        return Math.round(sum / weights);
    }
}
