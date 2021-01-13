package com.eveningoutpost.dexdrip.watch.thinjam.messages;


import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_RESET_PERSIST;

// jamorham

public class ResetPersistTx extends BaseTx {

    public ResetPersistTx(boolean sleep) {
        if (sleep) {
            init(OPCODE_RESET_PERSIST, 1);
        } else {
            init(OPCODE_RESET_PERSIST, 0);
        }
    }

}
