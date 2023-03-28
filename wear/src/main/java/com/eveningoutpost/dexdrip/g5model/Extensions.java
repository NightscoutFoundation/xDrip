package com.eveningoutpost.dexdrip.g5model;

/**
 * Created by joeginley on 3/19/16.
 */
public class Extensions {

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String lastTwoCharactersOfString(final String s) {
        if (s == null) return "NULL";
        return s.length() > 1 ? s.substring(s.length() - 2) : "ERR-" + s;
    }

    public static void doSleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
