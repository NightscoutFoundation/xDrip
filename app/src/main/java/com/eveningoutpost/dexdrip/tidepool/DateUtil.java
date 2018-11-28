package com.eveningoutpost.dexdrip.tidepool;

/**
 * jamorham
 *
 * Date utilities for preparing items for Tidepool upload
 */

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtil {

    static String toFormatAsUTC(final long timestamp) {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'0000Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(timestamp);
    }

    static String toFormatWithZone2(final long timestamp) {
        // ISO 8601 not introduced till api 24 - so we have to do some gymnastics
        final SimpleDateFormat formatIso8601 = new SimpleDateFormat("Z", Locale.US);
        formatIso8601.setTimeZone(TimeZone.getDefault());
        String zone = formatIso8601.format(timestamp);
        zone = zone.substring(0, zone.length() - 2) + ":" + zone.substring(zone.length() - 2);
        if (zone.substring(0, 1).equals("+")) {
            zone = zone.substring(1);
        }
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z" + zone + "'", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(timestamp);
    }


    static String toFormatNoZone(final long timestamp) {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(timestamp);
    }

    static int getTimeZoneOffsetMinutes(final long timestamp) {
        return TimeZone.getDefault().getOffset(timestamp) / 60000;
    }

}
