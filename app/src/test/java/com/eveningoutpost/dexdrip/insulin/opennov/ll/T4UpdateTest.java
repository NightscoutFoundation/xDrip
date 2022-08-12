package com.eveningoutpost.dexdrip.insulin.opennov.ll;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.HexTestTools;
import com.eveningoutpost.dexdrip.Models.JoH;

import org.junit.Test;

import lombok.val;

public class T4UpdateTest extends HexTestTools {

    private final boolean D = false;

    @Test
    public void encodeForMtuTest1() {

        val bytes = new byte[255];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }

        val list = T4Update.builder().bytes(bytes).build().encodeForMtu(80);

        if (D) {
            for (val x : list) {
                System.out.println(JoH.bytesToHex(x));
            }
        }
        val p1 = tolerantHexStringToByteArray(
                "00D600004B0000000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F2021222324" +
                        "25262728292A2B2C2D2E2F303132333435363738393A3B3C3D3E3F404142434445464748");
        val p2 = tolerantHexStringToByteArray(
                "00D6004B49494A4B4C4D4E4F505152535455565758595A5B5C5D5E5F606162636465666768696A6B" +
                        "6C6D6E6F707172737475767778797A7B7C7D7E7F808182838485868788898A8B8C8D8E8F9091");
        val p3 = tolerantHexStringToByteArray(
                "00D600944992939495969798999A9B9C9D9E9FA0A1A2" +
                        "A3A4A5A6A7A8A9AAABACADAEAFB0B1B2B3B4B5B6B7B8B9BABBBCBDBEBFC0C1" +
                        "C2C3C4C5C6C7C8C9CACBCCCDCECFD0D1D2D3D4D5D6D7D8D9DA");
        val p4 = tolerantHexStringToByteArray(
                "00D600DD24DBDCDDDEDFE0E1E2E3E4E5E6E7E8E9EA" +
                        "EBECEDEEEFF0F1F2F3F4F5F6F7F8F9FAFBFCFDFE");
        val p5 = tolerantHexStringToByteArray("00D600000200FF");

        assertWithMessage("fragmented size").that(list.size()).isEqualTo(5);
        assertWithMessage("fragmented packet 0 matches").that(list.get(0)).isEqualTo(p1);
        assertWithMessage("fragmented packet 1 matches").that(list.get(1)).isEqualTo(p2);
        assertWithMessage("fragmented packet 2 matches").that(list.get(2)).isEqualTo(p3);
        assertWithMessage("fragmented packet 3 matches").that(list.get(3)).isEqualTo(p4);
        assertWithMessage("fragmented packet 4 matches").that(list.get(4)).isEqualTo(p5);
    }

    @Test
    public void encodeForMtuTest2() {

        val bytes = new byte[200];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }

        val list = T4Update.builder().bytes(bytes).build().encodeForMtu(255);

        if (D) {
            for (val x : list) {
                System.out.println(JoH.bytesToHex(x));
            }
        }
        val p1 = tolerantHexStringToByteArray(
                "00D60000CA00C8000102030405060708090A0B0C0D0E0F101112131415161718191A1B1" +
                        "C1D1E1F202122232425262728292A2B2C2D2E2F303132333435363738393A3B3C3" +
                        "D3E3F404142434445464748494A4B4C4D4E4F505152535455565758595A5B5C5D5E" +
                        "5F606162636465666768696A6B6C6D6E6F707172737475767778797A7B7C7D7E7F8" +
                        "08182838485868788898A8B8C8D8E8F909192939495969798999A9B9C9D9E9FA0A1" +
                        "A2A3A4A5A6A7A8A9AAABACADAEAFB0B1B2B3B4B5B6B7B8B9BABBBCBDBEBFC0C1C2C" +
                        "3C4C5C6C7");

        assertWithMessage("non fragment size").that(list.size()).isEqualTo(1);
        assertWithMessage("non fragment packet 0 matches").that(list.get(0)).isEqualTo(p1);
    }
}
