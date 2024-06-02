package jamorham.keks.message;


import static jamorham.keks.util.Util.getRandomKey;

import java.nio.ByteBuffer;

/**
 * JamOrHam
 */

public class AuthRequestTxMessage2 extends BaseMessage {
    public final byte opcode = 0x02;

    public byte[] singleUseToken;
    private static final byte endByteStd = 0x2;
    private static final byte endByteAlt = 0x1;

    public AuthRequestTxMessage2(int token_size) {
        this(token_size, false, new byte[0]);
    }

    public AuthRequestTxMessage2(int token_size, boolean alt, byte[] chal) {
        this(token_size, (alt ? endByteAlt : endByteStd)
                + (chal.length > 2 ? chal[2] : 0));
    }

    public AuthRequestTxMessage2(int token_size, int slot) {
        init(opcode, token_size + 2);
        final byte[] randomBytes = getRandomKey();
        final ByteBuffer bb = ByteBuffer.allocate(token_size);
        bb.put(randomBytes, 0, token_size);
        singleUseToken = bb.array();
        data.put(singleUseToken);
        data.put((byte) slot);
        byteSequence = data.array();
    }
}
