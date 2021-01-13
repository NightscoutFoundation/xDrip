package com.eveningoutpost.dexdrip.watch.thinjam.messages;


import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_SET_GAMMA;

// jamorham

public class SetGammaEtc extends BaseTx {
    public SetGammaEtc(int value) {
        init(OPCODE_SET_GAMMA, 1);
        data.put((byte) value);
    }
}
