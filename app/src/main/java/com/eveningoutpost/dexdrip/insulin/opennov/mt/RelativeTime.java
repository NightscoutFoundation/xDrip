package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import static com.eveningoutpost.dexdrip.Models.JoH.tsl;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.Getter;
import lombok.val;

/**
 * JamOrHam
 * OpenNov relative time
 */

public class RelativeTime extends BaseMessage {

    @Getter
    private long relativeTime = -1;

    @Getter
    private long snapshotUtc = -1;

    public static RelativeTime parse(final ByteBuffer buffer) {
        val rt = new RelativeTime();
        rt.snapshotUtc = tsl();
        rt.relativeTime = getUnsignedInt(buffer);
        return rt;
    }

    public static RelativeTime parse(byte[] buffer) {
        return parse(ByteBuffer.wrap(buffer));
    }
}
