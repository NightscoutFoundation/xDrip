package com.eveningoutpost.dexdrip.g5model;

// jamorham

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class InvalidRxMessage extends BaseMessage {

    public static final byte opcode = (byte) 0xFF;
    private static final int length = 3;

    InvalidRxMessage(byte[] packet) {

        if ((packet.length == length) && packet[0] == opcode) {
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        }
    }

}
