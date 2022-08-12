package com.eveningoutpost.dexdrip.insulin.opennov.mt;


import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.val;

/**
 * JamOrHam
 * OpenNov implementation
 */

@Builder
@AllArgsConstructor
public class TrigSegmDataXfer extends BaseMessage {

    int infoLength;
    int segmentId;
    int responseCode;

    public TrigSegmDataXfer() {
    }

    public boolean isOkay() {
        return segmentId != 0 && responseCode == 0;
    }

    public byte[] encode() {
        val b = ByteBuffer.allocate(4);
        putUnsignedShort(b, segmentId);
        putUnsignedShort(b, responseCode);
        return b.array();
    }

    public static TrigSegmDataXfer parse(final ByteBuffer buffer) {

        val x = new TrigSegmDataXfer();
        x.segmentId = getUnsignedShort(buffer);
        x.responseCode = getUnsignedShort(buffer);

        if (d)
            log("TrigSeg: seg: " + x.segmentId + " response: " + x.responseCode + " ok: " + x.isOkay());
        return x;
    }

}
