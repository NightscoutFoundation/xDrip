package jamorham.keks.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * JamOrHam
 */

public class BaseMessage {

    public ByteBuffer data;
    public volatile byte[] byteSequence;

    protected void init(final byte opcode, final int length) {
        data = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        data.put(opcode);
        if (length == 1) {
            getByteSequence();
        }
    }
    public byte[] getByteSequence() {
        return byteSequence = data.array();
    }

}
