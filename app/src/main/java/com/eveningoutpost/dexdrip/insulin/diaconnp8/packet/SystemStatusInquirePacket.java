package com.eveningoutpost.dexdrip.insulin.diaconnp8.packet;

import java.nio.ByteBuffer;

public class SystemStatusInquirePacket extends DiaconnP8Packet {

    static byte MSG_TYPE = 0x55;

    public byte[] encode(int msgSeq) {
        ByteBuffer buffer = prefixEncode(MSG_TYPE, msgSeq, MSG_CON_END);
        return suffixEncode(buffer);
    }
}
