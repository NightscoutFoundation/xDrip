package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * @author Asbjørn Aarrestad
 */
public class CRC16ccittTest extends RobolectricTestWithConfig {

    // -- Known test vector: "123456789" with initial 0xFFFF --

    @Test
    public void knownInput_producesExpectedChecksum() {
        // :: Act
        byte[] data = "123456789".getBytes();
        byte[] result = CRC16ccitt.crc16ccitt(data, false, false);
        int crc = (result[1] & 0xFF) << 8 | (result[0] & 0xFF);

        // :: Verify — standard CRC-16/CCITT for "123456789" is 0x29B1
        assertWithMessage("CRC of '123456789'").that(crc).isEqualTo(0x29B1);
    }

    // -- skip_last_two excludes final 2 bytes --

    @Test
    public void skipLastTwo_excludesFinalBytes() {
        // :: Setup
        byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};

        // :: Act
        byte[] withoutSkip = CRC16ccitt.crc16ccitt(
                new byte[]{0x01, 0x02, 0x03}, false, false);
        byte[] withSkip = CRC16ccitt.crc16ccitt(data, true, false);

        // :: Verify
        assertThat(withSkip).isEqualTo(withoutSkip);
    }

    // -- skip_first excludes first byte --

    @Test
    public void skipFirst_changesCrc() {
        // :: Setup
        byte[] data = {0x01, 0x02, 0x03, 0x04};

        // :: Act
        byte[] skipFirstResult = CRC16ccitt.crc16ccitt(data, false, true);
        byte[] processAllResult = CRC16ccitt.crc16ccitt(data, false, false);

        // :: Verify
        assertThat(skipFirstResult).isNotEqualTo(processAllResult);
    }

    // -- Empty effective input returns initial value --

    @Test
    public void singleByte_skipFirst_returnsInitialValue() {
        // :: Act
        byte[] result = CRC16ccitt.crc16ccitt(new byte[]{0x42}, false, true);
        int crc = (result[1] & 0xFF) << 8 | (result[0] & 0xFF);

        // :: Verify
        assertWithMessage("No bytes processed should give initial value")
                .that(crc).isEqualTo(0xFFFF);
    }

    // -- Result is always 2 bytes (little-endian) --

    @Test
    public void result_isAlwaysTwoBytes() {
        // :: Act
        byte[] result = CRC16ccitt.crc16ccitt(new byte[]{0x01, 0x02, 0x03}, false, false);

        // :: Verify
        assertThat(result.length).isEqualTo(2);
    }

    // -- Custom initial value changes result --

    @Test
    public void customInitialValue_changesCrc() {
        // :: Setup
        byte[] data = {0x01, 0x02, 0x03};

        // :: Act
        byte[] defaultInit = CRC16ccitt.crc16ccitt(data, false, false, 0xFFFF);
        byte[] customInit = CRC16ccitt.crc16ccitt(data, false, false, 0x0000);

        // :: Verify
        assertThat(defaultInit).isNotEqualTo(customInit);
    }

    // -- Same input always gives same output --

    @Test
    public void sameInput_givesSameOutput() {
        // :: Setup
        byte[] data = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};

        // :: Act
        byte[] first = CRC16ccitt.crc16ccitt(data, false, false);
        byte[] second = CRC16ccitt.crc16ccitt(data, false, false);

        // :: Verify
        assertThat(first).isEqualTo(second);
    }
}
