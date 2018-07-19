package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_STAT_REQST;

// jamorham

public class StatusTx extends BaseMessage {

    final byte opcode = OPCODE_STAT_REQST; // 0x41
    final int length = 1;

    public StatusTx() {
        init(opcode, length, true);
    }
}

