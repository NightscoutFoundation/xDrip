package com.eveningoutpost.dexdrip.evaluators;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfigAndPowerMockLogRedirect;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;

import org.junit.Test;

import static com.google.common.truth.Truth.assertWithMessage;

// jamorham

public class PersistentHighTest extends RobolectricTestWithConfigAndPowerMockLogRedirect {

    private static long START_TIME = JoH.tsl() - Constants.HOUR_IN_MS * 4;
    private static final double HIGH_MARK = 170;


    @Test
    public void dataQualityCheckTest() {

        assertWithMessage("No sensor invalid time should fail").that(PersistentHigh.dataQualityCheck(1000, HIGH_MARK)).isFalse();
        assertWithMessage("No sensor ok time should fail").that(PersistentHigh.dataQualityCheck(START_TIME, HIGH_MARK)).isFalse();
        assertWithMessage("No sensor ok time should fail 2").that(PersistentHigh.dataQualityCheck(JoH.tsl(), HIGH_MARK)).isFalse();
        Sensor.create(1005);
        assertWithMessage("Predating sensor should fail").that(PersistentHigh.dataQualityCheck(1000, HIGH_MARK)).isFalse();
        assertWithMessage("Post sensor start no data should fail").that(PersistentHigh.dataQualityCheck(JoH.tsl(), HIGH_MARK)).isFalse();

        Sensor.create(START_TIME);
        // various all high time slices
        for (int i = 0; i < (12 * 4); i++) {
            final long timestamp = START_TIME + Constants.MINUTE_IN_MS * 5 * i;
            final BgReading bgr = BgReading.bgReadingInsertFromG5(400, timestamp);
            assertWithMessage("Test result A: " + i + " " + JoH.dateTimeText(timestamp) + " different to expected").that(PersistentHigh.dataQualityCheck(START_TIME + Constants.MINUTE_IN_MS * 10, HIGH_MARK)).isEqualTo(i > 16);
        }

        START_TIME++;
        Sensor.create(START_TIME);
        // single dipped point in sequence after a point we might have succeeded
        for (int i = 0; i < (12 * 4); i++) {
            final long timestamp = START_TIME + Constants.MINUTE_IN_MS * 5 * i;
            final BgReading bgr = BgReading.bgReadingInsertFromG5(i == 20 ? 100 : 400, timestamp);
            assertWithMessage("Test result C: " + i + " " + JoH.dateTimeText(timestamp) + " different to expected").that(PersistentHigh.dataQualityCheck(START_TIME + Constants.MINUTE_IN_MS * 10, HIGH_MARK)).isEqualTo(i > 16 && i < 20);
        }

        START_TIME++;
        Sensor.create(START_TIME);
        // single dipped point in sequence before we would succeed
        for (int i = 0; i < (12 * 4); i++) {
            final long timestamp = START_TIME + Constants.MINUTE_IN_MS * 5 * i;
            final BgReading bgr = BgReading.bgReadingInsertFromG5(i == 8 ? 100 : 400, timestamp);
            assertWithMessage("Test result D: " + i + " " + JoH.dateTimeText(timestamp) + " different to expected").that(PersistentHigh.dataQualityCheck(START_TIME + Constants.MINUTE_IN_MS * 10, HIGH_MARK)).isFalse();
        }

    }


}