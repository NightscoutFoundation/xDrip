package com.eveningoutpost.dexdrip.cgm.glupro;

import android.util.Pair;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import java.util.List;

import lombok.Setter;

import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder.DEXCOM_PERIOD;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;

/**
 * JamOrHam
 * <p>
 * Glucose Profile backfill calculator
 */
public class GluProBackFill {

    private static final String TAG = GluProBackFill.class.getSimpleName();
    private static final long MAX_BACKFILL_PERIOD_MS = HOUR_IN_MS * 8; // how far back to request backfill data
    private static final int BACKFILL_CHECK_SMALL = 3;
    private static final int BACKFILL_CHECK_LARGE = (int) (MAX_BACKFILL_PERIOD_MS / DEXCOM_PERIOD);

    @Setter
    private static volatile int nextBackFillCheckSize = BACKFILL_CHECK_LARGE;

    public static Pair<Long, Long> check() {

        final int check_readings = nextBackFillCheckSize;
        UserError.Log.d(TAG, "Checking " + check_readings + " for backfill requirement");
        final List<BgReading> lastReadings = BgReading.latest_by_size(check_readings); // newest first
        boolean ask_for_backfill = false;
        long check_timestamp = JoH.tsl(); // TODO can this be merged with latest timestamp??
        long earliest_timestamp = JoH.tsl() - MAX_BACKFILL_PERIOD_MS;
        long latest_timestamp = JoH.tsl();
        if ((lastReadings == null) || (lastReadings.size() != check_readings)) {
            ask_for_backfill = true;
        } else {
            // find a gap larger than specified period
            for (int i = 0; i < lastReadings.size(); i++) {
                final BgReading reading = lastReadings.get(i);
                if ((reading == null) || (check_timestamp - reading.timestamp) > (DexCollectionType.getCurrentSamplePeriod() + (Constants.MINUTE_IN_MS))) {
                    ask_for_backfill = true;
                    if ((reading != null) && (msSince(reading.timestamp) <= MAX_BACKFILL_PERIOD_MS)) {
                        earliest_timestamp = reading.timestamp;
                    }
                    if (reading != null) {
                        UserError.Log.d(TAG, "Flagging backfill tripped by reading: " + i + " at time: " + JoH.dateTimeText(reading.timestamp) + " creating backfill window: " + JoH.dateTimeText(earliest_timestamp));
                    } else {
                        UserError.Log.d(TAG, "Flagging backfill tripped by null reading: " + i);
                    }
                    break;
                } else {
                    // good record
                    latest_timestamp = reading.timestamp;
                    check_timestamp = reading.timestamp;
                }
            }
        }

        if (ask_for_backfill) {
            nextBackFillCheckSize = BACKFILL_CHECK_LARGE;

            final long startTime = earliest_timestamp - (Constants.MINUTE_IN_MS * 5);
            final long endTime = latest_timestamp + (Constants.MINUTE_IN_MS * 5);
            UserError.Log.d(TAG, "Requesting backfill between: " + JoH.dateTimeText(startTime) + " " + JoH.dateTimeText(endTime));
            return new Pair<>(startTime, endTime);
        } else {
            nextBackFillCheckSize = BACKFILL_CHECK_SMALL;
            return null;
        }

    }

}
