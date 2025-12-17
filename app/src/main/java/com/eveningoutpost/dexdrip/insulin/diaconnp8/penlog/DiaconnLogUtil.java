package com.eveningoutpost.dexdrip.insulin.diaconnp8.penlog;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DiaconnLogUtil {
    public static String getDttm(ByteBuffer buffer) {
        byte b0 = buffer.get();
        byte b1 = buffer.get();
        byte b2 = buffer.get();
        byte b3 = buffer.get();
        long pumpTime = Long.parseLong(String.format("%02x%02x%02x%02x", b3, b2, b1, b0), 16);
        long epochTime = new Date(0).getTime(); // 1970-01-01
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date(epochTime + pumpTime * 1000));
    }
    public static byte getByte(ByteBuffer buffer) {
        return buffer.get();
    }
    public static short getShort(ByteBuffer buffer) {
        return buffer.getShort();
    }
    public static int getInt(ByteBuffer buffer) {
        return buffer.getInt();
    }
    public static byte getType(String data) {
        byte[] bytes = hexStringToByteArray(data);
        return getType(bytes[4]);
    }
    public static byte getKind(String data) {
        byte[] bytes = hexStringToByteArray(data);
        return getKind(bytes[4]);
    }
    public static byte getType(byte b) {
        return (byte) ((b >> 6) & 0b00000011);
    }
    public static byte getKind(byte b) {
        return (byte) (b & 0b00111111);
    }
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
