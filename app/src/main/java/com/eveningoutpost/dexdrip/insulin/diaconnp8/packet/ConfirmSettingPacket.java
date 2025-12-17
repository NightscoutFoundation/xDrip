package com.eveningoutpost.dexdrip.insulin.diaconnp8.packet;

import java.nio.ByteBuffer;

public class ConfirmSettingPacket extends DiaconnP8Packet {

    public static final byte MSG_TYPE = 0x1F;

    private final byte _msgType;
    private final int _otp;

    public ConfirmSettingPacket(byte msgType, int otp) {
        this._msgType = msgType;
        this._otp = otp;
    }

    public byte getMsgType() {
        return _msgType;
    }

    public int getOtp() {
        return _otp;
    }

    public byte[] encode(int msgSeq) {
        ByteBuffer buffer = prefixEncode(MSG_TYPE, msgSeq, MSG_CON_END);
        buffer.put(getMsgType());
        buffer.putInt(getOtp());
        return suffixEncode(buffer);
    }
}
