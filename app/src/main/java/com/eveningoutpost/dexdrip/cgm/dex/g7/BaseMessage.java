package com.eveningoutpost.dexdrip.cgm.dex.g7;

import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * JamOrHam
 */

public class BaseMessage {

    public ByteBuffer data;

    // TODO unit test
    static long getUnsignedInt(ByteBuffer data) {
        return ((data.get() & 0xff) + ((data.get() & 0xff) << 8) + ((data.get() & 0xff) << 16) + ((long) (data.get() & 0xff) << 24));
    }

    static int getUnsignedShort(ByteBuffer data) {
        return ((data.get() & 0xff) + ((data.get() & 0xff) << 8));
    }

    static int getUnsignedByte(ByteBuffer data) {
        return ((data.get() & 0xff));
    }

    static String dottedStringFromData(ByteBuffer data, int length) {

        final byte[] bytes = new byte[length];
        data.get(bytes);
        final StringBuilder sb = new StringBuilder(100);
        for (byte x : bytes) {
            if (sb.length() > 0) sb.append(".");
            sb.append(String.format(Locale.US, "%d", (x & 0xff)));
        }
        return sb.toString();
    }
}
