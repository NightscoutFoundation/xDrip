package com.eveningoutpost.dexdrip.insulin.inpen.messages;

import java.nio.ByteBuffer;

import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.COUNTER_START;

// jamorham

class ChalTx extends BaseTx {

    private static final byte opcode = (byte) 0x99;

    ChalTx(final byte[] param) {
        init(16);
        final byte[] bytes = new byte[]{opcode, 0, 0, opcode};
        System.arraycopy(param, 1, bytes, 0, 3);
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        data.putLong(HEADER_MAGIC);
        data.putInt(COUNTER_START + getCounterId());
        data.putInt(byteBuffer.getInt());
    }

}
