package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Created by jamorham on 17/03/2018.
 */

public class LibreCrcTest extends RobolectricTestWithConfig {

    @Test
    public void crc16test1() {

        final byte[] testa = new byte[]{0x00, 0x00, 0x00};

        final byte[] testb = new byte[]{0x3e, 0x49, (byte) 0x91, (byte) 0xb4, (byte) 0x8b, (byte) 0xcb,
                (byte) 0x1b, (byte) 0xd9, (byte) 0xd3, (byte) 0xc4, (byte) 0xc1, (byte) 0x4a, (byte) 0x1f,
                (byte) 0x24, (byte) 0xc4, 0x15, (byte) 0xde, (byte) 0xab, (byte) 0xa4, 0x66};

        assertWithMessage("1 byte not processed A").that(LibreUtils.computeCRC16(testa, 0, 1)).isEqualTo(65535);
        assertWithMessage("1 byte not processed B").that(LibreUtils.computeCRC16(testb, 0, 1)).isEqualTo(65535);
        assertWithMessage("Example data test").that(LibreUtils.computeCRC16(testb, 0, testb.length - 2)).isEqualTo(19459);


    }

}
