package com.eveningoutpost.dexdrip.cgm.nsfollow;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Unitized;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.Entry;

import java.util.List;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.Models.BgReading.SPECIAL_FOLLOWER_PLACEHOLDER;

/**
 * jamorham
 *
 * Take a list of Nightscout entries and inject as BgReadings
 */

public class EntryProcessor {

    private static final String TAG = "NightscoutFollowEP";

    static synchronized void processEntries(final List<Entry> entries, final boolean live) {

        if (entries == null) return;

        final Sensor sensor = Sensor.createDefaultIfMissing();

        for (final Entry entry : entries) {
            if (entry != null) {
                UserError.Log.d(TAG, "ENTRY: " + entry.toS());
                UserError.Log.d(TAG, "Glucose value: " + Unitized.unitized_string_static(entry.sgv));

                final long recordTimestamp = entry.getTimeStamp();
                if (recordTimestamp > 0) {
                    final BgReading existing = BgReading.getForPreciseTimestamp(recordTimestamp, 10000);
                    if (existing == null) {
                        UserError.Log.d(TAG, "NEW NEW NEW New entry: " + entry.toS());

                        if (live) {
                            final BgReading bg = new BgReading();
                            bg.uuid = UUID.randomUUID().toString();
                            bg.timestamp = recordTimestamp;
                            bg.calculated_value = entry.sgv;
                            bg.raw_data = entry.unfiltered != 0 ? entry.unfiltered : SPECIAL_FOLLOWER_PLACEHOLDER;
                            bg.filtered_data = entry.filtered;
                            bg.noise = entry.noise + "";
                            // TODO need to handle slope??
                            bg.sensor = sensor;
                            bg.sensor_uuid = sensor.uuid;
                            bg.source_info = "Nightscout Follow";
                            bg.save();
                            Inevitable.task("entry-proc-post-pr",500, () -> bg.postProcess(false));
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
