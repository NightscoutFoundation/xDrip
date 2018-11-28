package com.eveningoutpost.dexdrip.Models;

import org.junit.Test;

import static com.eveningoutpost.dexdrip.Models.JoH.validateMacAddress;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.*;

// jamorham

public class JoHTest {

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

}