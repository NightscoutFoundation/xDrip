package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class CRC16ccittTest extends RobolectricTestWithConfig {

    // -- Known test vector: "123456789" with initial 0xFFFF --
    // Standard CRC-16/CCITT for "123456789" is 0x29B1

    @Test
    public void knownInput_producesExpectedChecksum() {
        byte[] data = "123456789".getBytes();
        byte[] result = CRC16ccitt.crc16ccitt(data, false, false);
        int crc = (result[1] & 0xFF) << 8 | (result[0] & 0xFF);
        assertWithMessage("CRC of '123456789'").that(crc).isEqualTo(0x29B1);
    }

    // -- skip_last_two excludes final 2 bytes --

    @Test
    public void skipLastTwo_excludesFinalBytes() {
        byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
        byte[] withoutSkip = CRC16ccitt.crc16ccitt(
                new byte[]{0x01, 0x02, 0x03}, false, false);
        byte[] withSkip = CRC16ccitt.crc16ccitt(data, true, false);
        assertThat(withSkip).isEqualTo(withoutSkip);
    }

    // -- skip_first excludes first byte --

    @Test
    public void skipFirst_changesCrc() {
        byte[] data = {0x01, 0x02, 0x03, 0x04};
        byte[] skipFirstResult = CRC16ccitt.crc16ccitt(data, false, true);
        byte[] processAllResult = CRC16ccitt.crc16ccitt(data, false, false);
        assertThat(skipFirstResult).isNotEqualTo(processAllResult);
    }

    // -- Empty effective input returns initial value --

    @Test
    public void singleByte_skipFirst_returnsInitialValue() {
        byte[] data = {0x42};
        byte[] result = CRC16ccitt.crc16ccitt(data, false, true);
        int crc = (result[1] & 0xFF) << 8 | (result[0] & 0xFF);
        assertWithMessage("No bytes processed should give initial value")
                .that(crc).isEqualTo(0xFFFF);
    }

    // -- Result is always 2 bytes (little-endian) --

    @Test
    public void result_isAlwaysTwoBytes() {
        byte[] data = {0x01, 0x02, 0x03};
        byte[] result = CRC16ccitt.crc16ccitt(data, false, false);
        assertThat(result.length).isEqualTo(2);
    }

    // -- Custom initial value changes result --

    @Test
    public void customInitialValue_changesCrc() {
        byte[] data = {0x01, 0x02, 0x03};
        byte[] defaultInit = CRC16ccitt.crc16ccitt(data, false, false, 0xFFFF);
        byte[] customInit = CRC16ccitt.crc16ccitt(data, false, false, 0x0000);
        assertThat(defaultInit).isNotEqualTo(customInit);
    }

    // -- Same input always gives same output --

    @Test
    public void sameInput_givesSameOutput() {
        byte[] data = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
        byte[] first = CRC16ccitt.crc16ccitt(data, false, false);
        byte[] second = CRC16ccitt.crc16ccitt(data, false, false);
        assertThat(first).isEqualTo(second);
    }
}
