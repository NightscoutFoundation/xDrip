package jamorham.keks.message;

import jamorham.keks.Plugin;
import lombok.val;

/**
 * JamOrHam
 */

public class CertInfoTxMessage extends BaseMessage {

    public static final byte opcode = 0x0b;

    public static byte[] expectMyCert(final Plugin plugin, final int which) {
        val p = new CertInfoTxMessage();
        val c = plugin.getContext();
        p.init(opcode, 6);
        p.data.put((byte) which);
        p.data.putInt(which == 0 ? c.getPartA().length : c.getPartB().length);
        return p.getByteSequence();
    }

    public static byte[] expectMyCert1(final Plugin plugin) {
        return expectMyCert(plugin, 0);
    }

    public static byte[] expectMyCert2(final Plugin plugin) {
        return expectMyCert(plugin, 1);
    }
}