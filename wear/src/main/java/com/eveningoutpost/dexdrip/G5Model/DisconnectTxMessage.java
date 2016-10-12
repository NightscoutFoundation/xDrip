package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/16/16.
 */
public class DisconnectTxMessage extends TransmitterMessage {
    byte opcode = 0x09;

    public DisconnectTxMessage() {
        data = ByteBuffer.allocate(1);
        data.put(opcode);

        byteSequence = data.array();
    }
}

