package com.eveningoutpost.dexdrip.insulin.inpen.messages;

// jamorham

public class KeepAliveTx extends BaseTx {

    private static final byte opcode = (byte) 0xF1;

    public KeepAliveTx() {
        init(1);
        data.put(opcode);
    }
}


