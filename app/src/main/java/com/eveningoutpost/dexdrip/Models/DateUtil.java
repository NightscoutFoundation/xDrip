package com.eveningoutpost.dexdrip.Models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

// from package info.nightscout.client.utils;

/**
 * Created by mike on 30.12.2015.
 */

/**
 * The Class DateUtil. A simple wrapper around SimpleDateFormat to ease the handling of iso date string &lt;-&gt; date obj
 * with TZ
 */
public class DateUtil {

    private static final String FORMAT_DATE_ISO = "yyyy-MM-dd'T'HH:mm:ss'Z'"; // eg 2017-03-24T22:03:27Z
    private static final String FORMAT_DATE_ISO2 = "yyyy-MM-dd'T'HH:mm:ssZ"; // eg 2017-03-27T17:38:14+0300
    private static final String FORMAT_DATE_ISO3 = "yyyy-MM-dd'T'HH:mmZ"; // eg 2017-05-12T08:16-0400

    /**
     * Takes in an ISO date string of the following format:
     * yyyy-mm-ddThh:mm:ss.ms+HoMo
     *
     * @param isoDateString the iso date string
     * @return the date
     * @throws Exception the exception
     */
    private static Date fromISODateString(String isoDateString)
            throws Exception {
        SimpleDateFormat f = new SimpleDateFormat(FORMAT_DATE_ISO);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.parse(isoDateString);
    }

    private static Date fromISODateString3(String isoDateString)
            throws Exception {
        SimpleDateFormat f = new SimpleDateFormat(FORMAT_DATE_ISO3);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.parse(isoDateString);
    }

    private static Date fromISODateString2(String isoDateString)
            throws Exception {
        try {
            SimpleDateFormat f = new SimpleDateFormat(FORMAT_DATE_ISO2);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            return f.parse(isoDateString);
        } catch (java.text.ParseException e) {
            return fromISODateString3(isoDateString);
        }
    }

    public static Date tolerantFromISODateString(String isoDateString)
            throws Exception {
        try {
            return fromISODateString(isoDateString.replaceFirst("\\.[0-9][0-9][0-9]Z$", "Z"));
        } catch (java.text.ParseException e) {
            return fromISODateString2(isoDateString);
        }
    }

    /**
     * Render date
     *
     * @param date   the date obj
     * @param format - if not specified, will use FORMAT_DATE_ISO
     * @param tz     - tz to set to, if not specified uses local timezone
     * @return the iso-formatted date string
     */
    public static String toISOString(Date date, String format, TimeZone tz) {
        if (format == null) format = FORMAT_DATE_ISO;
        if (tz == null) tz = TimeZone.getDefault();
        DateFormat f = new SimpleDateFormat(format);
        f.setTimeZone(tz);
        return f.format(date);
    }

    public static String toISOString(Date date) {
        return toISOString(date, FORMAT_DATE_ISO, TimeZone.getTimeZone("UTC"));
    }

    public static String toISOString(long date) {
        return toISOString(new Date(date), FORMAT_DATE_ISO, TimeZone.getTimeZone("UTC"));
    }

    public static String toNightscoutFormat(long date) {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(date);
    }
}