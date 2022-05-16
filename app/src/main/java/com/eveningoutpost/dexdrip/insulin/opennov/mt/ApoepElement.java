package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.AllArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 * OpenNov APOEP Element
 */

@AllArgsConstructor
public class ApoepElement extends BaseMessage {

    public static final int APOEP = 20601;
    public static final int SYS_TYPE_MANAGER = 0x80000000;
    public static final int SYS_TYPE_AGENT = 0x00800000;

    public long version = -1;
    public int encoding = -1;
    public long nomenclature = -1;
    public long functional = -1;
    public long systemType = -1;
    public byte[] systemId = null;
    public int configId = -1;
    public long recMode = -1;
    public int olistCount = -1;
    public int olistLen = -1;

    public ApoepElement() {
    }

    public byte[] encode() {
        val b = ByteBuffer.allocate(30 + systemId.length);
        putUnsignedInt(b, version);
        putUnsignedShort(b, encoding);
        putUnsignedInt(b, nomenclature);
        putUnsignedInt(b, functional);
        putUnsignedInt(b, systemType);
        putIndexedBytes(b, systemId);
        putUnsignedShort(b, configId);
        putUnsignedInt(b, recMode);
        putUnsignedShort(b, olistCount);
        putUnsignedShort(b, olistLen);
        return b.array();
    }

    public static ApoepElement parse(final ByteBuffer buffer) {
        val r = new ApoepElement();
        r.version = getUnsignedInt(buffer);
        r.encoding = getUnsignedShort(buffer);
        r.nomenclature = getUnsignedInt(buffer);
        r.functional = getUnsignedInt(buffer);
        r.systemType = getUnsignedInt(buffer);
        r.systemId = getIndexedBytes(buffer);
        r.configId = getUnsignedShort(buffer);
        r.recMode = getUnsignedInt(buffer);
        r.olistCount = getUnsignedShort(buffer);
        r.olistLen = getUnsignedShort(buffer);
        return r;
    }

    public static ApoepElement parse(final byte[] bytes) {
        return parse(ByteBuffer.wrap(bytes));
    }
}
