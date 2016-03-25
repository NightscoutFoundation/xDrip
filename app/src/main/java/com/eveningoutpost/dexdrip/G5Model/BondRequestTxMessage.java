package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/16/16.
 */
public class BondRequestTxMessage extends TransmitterMessage {
    int opcode = 0x7;

    public BondRequestTxMessage() {
        data = ByteBuffer.allocate(1);
        data.put((byte)opcode);

        byteSequence = data.array();
    }
}

