package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.val;

/**
 * JamOrHam
 * OpenNov Event Request
 */

@Builder
@AllArgsConstructor
public class EventRequest extends BaseMessage {

    private static final int FIRST = 0x80;
    private static final int MIDDLE = 0x00;
    private static final int LAST = 0x40;
    private static final int MARK = 0x80;

    public int handle;
    public long currentTime;
    public int type;
    public int replyLen;
    @Builder.Default
    public int reportId = -1;
    @Builder.Default
    public int reportResult = -1;
    @Builder.Default
    public int instance = -1;
    @Builder.Default
    public int index = -1;
    @Builder.Default
    public int count = -1;
    @Builder.Default
    public int block = -1;
    @Builder.Default
    public boolean confirmed = false;

    public EventRequest() {
    }

    public byte[] encode() {
        val b = ByteBuffer.allocate(10 + replyLen);
        putUnsignedShort(b, handle);
        putUnsignedInt(b, currentTime);
        putUnsignedShort(b, type);
        putUnsignedShort(b, replyLen);
        if (replyLen == 4) {
            putUnsignedShort(b, reportId);
            putUnsignedShort(b, reportResult);
        } else if (replyLen == 12) {
            putUnsignedShort(b, instance);
            putUnsignedShort(b, 0);
            putUnsignedShort(b, index);
            putUnsignedShort(b, 0);
            putUnsignedShort(b, count);
            putUnsignedByte(b, block);
            putUnsignedByte(b, confirmed ? MARK : 0);
        } else if (replyLen != 0) {
            error("Unsupported replyLen: " + replyLen);
            return null;
        }
        return b.array();
    }

    public EventRequest firstBlock() {
        block = FIRST;
        return this;
    }

    public EventRequest middleBlock() {
        block = MIDDLE;
        return this;
    }

    public EventRequest lastBlock() {
        block = LAST;
        return this;
    }

    public static EventRequest parse(final ByteBuffer buffer) {
        val e = new EventRequest();
        e.handle = getUnsignedShort(buffer);
        e.currentTime = getUnsignedInt(buffer);
        e.type = getUnsignedShort(buffer);
        e.replyLen = getUnsignedShort(buffer);

        if (e.replyLen == 4) {
            e.reportId = getUnsignedShort(buffer);
            e.reportResult = getUnsignedShort(buffer);
        }
        return e;
    }

}
