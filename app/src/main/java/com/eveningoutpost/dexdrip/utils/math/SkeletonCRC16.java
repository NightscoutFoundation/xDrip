package com.eveningoutpost.dexdrip.utils.math;

/**
 * jamorham
 *
 * Skeleton Key CRC routines
 *
 * Not optimized, designed for small data packets from various devices which all seem to use
 * a different flavour of CRC16
 *
 */

public class SkeletonCRC16 {


    static int reverseShort(int x) {
        x = (x & 0x5555) << 1 | (x >>> 1) & 0x5555;
        x = (x & 0x3333) << 2 | (x >>> 2) & 0x3333;
        x = (x & 0x0F0F) << 4 | (x >>> 4) & 0x0F0F;
        x = (x & 0x00FF) << 8 | (x >>> 8);
        return x;
    }

    static int reverseInt(long x) {
        x = (x & 0xAAAAAAAA) >> 1 | (x & 0x55555555) << 1;
        x = (x & 0xCCCCCCCC) >> 2 | (x & 0x33333333) << 2;
        x = (x & 0xF0F0F0F0) >> 4 | (x & 0x0F0F0F0F) << 4;
        x = (x & 0xFF00FF00) >> 8 | (x & 0x00FF00FF) << 8;
        return (int)(x >> 16 | x << 16);
    }


    public static int crc16Lsb(final byte[] bytes, int crc, final int polynomial, final int length) {
        for (int p = 0; p < length; p++) {
            crc ^= bytes[p] & 0xFF;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 1) == 0 ? crc >> 1 : (crc >> 1) ^ polynomial;
            }
        }
        return crc;
    }

    public static int crc16Msb(final byte[] bytes, int crc, final int polynomial, final int length) {
        for (int p = 0; p < length; p++) {
            crc ^= (bytes[p] & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? ((crc << 1) & 0xFFFF) ^ polynomial : (crc << 1) & 0xFFFF;
            }
        }
        return crc;
    }


}
