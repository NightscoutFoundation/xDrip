package com.eveningoutpost.dexdrip.watch.thinjam.messages;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_BACKFILL_REQ;

// jamorham

public class BackFillTx extends BaseTx {
    public BackFillTx(final int records) {
        init(OPCODE_BACKFILL_REQ, 1);
        data.put((byte) (records & 0xFF));
    }
}
