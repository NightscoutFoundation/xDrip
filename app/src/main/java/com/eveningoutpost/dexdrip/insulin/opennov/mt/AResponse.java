package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import static com.eveningoutpost.dexdrip.models.JoH.cloneObject;
import static com.eveningoutpost.dexdrip.insulin.opennov.mt.ApoepElement.APOEP;
import static com.eveningoutpost.dexdrip.insulin.opennov.mt.ApoepElement.SYS_TYPE_MANAGER;

import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.AllArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 * OpenNov A Response
 */

@AllArgsConstructor
public class AResponse extends BaseMessage {

    int result;
    int protocol;
    ApoepElement apoep;

    public AResponse() {
    }

    public AResponse(final ARequest request) {
        this.result = 3;
        this.protocol = APOEP;
        this.apoep = (ApoepElement) cloneObject(request.getApoep());
    }

    public byte[] encode() {
        apoep.recMode = 0;
        apoep.configId = 0;
        apoep.systemType = SYS_TYPE_MANAGER;
        apoep.olistCount = 0;
        apoep.olistLen = 0;

        val a = apoep.encode();
        val b = ByteBuffer.allocate(6 + a.length);
        putUnsignedShort(b, result);
        putUnsignedShort(b, protocol);
        putIndexedBytes(b, a);
        return b.array();
    }

    public static AResponse parse(final ByteBuffer buffer) {
        val a = new AResponse();
        a.result = getUnsignedShort(buffer);
        a.protocol = getUnsignedShort(buffer);
        if (a.protocol == APOEP) {
            a.apoep = ApoepElement.parse(getIndexedBytes(buffer));
            if (d) log(a.apoep.toJson());
        }
        return a;
    }

    public static AResponse parse(final byte[] bytes) {
        return parse(ByteBuffer.wrap(bytes));
    }
}
