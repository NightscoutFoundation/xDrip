package com.eveningoutpost.dexdrip.g5model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// created by jamorham

public class BackFillRxMessage extends BaseMessage {

    public static final int opcode = 0x51;
    private static final int length = 20;

    private boolean valid = false;

    BackFillRxMessage(byte[] packet) {

        if (packet.length == length) {
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            if ((data.get() == opcode) && checkCRC(packet)) {
                valid = true;
                // meh
                // 51 00 01 01 A1A00200 DDAF0200 82000000 2361 1625
            }
        }
    }

    public boolean valid() {
        return valid;
    }
}
