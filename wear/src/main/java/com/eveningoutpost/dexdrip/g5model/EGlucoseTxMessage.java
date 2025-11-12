package com.eveningoutpost.dexdrip.g5model;

/**
 * JamOrHam
 */


public class EGlucoseTxMessage extends BaseMessage {

    final byte opcode = 0x4e;

    public EGlucoseTxMessage(final boolean small) {
        init(opcode, small ? 1 : 3);
    }

    public EGlucoseTxMessage() {
        this(false);
    }

}
