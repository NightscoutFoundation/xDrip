package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

import com.eveningoutpost.dexdrip.HexTestTools;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import static com.google.common.truth.Truth.assertWithMessage;

// jamorham

public class InboundStreamTest extends RobolectricTestWithConfig {

    @Test
    public void inboundStreamTest() {

        final InboundStream inboundStream = new InboundStream();
        assertWithMessage("not complete at start").that(inboundStream.isComplete()).isFalse();
        assertWithMessage("null at start").that(inboundStream.getByteSequence()).isNull();

        inboundStream.push(HexTestTools.tolerantHexStringToByteArray("25:00:83:41:00:00:ac:00:00:04:64:05:00:00:00:00:00:00:00:00"));
        assertWithMessage("not complete after first push").that(inboundStream.isComplete()).isFalse();
        assertWithMessage("null after first push").that(inboundStream.getByteSequence()).isNull();

        inboundStream.push(HexTestTools.tolerantHexStringToByteArray("00:00:00:00:00:00:00:09:00:00:00:00:00:00:00:00:00:c8:08"));
        assertWithMessage("complete after second push").that(inboundStream.isComplete()).isTrue();
        assertWithMessage("not null after second push").that(inboundStream.getByteSequence()).isNotNull();
        assertWithMessage("stream size 37").that(inboundStream.getByteSequence().length).isEqualTo(37);

        inboundStream.push(HexTestTools.tolerantHexStringToByteArray("00:00:00:00:00:00:00:09:00:00:00:00:00:00:00:00:00:c8:08"));
        assertWithMessage("complete after duplicate second push").that(inboundStream.isComplete()).isTrue();
        assertWithMessage("not null after duplicate second push").that(inboundStream.getByteSequence()).isNotNull();
        assertWithMessage("stream size still 37").that(inboundStream.getByteSequence().length).isEqualTo(37);

        //final byte[] result = inboundStream.getByteSequence();
        //System.out.println(HexDump.dumpHexString(result));

    }

}