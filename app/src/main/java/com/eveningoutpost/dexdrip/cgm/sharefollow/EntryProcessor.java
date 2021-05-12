package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;

import java.util.Collections;
import java.util.List;

import static com.eveningoutpost.dexdrip.Models.BgReading.SPECIAL_FOLLOWER_PLACEHOLDER;

/**
 * jamorham
 *
 * Take a list of Share entries and inject as BgReadings
 */

public class EntryProcessor {

    private static final String TAG = "ShareFollowEP";
    private static final boolean D = false;

    static synchronized void processEntries(final List<ShareGlucoseRecord> entries, final boolean live) {

        if (entries == null) return;

        final Sensor sensor = Sensor.createDefaultIfMissing();

        // place in order of oldest first
        Collections.sort(entries, (o1, o2) -> o1.getTimestamp().compareTo(o2.getTimestamp()));

        for (final ShareGlucoseRecord entry : entries) {
            if (entry != null) {
                if (D) UserError.Log.d(TAG, "ENTRY: " + entry.toS());

                final long recordTimestamp = entry.getTimestamp();
                if (recordTimestamp > 0) {
                    final BgReading existing = BgReading.getForPreciseTimestamp(recordTimestamp, 10_000);
                    if (existing == null) {
                        UserError.Log.d(TAG, "NEW NEW NEW New entry: " + entry.toS());

                        if (live) {
                            final BgReading bg = new BgReading();
                            bg.timestamp = recordTimestamp;
                            bg.calculated_value = entry.Value;
                            bg.raw_data = SPECIAL_FOLLOWER_PLACEHOLDER;
                            bg.filtered_data = entry.Value;

                            final Double slope = entry.slopePerMsFromDirection();
                            if (slope != null) {
                                bg.calculated_value_slope = slope; // this is made up but should match arrow
                            } else {
                                bg.hide_slope = true;
                            }

                            bg.sensor = sensor;
                            bg.sensor_uuid = sensor.uuid;
                            bg.source_info = "Share Follow";
                            bg.save();
                            Inevitable.task("entry-proc-post-pr", 500, () -> bg.postProcess(false));
                        }
                    } else {
                        // break; // stop if we have this reading TODO are entries always in order?
                    }
                } else {
                    UserError.Log.e(TAG, "Could not parse a timestamp from: " + entry.toS());
                }

            } else {
                UserError.Log.d(TAG, "Entry is null");
            }
        }
    }
}
