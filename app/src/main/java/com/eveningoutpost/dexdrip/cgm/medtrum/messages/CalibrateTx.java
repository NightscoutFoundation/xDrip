package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

import com.eveningoutpost.dexdrip.cgm.medtrum.TimeKeeper;

import java.security.InvalidAlgorithmParameterException;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_CALI_REQST;

// jamorham

public class CalibrateTx extends BaseMessage {

    final byte opcode = OPCODE_CALI_REQST; // 0x40
    final int length = 8;

    public CalibrateTx(long serial, long time, int mgdl) throws InvalidAlgorithmParameterException {

        final int seconds_since = TimeKeeper.secondsSinceReferenceTime(serial, time);
        if (seconds_since <= 0) {
            throw new InvalidAlgorithmParameterException("sensor time unknown");
        }

        init(opcode, length, true);
        data.putShort((short) mgdl);
        data.putInt(seconds_since); // time since sensor start in seconds - need lag time?
        data.put((byte) 0x00); // 0 = finger-stick, 1 = other??
    }
}
