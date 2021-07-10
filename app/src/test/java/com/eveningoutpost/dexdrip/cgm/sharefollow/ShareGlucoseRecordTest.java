package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.eveningoutpost.dexdrip.Models.BgReading;

import org.junit.Test;

import static com.google.common.truth.Truth.assertWithMessage;

public class ShareGlucoseRecordTest {

    @Test
    public void slopeDirectionTest() {
        // TODO
    }

    @Test
    public void slopePerMsFromDirectionTest() {

        for (int direction = 1; direction < 8; direction++) {
            final ShareGlucoseRecord record = new ShareGlucoseRecord();
            record.Trend = direction;
            assertWithMessage("Slope check " + direction).that(BgReading.slopeName(record.slopePerMsFromDirection() * 60000)).isEqualTo(record.slopeDirection());
        }

    }
}