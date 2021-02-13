package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.HexTestTools;

import org.junit.Test;

import static com.eveningoutpost.dexdrip.Models.JoH.validateMacAddress;
import static com.google.common.truth.Truth.assertWithMessage;

// jamorham

public class JoHTest extends HexTestTools {

    @Test
    public void validateMacAddressTest() {
        assertWithMessage("pass 1").that(validateMacAddress("00:11:22:33:44:55")).isTrue();
        assertWithMessage("pass 2").that(validateMacAddress("06:18:A2:33:3F:91")).isTrue();
        assertWithMessage("pass 3").that(validateMacAddress("AB:CD:EF:12:34:CC")).isTrue();
        assertWithMessage("pass 4").that(validateMacAddress("ab:cd:ef:12:34:cc")).isTrue();
        assertWithMessage("pass 5").that(validateMacAddress("ab:AA:Ef:12:54:cD")).isTrue();

        assertWithMessage("fail 1").that(validateMacAddress("00:11:22:33:44:55:66")).isFalse();
        assertWithMessage("fail 2").that(validateMacAddress("00:11:22:33:44")).isFalse();
        assertWithMessage("fail 3").that(validateMacAddress("00:11:22:33:44:55:")).isFalse();
        assertWithMessage("fail 4").that(validateMacAddress("00:11:22:33:44:")).isFalse();
        assertWithMessage("fail 5").that(validateMacAddress("00:11:22:33:44")).isFalse();
        assertWithMessage("fail 6").that(validateMacAddress("Hello World")).isFalse();
        assertWithMessage("fail 7").that(validateMacAddress("")).isFalse();
        assertWithMessage("fail 8").that(validateMacAddress(null)).isFalse();
        assertWithMessage("fail 9").that(validateMacAddress("ab:AA:Ef:12:54:cG")).isFalse();

    }

    @Test
    public void bytesToHexMacFormatTest() {
        final byte[] test1 = hexStringToByteArray("001122334455");
        final byte[] test2 = hexStringToByteArray("001AF2B3EDC5");
        final byte[] test3 = hexStringToByteArray("A3");
        assertWithMessage("test1").that(JoH.bytesToHexMacFormat(test1)).isEqualTo("00:11:22:33:44:55");
        assertWithMessage("test2").that(JoH.bytesToHexMacFormat(test2)).isEqualTo("00:1A:F2:B3:ED:C5");
        assertWithMessage("test3").that(JoH.bytesToHexMacFormat(test3)).isEqualTo("A3");
        assertWithMessage("null value").that(JoH.bytesToHexMacFormat(null)).isEqualTo("NoMac");
        assertWithMessage("empty value").that(JoH.bytesToHexMacFormat(new byte[0])).isEqualTo("NoMac");
    }


    @Test
    public void macFormatTest() {
        assertWithMessage("test1").that(JoH.macFormat("001122334455")).isEqualTo("00:11:22:33:44:55");
        assertWithMessage("test2").that(JoH.macFormat("001A:F2B3EDC5")).isEqualTo("00:1A:F2:B3:ED:C5");
        assertWithMessage("test too short").that(JoH.macFormat("A3")).isNull();
        assertWithMessage("test null").that(JoH.macFormat(null)).isNull();
    }

}