package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

/**
 * Created by jamorham on 25/11/2016.
 */

public class VersionRequestTxMessage extends BaseMessage {

    static final byte opcode0 = 0x20;
    static final byte opcode1 = 0x4A;
    static final byte opcode2 = 0x52;

    public VersionRequestTxMessage() {
        this(0);
    }

    public VersionRequestTxMessage(final int version) {
        byte this_opcode = 0;
        switch (version) {
            case 0:
                this_opcode = opcode0;
                break;
            case 1:
                this_opcode = opcode1;
                break;
            case 2:
                this_opcode = opcode2;
                break;

        }
        init(this_opcode, 3);
        UserError.Log.d(TAG, "VersionTx (" + version + ") dbg: " + JoH.bytesToHex(byteSequence));
    }
}

