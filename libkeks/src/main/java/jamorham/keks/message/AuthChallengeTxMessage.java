package jamorham.keks.message;

import java.nio.ByteBuffer;

/**
 * JamOrHam
 */

public class AuthChallengeTxMessage extends BaseMessage {
    byte opcode = 0x04;
    byte[] challengeHash;

    public AuthChallengeTxMessage(byte[] challenge) {
        challengeHash = challenge;
        data = ByteBuffer.allocate(9);
        data.put(opcode);
        data.put(challengeHash);
        byteSequence = data.array();
    }
}