package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.val;

/**
 * JamOrHam
 * OpenNov value maps
 */

public class ValueMap extends BaseMessage {

    @Data
    @AllArgsConstructor
    public static class ValueMapEntry {
        final int type;
        final int tcount;
    }

    private final List<ValueMapEntry> maps = new LinkedList<>();

    public static ValueMap parse(final ByteBuffer buffer) {
        val vmap = new ValueMap();
        val count = getUnsignedShort(buffer);
        val len = getUnsignedShort(buffer);
        for (int i = 0; i < count; i++) {
            val type = getUnsignedShort(buffer);
            val tcount = getUnsignedShort(buffer);

            log("ValueMap: " + type + " (" + tcount + ")");

            vmap.maps.add(new ValueMapEntry(type, tcount));
        }

        return vmap;
    }

    public static ValueMap parse(final byte[] bytes) {
        return parse(ByteBuffer.wrap(bytes));
    }

}
