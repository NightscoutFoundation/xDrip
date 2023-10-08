package com.eveningoutpost.dexdrip.g5model;

// jamorham

public class ExtensionTxMessage extends BaseMessage {
    static final byte opcode = (byte) 0x42;

    public static final int PARAM_ENABLE = 1;
    public static final int PARAM_DISABLE = 2;

    public ExtensionTxMessage(final int param) {
        init(opcode, 4);

        switch (param) {
            case PARAM_ENABLE:
                data.put((byte) 0x80);
                break;
            case PARAM_DISABLE:
                data.put((byte) 0x40);
                break;
            default:
                throw new RuntimeException("Invalid parameter in ExtensionTxMessage");
        }

        appendCRC();
    }
}

