package com.eveningoutpost.dexdrip.insulin.opennov.ll;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.HexTestTools;

import org.junit.Test;

import lombok.val;

public class PHDllTest extends HexTestTools {

    private static final byte[] testBytes = {14, 15, 16, 17, 18, 19, 20};

    @Test
    public void testEncode() {

        val x = PHDll.builder().seq(7).inner(testBytes).build().encode();
        assertWithMessage("matches").that(x)
                .isEqualTo(tolerantHexStringToByteArray("D10308504844870E0F1011121314"));

        val y = PHDll.parse(x);
        assertWithMessage("b7 set").that(y.checkBit(7)).isTrue();
        assertWithMessage("b6 clr").that(y.checkBit(6)).isFalse();
        assertWithMessage("b5 clr").that(y.checkBit(5)).isFalse();
        assertWithMessage("b4 clr").that(y.checkBit(4)).isFalse();
    }

    @Test
    public void testParse() {

        val inner = tolerantHexStringToByteArray(
                "E7 00 00 F8 00 F6 80 01 01 01 00 F0 01 00 00 09 " +
                        "04 52 0D 21 00 E6 00 10 00 00 00 00 00 00 00 12 " +
                        "80 00 00 D8 00 08 88 F2 FF 00 00 0A 08 00 00 00 " +
                        "00 08 83 E3 FF 00 00 0A 08 00 00 00 00 08 83 E1 " +
                        "FF 00 00 0A 08 00 00 00 00 08 83 DE FF 00 00 0A " +
                        "08 00 00 00 00 08 83 DC FF 00 00 0A 08 00 00 00 " +
                        "00 08 83 DB FF 00 00 0A 08 00 00 00 00 08 78 15 " +
                        "FF 00 00 0A 08 00 00 00 00 08 78 13 FF 00 00 0A " +
                        "08 00 00 00 00 08 78 10 FF 00 00 0A 08 00 00 00 " +
                        "00 08 78 0E FF 00 00 0A 08 00 00 00 00 08 78 0C " +
                        "FF 00 00 0A 08 00 00 00 00 08 74 D5 FF 00 00 0A " +
                        "08 00 00 00 00 08 74 D3 FF 00 00 0A 08 00 00 00 " +
                        "00 08 74 D0 FF 00 00 0A 08 00 00 00 00 08 74 CE " +
                        "FF 00 00 0A 08 00 00 00 00 08 73 14 FF 00 00 1E " +
                        "08 00 00 00 00 08 73 10 FF 00 00 1E 08 00 00 00 " +
                        "00 08 73 0D FF 00 00 1E 08 00 00 00 ");

        val x = PHDll.parse(tolerantHexStringToByteArray(
                "D1 03 FD 50 48 44 8E E7 00 00 F8 00 F6 80 01 01 " +
                        "01 00 F0 01 00 00 09 04 52 0D 21 00 E6 00 10 00 " +
                        "00 00 00 00 00 00 12 80 00 00 D8 00 08 88 F2 FF" +
                        "00 00 0A 08 00 00 00 00 08 83 E3 FF 00 00 0A 08 " +
                        "00 00 00 00 08 83 E1 FF 00 00 0A 08 00 00 00 00 " +
                        "08 83 DE FF 00 00 0A 08 00 00 00 00 08 83 DC FF " +
                        "00 00 0A 08 00 00 00 00 08 83 DB FF 00 00 0A 08 " +
                        "00 00 00 00 08 78 15 FF 00 00 0A 08 00 00 00 00 " +
                        "08 78 13 FF 00 00 0A 08 00 00 00 00 08 78 10 FF " +
                        "00 00 0A 08 00 00 00 00 08 78 0E FF 00 00 0A 08 " +
                        "00 00 00 00 08 78 0C FF 00 00 0A 08 00 00 00 00 " +
                        "08 74 D5 FF 00 00 0A 08 00 00 00 00 08 74 D3 FF " +
                        "00 00 0A 08 00 00 00 00 08 74 D0 FF 00 00 0A 08 " +
                        "00 00 00 00 08 74 CE FF 00 00 0A 08 00 00 00 00 " +
                        "08 73 14 FF 00 00 1E 08 00 00 00 00 08 73 10 FF " +
                        "00 00 1E 08 00 00 00 00 08 73 0D FF 00 00 1E 08 " +
                        "00 00 00 "));


        assertWithMessage("non null").that(x).isNotNull();
        assertWithMessage("inner validate").that(x.inner).isEqualTo(inner);
        assertWithMessage("payload len").that(x.payloadLen).isEqualTo(252);
        assertWithMessage("payload seq").that(x.seq).isEqualTo(14);
        assertWithMessage("payload opcode").that(x.opcode).isEqualTo(209);
        assertWithMessage("payload typelen").that(x.typeLen).isEqualTo(3);
        assertWithMessage("payload chk").that(x.chk).isEqualTo(142);

    }
}