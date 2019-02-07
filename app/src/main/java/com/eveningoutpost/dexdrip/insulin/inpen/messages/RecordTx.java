package com.eveningoutpost.dexdrip.insulin.inpen.messages;

// jamorham

public class RecordTx extends BaseTx {

    private static final byte opcode = (byte) 0xC7;

    public RecordTx(final int start, final int end) {
        init(4);
        data.putShort((short) start);
        data.putShort((short) end);
    }

    private byte[] twoBytesAt(final int slice) {
        final byte[] bytes = new byte[2];
        System.arraycopy(getBytes(), slice, bytes, 0, 2);
        return bytes;
    }

    public byte[] startBytes() {
        return twoBytesAt(0);
    }

    public byte[] endBytes() {
        return twoBytesAt(2);
    }

    public byte[] triggerBytes() {
        return new byte[]{opcode};
    }
}
