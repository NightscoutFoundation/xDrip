package com.eveningoutpost.dexdrip.watch.lefun.messages;

// jamorham

import java.io.UnsupportedEncodingException;

public class TxAlert extends BaseTx {

    private static final byte opcode = 0x17;

    public TxAlert(final String msg) {

        byte[] messageBytes = new byte[1];
        try {
            messageBytes = msg.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //
        }

        init(4 + messageBytes.length);

        data.put(opcode);
        data.put((byte) 0x08); // icon
        data.put((byte) 0x01); // piece
        data.put((byte) 0x01); // total
        data.put(messageBytes);

    }

}
