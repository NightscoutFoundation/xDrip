package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.val;

/**
 * JamOrHam
 * OpenNov Arguments Simple
 */

@Builder
@AllArgsConstructor
public class ArgumentsSimple extends BaseMessage {

    private final List<Integer> attributes = new LinkedList<>();

    @Builder.Default
    int handle = -1;

    public byte[] encode() {
        val b = ByteBuffer.allocate(6);
        putUnsignedShort(b, handle);
        putUnsignedShort(b, attributes.size());
        putUnsignedShort(b, attributes.size());
        return b.array();
    }
}
