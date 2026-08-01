package com.eveningoutpost.dexdrip.cgm.medtrumfollow;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.models.BgReading.SPECIAL_FOLLOWER_PLACEHOLDER;

final class EntryProcessor {

    private static final String TAG = "MedtrumFollowEP";

    private EntryProcessor() {
    }

    static synchronized void processEntries(final List<MedtrumFollowData.Entry> entries) {
        if (entries == null || entries.isEmpty()) return;

        final Sensor sensor = Sensor.createDefaultIfMissing();
        Collections.sort(entries, (left, right) -> Long.compare(left.timestamp, right.timestamp));

        for (final MedtrumFollowData.Entry entry : entries) {
            if (entry == null || entry.timestamp <= 0 || entry.glucose <= 0) continue;
            if (BgReading.getForPreciseTimestamp(entry.timestamp, 10_000) != null) continue;

            UserError.Log.d(TAG, "New EasyView entry at " + entry.timestamp + ": " + entry.glucose);
            final BgReading bg = new BgReading();
            bg.uuid = UUID.randomUUID().toString();
            bg.timestamp = entry.timestamp;
            bg.calculated_value = entry.glucose;
            bg.raw_data = SPECIAL_FOLLOWER_PLACEHOLDER;
            bg.filtered_data = entry.glucose;
            if (entry.trendName != null) {
                bg.calculated_value_slope = BgReading.slopefromName(entry.trendName);
            } else {
                bg.hide_slope = true;
            }
            bg.sensor = sensor;
            bg.sensor_uuid = sensor.uuid;
            bg.source_info = "Medtrum Follow";
            bg.save();
            Inevitable.task("medtrum-follow-post-process", 500, () -> bg.postProcess(false));
        }
    }
}

