package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.Getter;

// created by jamorham

public class SessionStartTxMessage extends BaseMessage {

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
        final boolean using_g6 = (code != null);
        data = ByteBuffer.allocate(code == null || new G6CalibrationParameters(code).isNullCode() ? (using_g6 ? 13 : 11) : 17);
        data.order(ByteOrder.LITTLE_ENDIAN);
        data.put(opcode);
        data.putInt(dexTime);
        data.putInt((int) (startTime / 1000));

        if (code != null) {
            final G6CalibrationParameters params = new G6CalibrationParameters(code);
            if (params.isValid() && !params.isNullCode()) {
                data.putShort((short) params.getParamA());
                data.putShort((short) params.getParamB());
            } else {
                if (!params.isValid()) {
                    throw new IllegalArgumentException("Invalid G6 code in SessionStartTxMessage");
                }
            }
        }
        if (using_g6) {
            data.putShort((short) 0x0000);
        }
        appendCRC();
        UserError.Log.d(TAG, "SessionStartTxMessage dbg: " + JoH.bytesToHex(byteSequence));
    }

}
