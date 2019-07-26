package com.eveningoutpost.dexdrip.cgm.nsfollow.utils;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;

import org.junit.Before;
import org.junit.Test;

import java.util.TimeZone;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * jamorham
 */
public class AnticipateTestRobolectric extends RobolectricTestWithConfig {

    @Before
    public void initLocale() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    @Test // look for the first failure for issue as they are daisy chained
    public void nextTest() {

        final long now = 1545157669000L;
        final long last = now - Constants.MINUTE_IN_MS;

        // Tuesday 18. December 2018 18:27:49
        assertWithMessage("first now time").that(now).isEqualTo(1545157669000L);
        // Tuesday 18. December 2018 18:26:49
        assertWithMessage("first last time").that(last).isEqualTo(1545157609000L);
        //System.out.println("Time lst: "+ dateTimeText(last));
        // System.out.println("Time now: "+ dateTimeText(now));

        long next = next(now, last);

        // recent reading
        assertWithMessage("next 0").that(dateTimeText(next)).isEqualTo("2018-12-18 18:31:39");
        next = next(next, last);
        assertWithMessage("next 1").that(dateTimeText(next)).isEqualTo("2018-12-18 18:31:49");
        next = next(next, last);
        assertWithMessage("next 2").that(dateTimeText(next)).isEqualTo("2018-12-18 18:31:59");
        next = next(next, last);
        assertWithMessage("next 3").that(dateTimeText(next)).isEqualTo("2018-12-18 18:32:09");
        next = next(next, last);

        // Modulus calculation
        assertWithMessage("next 4").that(dateTimeText(next)).isEqualTo("2018-12-18 18:36:39");
        next = next(next, last);
        assertWithMessage("next 5").that(dateTimeText(next)).isEqualTo("2018-12-18 18:36:49");
        next = next(next, last);
        assertWithMessage("next 6").that(dateTimeText(next)).isEqualTo("2018-12-18 18:36:59");
        next = next(next, last);
        assertWithMessage("next 7").that(dateTimeText(next)).isEqualTo("2018-12-18 18:37:09");
        next = next(next, last);
        assertWithMessage("next 8").that(dateTimeText(next)).isEqualTo("2018-12-18 18:41:39");
        next = next(next, last);
        assertWithMessage("next 9").that(dateTimeText(next)).isEqualTo("2018-12-18 18:41:49");

        next = next(next - Constants.SECOND_IN_MS, last);
        assertWithMessage("next -1").that(dateTimeText(next)).isEqualTo("2018-12-18 18:41:49");

        next = next(next + Constants.SECOND_IN_MS, last);
        assertWithMessage("next +1").that(dateTimeText(next)).isEqualTo("2018-12-18 18:41:59");

        next = next(now, 0);
        assertWithMessage("next from 0").that(dateTimeText(next)).isEqualTo("2018-12-18 18:29:50");
    }


    private static long next(final long now, final long last) {
        return Anticipate.next(now, last, Constants.MINUTE_IN_MS * 5, Constants.SECOND_IN_MS * 10);
    }

    private static String dateTimeText(long t) {
        return JoH.dateTimeText(t);
    }

}