package com.eveningoutpost.dexdrip.utils;

// jamorham

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

public class CRC16ccitt {

    private static final String TAG = CRC16ccitt.class.getSimpleName();
    private static final boolean d = false;


    public static byte[] crc16ccitt(byte[] bytes, boolean skip_last_two, boolean skip_first) {
        return crc16ccitt(bytes, skip_last_two, skip_first, 0xFFFF);
    }


    public static byte[] crc16ccitt(byte[] bytes, boolean skip_last_two, boolean skip_first, int initial_value) {
        return crc16ccitt(bytes, skip_last_two ? 2 : 0, skip_first, initial_value);
    }

    //  1 + x + x^5 + x^12 + x^16 is irreducible polynomial.
    //  http://introcs.cs.princeton.edu/java/61data/CRC16CCITT.java
    public static byte[] crc16ccitt(byte[] bytes, int skip_last_bytes, boolean skip_first, int initial_value) {
        int crc = initial_value;          // initial value
        final int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)

        int processed = 0;
        final int toprocess = bytes.length - skip_last_bytes;
        for (byte b : bytes) {
            processed++;
            if (processed > toprocess) break;
            if (skip_first) {
                skip_first = false;
                continue;
            }
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff; // 16 bits only
        final byte[] ret = new byte[2];
        ret[0] = (byte) (crc & 0xff);
        ret[1] = (byte) (crc >> 8 & 0xff); // little endian
        if (d) UserError.Log.d(TAG, "CCITT checksum: " + JoH.bytesToHex(ret));
        return ret;
    }

}
