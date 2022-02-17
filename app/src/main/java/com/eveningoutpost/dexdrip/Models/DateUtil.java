package com.eveningoutpost.dexdrip.Models;

import org.joda.time.DateTime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * The Class DateUtil. A simple wrapper to ease the handling of iso date string &lt;-&gt; date object conversion.
 * Supports strings with our without time zone/offset values and uses java.time classes if compiled with Android API >= Oreo.
 *
 * Created by mike on 30.12.2015.
 *
 */
public class DateUtil {

    private static final String FORMAT_DATE_ISO = "yyyy-MM-dd'T'HH:mm:ss'Z'"; // eg 2017-03-24T22:03:27Z
    private static final Pattern pDateFix = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})[ T](\\d{2}:\\d{2}:\\d{2})(?:.\\d+Z)*[^Z]?$");
    private static final Pattern pOffsetFix = Pattern.compile("([+-]\\d{2})(\\d{2})$");

    /**
     * Takes in an ISO date string of the following format:
     * yyyy-MM-dd'T'HH:mm:ss.SSS(Z|+/-HH:mm)
     *
     * The milliseconds and time zone/offset values are optional.
     *
     * @param isoDateString the iso date string to parse
     * @return the date object
     */
    public static Date tolerantFromISODateString(String isoDateString) {
        String strDateTime = pDateFix.matcher(isoDateString).replaceFirst("$1T$2Z");
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) { // use Joda Time classes
            return DateTime.parse(strDateTime).toDate();
        } else {  // use built-in java.time classes
            OffsetDateTime odt = OffsetDateTime.parse(pOffsetFix.matcher(strDateTime).replaceFirst("$1:$2"));
            return Date.from(odt.toInstant());
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