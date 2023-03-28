package jamorham.keks.message;


import java.nio.ByteBuffer;

/**
 * JamOrHam
 */

public class AuthStatusRxMessage extends BaseMessage {
    public static final int opcode = 0x5;
    public int authenticated;
    public int bonded;

    public AuthStatusRxMessage(byte[] packet) {
        if (packet.length >= 3) {
            if (packet[0] == opcode) {
                data = ByteBuffer.wrap(packet);
                authenticated = data.get(1);
                bonded = data.get(2);
            }
        }
    }

    public boolean isAuthenticated() {
        return authenticated == 1;
    }
    public boolean isBonded() {
        return bonded == 1;
    }
    public boolean needsRefresh() {
        return bonded == 3;
    }
}