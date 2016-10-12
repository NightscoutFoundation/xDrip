package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/16/16.
 */
public class BondRequestTxMessage extends TransmitterMessage {
    byte opcode = 0x7;

    public BondRequestTxMessage() {
        data = ByteBuffer.allocate(1);
        data.put(opcode);

        byteSequence = data.array();
    }
}

