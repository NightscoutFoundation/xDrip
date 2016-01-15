package com.eveningoutpost.dexdrip.Models;

import java.text.DecimalFormat;

/**
 * Created by jamorham on 06/01/16.
 *
 * lazy helper class for utilities
 */
public class JoH {
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    // qs = quick string conversion of double for printing
    public static String qs(double x) {
        return qs(x, 2);
    }

    public static String qs(double x, int digits) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(digits);
        df.setMinimumIntegerDigits(1);
        return df.format(x);
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
