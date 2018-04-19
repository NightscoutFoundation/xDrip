package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// created by jamorham

public class SessionStartTxMessage extends TransmitterMessage {

    final byte opcode = 0x26;

    public SessionStartTxMessage(int dexTime) {
        this((int) (JoH.tsl() / 1000), dexTime);
    }

    public SessionStartTxMessage(long startTime, int dexTime) {
        data = ByteBuffer.allocate(11);
        data.order(ByteOrder.LITTLE_ENDIAN);
        data.put(opcode);
        data.putInt(dexTime);
        data.putInt((int)(startTime/1000));
        appendCRC();
        UserError.Log.d(TAG, "SessionStartTxMessage dbg: " + JoH.bytesToHex(byteSequence));
    }


}
