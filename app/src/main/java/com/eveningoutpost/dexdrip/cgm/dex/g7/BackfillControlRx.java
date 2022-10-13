package com.eveningoutpost.dexdrip.cgm.dex.g7;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.Getter;

/**
 * JamOrHam
 */

public class BackfillControlRx extends BaseMessage{

    public static final byte opcode = 0x59;
    @Getter
    private boolean valid;

    public BackfillControlRx(final byte[] packet) {
        data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if ((data.get() == opcode)) {
            valid = true;
            // TODO more to parse here
        }
    }

}
