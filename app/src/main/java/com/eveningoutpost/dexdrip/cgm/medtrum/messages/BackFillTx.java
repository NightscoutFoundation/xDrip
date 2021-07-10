package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_BACK_REQST;

// jamorham

public class BackFillTx extends BaseMessage {

    final byte opcode = OPCODE_BACK_REQST; // 0x42
    final int length = 5;

    public BackFillTx(int tickStart, int tickEnd) {
        init(opcode, length, true);
        data.putShort((short) tickStart);
        data.putShort((short) ((tickEnd - tickStart) + 1));

    }
}
