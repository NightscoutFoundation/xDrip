package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

import com.eveningoutpost.dexdrip.cgm.medtrum.Crypt;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_AUTH_REQST;

// jamorham

public class AuthTx extends BaseMessage {

    final byte opcode = OPCODE_AUTH_REQST; // 0x05
    final int length = 10;

    public AuthTx(long serial) {
        init(opcode, length, true);
        data.put((byte) 0x02);
        data.putInt(0);
        data.putInt((int) Crypt.doubleSchrage(serial));
    }

}
