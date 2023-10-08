package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

// jamorham

public class BaseAuthChallengeTxMessage extends BaseMessage {
    static final byte opcode = 0x04;

    public BaseAuthChallengeTxMessage(final byte[] challenge) {

        init(opcode, 9);
        data.put(challenge);
        byteSequence = data.array();
        UserError.Log.d(TAG, "BaseAuthChallengeTX: " + JoH.bytesToHex(byteSequence));
    }
}
