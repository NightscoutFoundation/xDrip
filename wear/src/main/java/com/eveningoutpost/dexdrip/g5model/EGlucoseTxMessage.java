package com.eveningoutpost.dexdrip.g5model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// created by jamorham


public class EGlucoseTxMessage extends BaseMessage {

    final byte opcode = 0x4e;

    public EGlucoseTxMessage() {
        data = ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN);
        data.put(opcode);
        appendCRC();
    }

}
