package com.eveningoutpost.dexdrip.insulin.diaconnp8.packet;

import java.nio.ByteBuffer;

public class BigLogInquirePacket extends DiaconnP8Packet {

    static byte MSG_TYPE = 0x5D;
    final int _startLogNum;
    final int _endLogNum;
    final int _delayTime;

    public BigLogInquirePacket(int startLogNum, int endLogNum,int delayTime) {
        this._startLogNum = startLogNum;
        this._endLogNum = endLogNum;
        this._delayTime = delayTime;

    }
    int getStartLogNum() {
        return _startLogNum;
    }

    int getEndLogNum() {
        return _endLogNum;
    }

    int getDelayTime() {
        return _delayTime;
    }
    public byte[] encode(int msgSeq) {
        ByteBuffer buffer = prefixEncode(MSG_TYPE, msgSeq, MSG_CON_END);
        buffer.putShort((short)getStartLogNum());
        buffer.putShort((short)getEndLogNum());
        buffer.put((byte)getDelayTime());
        return suffixEncode(buffer);
    }
}
