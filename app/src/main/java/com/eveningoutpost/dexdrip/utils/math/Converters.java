package com.eveningoutpost.dexdrip.utils.math;

/**
 * jamorham
 *
 * Small utility converters
 *
 * TODO: there may be some duplication in some message classes
 */

public class Converters {

    public static int unsignedBytesToInt(final byte[] bytes) {
        if (bytes == null) return -1;
        switch (bytes.length) {
            case 1:
                return unsignedByteToInt(bytes[0]);
            case 2:
                return unsignedBytesToInt(bytes[0], bytes[1]);
            default:
                throw new RuntimeException("Not handled more than 2 bytes at the moment: " + bytes.length);
        }
    }

    public static int unsignedBytesToInt(final byte byte0, final byte byte1) {
        return unsignedByteToInt(byte0) + (unsignedByteToInt(byte1) << 8);
    }

    public static int unsignedByteToInt(final byte b) {
        return b & 0xFF;
    }

    public static int unsignedShortToInt(final short b) {
        return b & 0xFFFF;
    }

    public static long unsignedIntToLong(final int b) {
        return ((long)b) & 0xFFFFFFFF;
    }


}
