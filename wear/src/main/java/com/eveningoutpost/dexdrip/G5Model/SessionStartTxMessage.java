package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.Getter;

// created by jamorham

public class SessionStartTxMessage extends TransmitterMessage {

    final byte opcode = 0x26;
    @Getter
    private final long startTime;
    @Getter
    private final int dexTime;

    public SessionStartTxMessage(int dexTime) {
        this((int) (JoH.tsl() / 1000), dexTime);
    }

    public SessionStartTxMessage(long startTime, int dexTime) {
        this(startTime, dexTime, null);
    }

    public SessionStartTxMessage(long startTime, int dexTime, String code) {
        this.startTime = startTime;
        this.dexTime = dexTime;
        data = ByteBuffer.allocate(code == null ? 11 : 15);
        data.order(ByteOrder.LITTLE_ENDIAN);
        data.put(opcode);
        data.putInt(dexTime);
        data.putInt((int) (startTime / 1000));

        if (code != null) {
            final G6CalibrationParameters params = new G6CalibrationParameters(code);
            if (params.isValid()) {
                data.putShort((short) params.getParamA());
                data.putShort((short) params.getParamB());
            } else {
                throw new IllegalArgumentException("Invalid G6 code in SessionStartTxMessage");
            }
        }
        appendCRC();
        UserError.Log.d(TAG, "SessionStartTxMessage dbg: " + JoH.bytesToHex(byteSequence));
    }

}
