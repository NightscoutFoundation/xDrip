package com.eveningoutpost.dexdrip.importedlibraries.dexcom;

import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.EGVRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.GlucoseDataSet;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.SensorRecord;

import java.util.Date;
import java.util.TimeZone;

// This code and this particular library are from the NightScout android uploader
// Check them out here: https://github.com/nightscout/android-uploader
// Some of this code may have been modified for use in this project

public class Utils {

    public static Date receiverTimeToDate(long delta) {
        int currentTZOffset = TimeZone.getDefault().getRawOffset();
        long epochMS = 1230768000000L;  // Jan 01, 2009 00:00 in UTC
        long milliseconds = epochMS - currentTZOffset;
        long timeAdd = milliseconds + (1000L * delta);
        TimeZone tz = TimeZone.getDefault();
        if (tz.inDaylightTime(new Date())) timeAdd = timeAdd - (1000 * 60 * 60);
        return new Date(timeAdd);
    }

    public static String getTimeString(long timeDeltaMS) {
        long minutes = (timeDeltaMS / 1000) / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        minutes= minutes - hours * 60;
        hours = hours - days * 24;
        days= days - weeks * 7;

        String timeAgoString = "";
        if (weeks > 0) {
            timeAgoString += weeks + " weeks ";
        }
        if (days > 0) {
            timeAgoString += days + " days ";
        }
        if (hours > 0) {
            timeAgoString += hours + " hours ";
        }
        if (minutes >= 0) {
            timeAgoString += minutes + " min ";
        }

        return (timeAgoString.equals("") ? "--" : timeAgoString + "ago");
    }

    public static GlucoseDataSet[] mergeGlucoseDataRecords(EGVRecord[] egvRecords,
                                                           SensorRecord[] sensorRecords) {
        int egvLength = egvRecords.length;
        int sensorLength = sensorRecords.length;
        int smallerLength = egvLength < sensorLength ? egvLength : sensorLength;
        GlucoseDataSet[] glucoseDataSets = new GlucoseDataSet[smallerLength];
        for (int i = 1; i <= smallerLength; i++) {
            glucoseDataSets[smallerLength - i] = new GlucoseDataSet(egvRecords[egvLength - i], sensorRecords[sensorLength - i]);
        }
        return glucoseDataSets;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = " ".toCharArray()[0];
        }
        return new String(hexChars);
    }
}
