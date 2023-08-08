package com.eveningoutpost.dexdrip.g5model;

// jamorham

public class TimeTxMessage extends BaseMessage {
    public static final byte opcode = 0x24;

    TimeTxMessage() {
        init(opcode, 3);
    }
}
