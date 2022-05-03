package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.val;

/**
 * JamOrHam
 * OpenNov Confirmed Action
 */

@Builder
@AllArgsConstructor
public class ConfirmedAction extends BaseMessage {

    private static final int ALL_SEGMENTS = 0x0001;

    public int handle;
    public int type;

    public byte[] bytes;

    public ConfirmedAction allSegments() {
        val b = ByteBuffer.allocate(6);
        putUnsignedShort(b, ALL_SEGMENTS);
        putUnsignedShort(b, 2);
        putUnsignedShort(b, 0);
        bytes = b.array();
        return this;
    }

    public ConfirmedAction segment(final int segment) {
        val b = ByteBuffer.allocate(2);
        putUnsignedShort(b, segment);
        bytes = b.array();
        return this;
    }

    public byte[] encode() {
        val b = ByteBuffer.allocate(6 + (bytes != null ? bytes.length : 0));
        putUnsignedShort(b, handle);
        putUnsignedShort(b, type);
        putIndexedBytes(b, bytes != null ? bytes : new byte[0]);
        return b.array();
    }

    public static ConfirmedAction parse(final ByteBuffer buffer) {
        return ConfirmedAction.builder()
                .handle(getUnsignedShort(buffer))
                .type(getUnsignedShort(buffer))
                .bytes(getIndexedBytes(buffer))
                .build();
    }

}
