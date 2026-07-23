package com.eveningoutpost.dexdrip.g5model;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.Getter;

/**
 * JamOrHam
 */

public class BackFillControlRxMessage extends BaseMessage {

    public static final byte opcode = 0x59;
    @Getter
    private boolean valid;

    public BackFillControlRxMessage(final byte[] packet) {
        data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if ((data.get() == opcode)) {
            valid = true;
            // TODO more to parse here
        }
    }

}
