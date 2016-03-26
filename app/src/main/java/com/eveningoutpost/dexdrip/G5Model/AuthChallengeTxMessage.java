package com.eveningoutpost.dexdrip.G5Model;
import com.eveningoutpost.dexdrip.G5Model.TransmitterMessage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthChallengeTxMessage extends TransmitterMessage {
    byte opcode = 0x4;
    byte[] challengeHash;

    public AuthChallengeTxMessage(byte[] challenge) {
        challengeHash = challenge;
        if (challengeHash[0] < 0) {
            for (int i = 0; i < challengeHash.length / 2; i++) {
                byte temp = challenge[i];
                challengeHash[i] = challenge[challengeHash.length - 1 - i];
                challengeHash[challengeHash.length - 1 - i] = temp;
            }
        }

        data = ByteBuffer.allocate(9);
        data.put(opcode);
        data.put(challengeHash);

        byteSequence = data.array();
    }
}
