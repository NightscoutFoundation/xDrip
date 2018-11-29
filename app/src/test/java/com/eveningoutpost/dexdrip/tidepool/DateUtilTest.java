package com.eveningoutpost.dexdrip.tidepool;

import org.junit.Test;

import java.util.TimeZone;

import static com.google.common.truth.Truth.assertWithMessage;

public class DateUtilTest {

    @Test
    public void toFormatWithZoneTest() {
        assertWithMessage("Specimen As UTC").that(DateUtil.toFormatAsUTC(1536873803123L)).isEqualTo("2018-09-13T21:23:23.1230000Z");
    }

    @Test
    public void toFormatNoZoneTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+3"));
        assertWithMessage("Specimen as GMT+3").that(DateUtil.toFormatNoZone(1536873803123L)).isEqualTo("2018-09-14T00:23:23");
    }
}