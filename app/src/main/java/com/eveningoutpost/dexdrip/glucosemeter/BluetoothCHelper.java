package com.eveningoutpost.dexdrip.glucosemeter;

/**
 * Created by jamorham on 07/12/2016.
 */

abstract class BluetoothCHelper {

    float getSfloat16(byte b0, byte b1) {
        int mantissa = unsignedToSigned(unsignedByteToInt(b0)
                + ((unsignedByteToInt(b1) & 0x0F) << 8), 12);
        int exponent = unsignedToSigned(unsignedByteToInt(b1) >> 4, 4);
        return (float) (mantissa * Math.pow(10, exponent));
    }

    int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    int unsignedBytesToInt(byte b, byte c) {
        return ((unsignedByteToInt(c) << 8) + unsignedByteToInt(b)) & 0xFFFF;
    }

    private int unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size - 1)) != 0) {
            unsigned = -1 * ((1 << size - 1) - (unsigned & ((1 << size - 1) - 1)));
        }
        return unsigned;
    }

}
