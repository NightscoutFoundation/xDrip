package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_CONN_REQST;

// jamorham

public class ConnParamTx extends BaseMessage {

    final byte opcode = OPCODE_CONN_REQST; // 0x0a
    final int length = 2;

    public ConnParamTx() {
        init(opcode, length, true);
        data.put((byte) 0x01);
    }
}
