package com.eveningoutpost.dexdrip.Models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

// from package info.nightscout.client.utils;

/**
 * Created by mike on 30.12.2015.
 */

/**
 * The Class DateUtil. A simple wrapper around SimpleDateFormat to ease the handling of iso date string &lt;-&gt; date obj
 * with TZ
 */
public class DateUtil
{

    /** The date format in iso. */
    public static String FORMAT_DATE_ISO="yyyy-MM-dd'T'HH:mm:ssZ";

    /**
     * Takes in an ISO date string of the following format:
     * yyyy-mm-ddThh:mm:ss.ms+HoMo
     *
     * @param isoDateString the iso date string
     * @return the date
     * @throws Exception the exception
     */
    public static Date fromISODateString(String isoDateString)
            throws Exception
    {
        DateFormat f = new SimpleDateFormat(FORMAT_DATE_ISO);
        return f.parse(isoDateString);
    }

    /**
     * Render date
     *
     * @param date the date obj
     * @param format - if not specified, will use FORMAT_DATE_ISO
     * @param tz - tz to set to, if not specified uses local timezone
     * @return the iso-formatted date string
     */
    public static String toISOString(Date date, String format, TimeZone tz)
    {
        if( format == null ) format = FORMAT_DATE_ISO;
        if( tz == null ) tz = TimeZone.getDefault();
        DateFormat f = new SimpleDateFormat(format);
        f.setTimeZone(tz);
        return f.format(date);
    }

    public static String toISOString(Long date)
    { return toISOString(new Date(date), FORMAT_DATE_ISO, TimeZone.getDefault()); }

    public static String toISOString(Date date)
    { return toISOString(date,FORMAT_DATE_ISO,TimeZone.getDefault()); }
}