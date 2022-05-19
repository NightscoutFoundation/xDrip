package com.eveningoutpost.dexdrip.insulin.opennov.mt;


import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.val;

/**
 * JamOrHam
 * OpenNov Data Apdu
 */

@Builder
@AllArgsConstructor
public class DataApdu extends BaseMessage {

    public int olen;
    public int invokeId;
    public int dchoice;
    public int dlen;

    byte[] dataPayload;

    private DataApdu() {
    }

    public byte[] encode() {
        val b = ByteBuffer.allocate((dataPayload != null ? dataPayload.length : 0) + 8);
        putUnsignedShort(b, b.capacity() - 2);
        putUnsignedShort(b, invokeId);
        putUnsignedShort(b, dchoice);
        putIndexedBytes(b, dataPayload);
        return b.array();
    }

    public static DataApdu parse(final ByteBuffer buffer) {
        val a = new DataApdu();
        a.olen = getUnsignedShort(buffer);
        a.invokeId = getUnsignedShort(buffer);
        a.dchoice = getUnsignedShort(buffer);
        a.dlen = getUnsignedShort(buffer);
        return a;
    }

}
