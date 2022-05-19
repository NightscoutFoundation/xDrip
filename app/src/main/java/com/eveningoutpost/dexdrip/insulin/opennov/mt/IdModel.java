package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.Getter;
import lombok.val;

/**
 * JamOrHam
 * OpenNov Model ID
 */

public class IdModel extends BaseMessage {

    @Getter
    private String model = "";

    @SuppressWarnings("StringConcatenationInLoop")
    public static IdModel parse(final ByteBuffer buffer) {
        val rt = new IdModel();
        while (buffer.hasRemaining()) {
            if (rt.model.length() > 0) rt.model += " ";
            rt.model += getIndexedString(buffer);
        }
        return rt;
    }

    public static IdModel parse(final byte[] buffer) {
        return parse(ByteBuffer.wrap(buffer));
    }
}
