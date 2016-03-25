package com.eveningoutpost.dexdrip.G5Model;
import com.eveningoutpost.dexdrip.G5Model.TransmitterMessage;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthChallengeTxMessage extends TransmitterMessage {
    int opcode = 0x4;
    byte[] challengeHash;

    public AuthChallengeTxMessage(byte[] challenge) {
        challengeHash = challenge;

        data = ByteBuffer.allocate(17);
        data.put((byte)opcode);
        data.put(challengeHash);

        byteSequence = data.array();
    }
}
