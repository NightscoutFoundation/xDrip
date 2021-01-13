package com.eveningoutpost.dexdrip.G5Model;


import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.usererror.UserErrorLog;

// jamorham

public class BaseAuthChallengeTxMessage extends BaseMessage {
    static final byte opcode = 0x04;

    public BaseAuthChallengeTxMessage(final byte[] challenge) {

        init(opcode, 9);
        data.put(challenge);
        byteSequence = data.array();
        UserErrorLog.d(TAG, "BaseAuthChallengeTX: " + JoH.bytesToHex(byteSequence));
    }
}
