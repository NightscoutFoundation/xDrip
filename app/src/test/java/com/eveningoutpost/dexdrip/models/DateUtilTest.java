package com.eveningoutpost.dexdrip.models;

import android.os.Build;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.OrderWith;
import org.junit.runner.RunWith;
import org.junit.runner.manipulation.Alphanumeric;
import org.robolectric.annotation.Config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.assertEquals;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

@RunWith(Enclosed.class)
@OrderWith(Alphanumeric.class)
public class DateUtilTest {

    public static Collection<String> testISODateStrings = Arrays.asList("2018-03-03 11:22:48", "2018-03-03T11:22:48", "2018-03-03T11:22:48Z",
            "2018-03-03T11:22:48.384Z", "2018-03-03T12:22:48+01:00", "2018-03-03T05:22:48-06:00", "2018-03-03T14:22:48+0300");
    public static Date dtExpected = Date.from(Instant.parse("2018-03-03T11:22:48.000Z"));

    @Config(sdk = Build.VERSION_CODES.O)
    public static class UpperSdkVersionsTest extends RobolectricTestWithConfig{

        @Test
        public void tolerantFromISODateString() {
            for (String strInput : testISODateStrings) {
                try {
                    assertEquals(dtExpected, DateUtil.tolerantFromISODateString(strInput));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Config(sdk = Build.VERSION_CODES.O)
    public static class DateUtilMiscTests extends RobolectricTestWithConfig {

        @Test
        public void tolerantFromISODateStringWithoutSeconds() {
            try {
                Date dtExpectedZeroSeconds = Date.from(dtExpected.toInstant().truncatedTo(ChronoUnit.MINUTES));
                assertEquals(dtExpectedZeroSeconds, DateUtil.tolerantFromISODateString("2018-03-03T05:22-0600"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
