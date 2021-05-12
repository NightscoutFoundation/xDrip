package com.eveningoutpost.dexdrip.G5Model;

// jamorham

public class TimeTxMessage extends BaseMessage {
    public static final byte opcode = 0x24;

    TimeTxMessage() {
        init(opcode, 3);
    }
}
