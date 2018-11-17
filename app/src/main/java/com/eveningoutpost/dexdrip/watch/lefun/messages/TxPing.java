package com.eveningoutpost.dexdrip.watch.lefun.messages;

// jamorham

public class TxPing extends BaseTx {

    final byte opcode = 0x00;

    public TxPing() {

        init(1);

        data.put(opcode);

    }

}
