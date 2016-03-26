package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/16/16.
 */
public class KeepAliveTxMessage extends TransmitterMessage {
    int opcode = 0x6;
    int time;

    public KeepAliveTxMessage(int time) {
        this.time = time;

        data = ByteBuffer.allocate(2);
        data.put(new byte[]{ (byte)opcode, (byte)this.time });
        byteSequence = data.order(ByteOrder.LITTLE_ENDIAN).array();
    }
}