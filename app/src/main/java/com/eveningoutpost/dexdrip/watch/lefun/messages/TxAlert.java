package com.eveningoutpost.dexdrip.watch.lefun.messages;

// jamorham

import java.io.UnsupportedEncodingException;

public class TxAlert extends BaseTx {

    private static final byte opcode = 0x17;
    public static final byte ICON_CHAT = 0x08;
    public static final byte ICON_CALL = 0x01;

    public TxAlert(final String msg) {
        this(msg, ICON_CHAT);
    }

    public TxAlert(final String msg, final byte icon) {

        byte[] messageBytes = new byte[1];
        try {
            messageBytes = msg.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //
        }

        init(4 + messageBytes.length);

        data.put(opcode);
        data.put(icon); // icon
        data.put((byte) 0x01); // piece
        data.put((byte) 0x01); // total
        data.put(messageBytes);

    }

}
