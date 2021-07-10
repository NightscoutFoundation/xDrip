package com.eveningoutpost.dexdrip.watch.lefun.messages;

// jamorham

public class TxShakeDetect extends BaseTx {

    final byte opcode = 0x0D;

    public TxShakeDetect(final boolean enabled) {

        init(2);

        data.put(opcode);
        data.put(enabled ? (byte)0x01 : (byte)0x00);
    }

}
