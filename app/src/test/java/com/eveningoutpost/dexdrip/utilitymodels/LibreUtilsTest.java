package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class LibreUtilsTest extends RobolectricTestWithConfig {

    // -- computeCRC16: deterministic --

    @Test
    public void computeCRC16_sameInput_sameOutput() {
        byte[] data = new byte[]{0x00, 0x00, 0x10, 0x20, 0x30, 0x40};
        long first = LibreUtils.computeCRC16(data, 0, data.length);
        long second = LibreUtils.computeCRC16(data, 0, data.length);
        assertThat(first).isEqualTo(second);
    }

    // -- computeCRC16: different data gives different CRC --

    @Test
    public void computeCRC16_differentData_differentCrc() {
        byte[] a = new byte[]{0x00, 0x00, 0x10, 0x20, 0x30};
        byte[] b = new byte[]{0x00, 0x00, 0x10, 0x20, 0x31};
        long crcA = LibreUtils.computeCRC16(a, 0, a.length);
        long crcB = LibreUtils.computeCRC16(b, 0, b.length);
        assertThat(crcA).isNotEqualTo(crcB);
    }

    // -- computeCRC16: known value from existing test --

    @Test
    public void computeCRC16_knownTestVector() {
        byte[] data = new byte[]{0x3e, 0x49, (byte) 0x91, (byte) 0xb4,
                (byte) 0x8b, (byte) 0xcb, (byte) 0x1b, (byte) 0xd9,
                (byte) 0xd3, (byte) 0xc4, (byte) 0xc1, (byte) 0x4a,
                (byte) 0x1f, (byte) 0x24, (byte) 0xc4, 0x15,
                (byte) 0xde, (byte) 0xab, (byte) 0xa4, 0x66};
        long crc = LibreUtils.computeCRC16(data, 0, data.length - 2);
        assertThat(crc).isEqualTo(19459);
    }

    // -- isSensorReady: ready states --

    @Test
    public void isSensorReady_starting_isReady() {
        assertThat(LibreUtils.isSensorReady((byte) 0x02)).isTrue();
    }

    @Test
    public void isSensorReady_ready_isReady() {
        assertThat(LibreUtils.isSensorReady((byte) 0x03)).isTrue();
    }

    // -- isSensorReady: not-ready states --

    @Test
    public void isSensorReady_notStarted_isNotReady() {
        assertThat(LibreUtils.isSensorReady((byte) 0x01)).isFalse();
    }

    @Test
    public void isSensorReady_expired_isNotReady() {
        assertThat(LibreUtils.isSensorReady((byte) 0x04)).isFalse();
    }

    @Test
    public void isSensorReady_shutdown_isNotReady() {
        assertThat(LibreUtils.isSensorReady((byte) 0x05)).isFalse();
    }

    @Test
    public void isSensorReady_failure_isNotReady() {
        assertThat(LibreUtils.isSensorReady((byte) 0x06)).isFalse();
    }

    @Test
    public void isSensorReady_unknownStatus_isNotReady() {
        assertThat(LibreUtils.isSensorReady((byte) 0xFF)).isFalse();
    }

    // -- validatePatchInfo --

    @Test
    public void validatePatchInfo_validBytes_returnsTrue() {
        byte[] buf = new byte[11];
        buf[9] = 0x07;
        buf[10] = (byte) 0xE0;
        assertThat(LibreUtils.validatePatchInfo(buf)).isTrue();
    }

    @Test
    public void validatePatchInfo_tooShort_returnsFalse() {
        byte[] buf = new byte[10];
        assertThat(LibreUtils.validatePatchInfo(buf)).isFalse();
    }

    @Test
    public void validatePatchInfo_wrongBytes_returnsFalse() {
        byte[] buf = new byte[11];
        buf[9] = 0x07;
        buf[10] = 0x00;
        assertThat(LibreUtils.validatePatchInfo(buf)).isFalse();
    }

    // -- decodeSerialNumber: deterministic --

    @Test
    public void decodeSerialNumber_sameInput_sameOutput() {
        byte[] uid = new byte[11];
        uid[3] = 0x01; uid[4] = 0x02; uid[5] = 0x03;
        uid[6] = 0x04; uid[7] = 0x05; uid[8] = 0x06;
        uid[9] = 0x07; uid[10] = 0x08;
        String first = LibreUtils.decodeSerialNumber(uid);
        String second = LibreUtils.decodeSerialNumber(uid);
        assertThat(first).isEqualTo(second);
    }

    // -- decodeSerialNumber: starts with '0' --

    @Test
    public void decodeSerialNumber_startsWithZero() {
        byte[] uid = new byte[11];
        String serial = LibreUtils.decodeSerialNumber(uid);
        assertThat(serial).startsWith("0");
    }

    // -- decodeSerialNumber: length is 11 (prefix '0' + 10 encoded chars) --

    @Test
    public void decodeSerialNumber_hasExpectedLength() {
        byte[] uid = new byte[11];
        uid[3] = 0x12; uid[4] = 0x34; uid[5] = 0x56;
        uid[6] = 0x78; uid[7] = (byte) 0x9A; uid[8] = (byte) 0xBC;
        String serial = LibreUtils.decodeSerialNumber(uid);
        assertThat(serial.length()).isEqualTo(11);
    }
}
