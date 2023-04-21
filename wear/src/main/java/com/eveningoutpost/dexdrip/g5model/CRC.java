package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.importedlibraries.dexcom.CRC16;

/**
 * Created by jcostik1 on 3/24/16.
 */
public class CRC {

    public static byte[] calculate(byte b) {
        int crcShort = 0;
        crcShort = ((crcShort >>> 8) | (crcShort << 8)) & 0xffff;
        crcShort ^= (b & 0xff);
        crcShort ^= ((crcShort & 0xff) >> 4);
        crcShort ^= (crcShort << 12) & 0xffff;
        crcShort ^= ((crcShort & 0xFF) << 5) & 0xffff;
        crcShort &= 0xffff;
        return new byte[] {(byte) (crcShort & 0xff), (byte) ((crcShort >> 8) & 0xff)};
    }

    public static byte[] calculate(byte[] bytes, int start, int end) {
        return CRC16.calculate(bytes, 0, end);
    }

}
