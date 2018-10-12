package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;

/**
 * Created by jamorham on 25/11/2016.
 */

public class VersionRequestTxMessage extends BaseMessage {

    static final byte opcode = 0x4A;

    public VersionRequestTxMessage() {
        init(opcode, 3);
        UserError.Log.e(TAG, "VersionTx dbg: " + JoH.bytesToHex(byteSequence));
    }
}

