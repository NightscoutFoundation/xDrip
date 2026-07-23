package com.eveningoutpost.dexdrip.g5model;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthChallengeTxMessage extends TransmitterMessage {
    byte opcode = 0x04;
    byte[] challengeHash;

    public AuthChallengeTxMessage(byte[] challenge) {
        challengeHash = challenge;

        data = ByteBuffer.allocate(9);
        data.put(opcode);
        data.put(challengeHash);

        byteSequence = data.array();
        UserError.Log.d(TAG,"AuthChallengeTX: "+ JoH.bytesToHex(byteSequence));
    }
}
