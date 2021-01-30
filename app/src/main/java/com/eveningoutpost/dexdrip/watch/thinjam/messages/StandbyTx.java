package com.eveningoutpost.dexdrip.watch.thinjam.messages;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_SWITCH_OFF;

// jamorham

public class StandbyTx extends BaseTx {
    public StandbyTx() {
        init(OPCODE_SWITCH_OFF, 1);
        data.put((byte) 1); // only support type 1 at the moment
    }
}

