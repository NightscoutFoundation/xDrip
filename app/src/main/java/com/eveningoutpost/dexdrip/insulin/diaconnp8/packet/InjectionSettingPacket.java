package com.eveningoutpost.dexdrip.insulin.diaconnp8.packet;

import java.nio.ByteBuffer;

public class InjectionSettingPacket extends DiaconnP8Packet {

    public static final byte MSG_TYPE = 0x05;

    private final int _setAmt;

    public InjectionSettingPacket(int setAmt) {
        this._setAmt = setAmt;
    }

    public int getSetAmt() {
        return _setAmt;
    }

    public byte[] encode(int msgSeq) {
        ByteBuffer buffer = prefixEncode(MSG_TYPE, msgSeq, MSG_CON_END);
        buffer.putShort((short) getSetAmt());
        return suffixEncode(buffer);
    }
}
