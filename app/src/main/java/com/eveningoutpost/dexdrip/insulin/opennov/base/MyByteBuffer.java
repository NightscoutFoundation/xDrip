package com.eveningoutpost.dexdrip.insulin.opennov.base;

import java.nio.ByteBuffer;

/**
 * JamOrHam
 * OpenNov base buffer helper
 */

public class MyByteBuffer {

    public static int getUnsignedByte(final ByteBuffer data) {
        return data.get() & 0xff;
    }

    public static void putUnsignedByte(final ByteBuffer data, int s) {
        data.put((byte) (s & 0xff));
    }

    public static int getUnsignedShort(final ByteBuffer data) {
        return data.getShort() & 0xffff;
    }

    public static void putUnsignedShort(final ByteBuffer data, int s) {
        data.putShort((short) (s & 0xffff));
    }

    public static long getUnsignedInt(final ByteBuffer data) {
        return data.getInt() & 0xffffffffL;
    }

    public static void putUnsignedInt(final ByteBuffer data, long s) {
        data.putInt((int) (s & 0xffffffff));
    }

}
