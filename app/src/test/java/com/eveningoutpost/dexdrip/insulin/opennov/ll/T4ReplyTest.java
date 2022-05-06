package com.eveningoutpost.dexdrip.insulin.opennov.ll;


import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.HexTestTools;

import org.junit.Test;

import lombok.val;

public class T4ReplyTest extends HexTestTools {

    @Test
    public void parseTest1() {
        assertWithMessage("null in null out").that(T4Reply.parse(null)).isNull();

        val b1 = tolerantHexStringToByteArray("0090");
        assertWithMessage("invalid short not null").that(T4Reply.parse(b1)).isNotNull();
        assertWithMessage("invalid short fails check").that(T4Reply.parse(b1).isOkay()).isFalse();
        assertWithMessage("invalid short is error").that(T4Reply.parse(b1).asInteger()).isEqualTo(-1);

        val b2 = tolerantHexStringToByteArray("12340090");
        assertWithMessage("valid short invalid xfer not null").that(T4Reply.parse(b2)).isNotNull();
        assertWithMessage("valid short invalid xfer fails check").that(T4Reply.parse(b2).isOkay()).isFalse();
        assertWithMessage("valid short invalid xfer is error").that(T4Reply.parse(b2).asInteger()).isEqualTo(0x1234);

        val b3 = tolerantHexStringToByteArray("45679000");
        assertWithMessage("valid short valid xfer not null").that(T4Reply.parse(b3)).isNotNull();
        assertWithMessage("valid short valid xfer passes check").that(T4Reply.parse(b3).isOkay()).isTrue();
        assertWithMessage("valid short valid xfer is error").that(T4Reply.parse(b3).asInteger()).isEqualTo(0x4567);
    }

    @Test
    public void parseTest2() {
        val b1 = tolerantHexStringToByteArray("012345679000");
        val b2 = tolerantHexStringToByteArray("890123459000");

        val r = T4Reply.parse(b1);
        assertWithMessage("valid bytes not null").that(r).isNotNull();
        assertWithMessage("valid bytes okay").that(r.isOkay()).isTrue();
        assertWithMessage("valid bytes match").that(r.bytes).isEqualTo(tolerantHexStringToByteArray("01234567"));

        val r2 = T4Reply.parse(b2, r);
        assertWithMessage("valid bytes 2 not null").that(r2).isNotNull();
        assertWithMessage("valid bytes 2 okay").that(r2.isOkay()).isTrue();
        assertWithMessage("valid bytes 2 match").that(r2.bytes).isEqualTo(tolerantHexStringToByteArray("0123456789012345"));
    }
}