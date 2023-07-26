package jamorham.keks.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.Getter;

/**
 * JamOrHam
 */

public class CertInfoRxMessage extends BaseMessage {

    public static final byte opcode = 0x0b;

    @Getter
    private int size = -1;
    @Getter
    private int which = -1;
    @Getter
    private int state = 0;

    public boolean valid() {
        return (size > 0 && state == 0 && which >= 0);
    }

    public CertInfoRxMessage(final byte[] packet) {
        data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);;
        if (packet.length == 7) {
            if (data.get() == opcode) {
                state = data.get();
                which = data.get();
                size = data.getShort(); // might be an int but just ignore later bytes
            }
        }
    }
}
