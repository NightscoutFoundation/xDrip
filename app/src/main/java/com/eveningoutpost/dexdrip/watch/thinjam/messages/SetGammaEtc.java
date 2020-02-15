package com.eveningoutpost.dexdrip.watch.thinjam.messages;

// jamorham

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_SET_GAMMA;

public class SetGammaEtc extends BaseTx {
    public SetGammaEtc(int value) {
        init(OPCODE_SET_GAMMA, 1);
        data.put((byte) value);
    }
}
