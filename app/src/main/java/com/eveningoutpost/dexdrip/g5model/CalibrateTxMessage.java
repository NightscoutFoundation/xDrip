package com.eveningoutpost.dexdrip.g5model;


// created by jamorham

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

public class CalibrateTxMessage extends BaseMessage {

    final byte opcode = 0x34;
    final int length = 9;

    final int glucose;

    public CalibrateTxMessage(int glucose, int dexTime) {
        init(opcode, length);
        this.glucose = glucose;
        data.putShort((short) glucose);
        data.putInt(dexTime);
        appendCRC();
        UserError.Log.d(TAG, "CalibrateGlucoseTxMessage dbg: " + JoH.bytesToHex(byteSequence));
    }

}
