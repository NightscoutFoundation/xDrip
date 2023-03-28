package jamorham.keks;

import static java.util.Arrays.fill;
import static jamorham.keks.Config.Get.PREFIX;
import static jamorham.keks.util.Util.arrayAppend;

import org.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import jamorham.keks.util.Log;

/**
 * JamOrHam
 * <p>
 * KEKS context
 */

public class Context {

    public KeyPair keyA;
    public KeyPair KeyB;
    public String password;
    public byte[] passwordBytes;
    public byte[] alice;
    public byte[] bob;
    public byte[] challenge;
    public volatile byte[] savedKey;
    public volatile Packet[] packet = new Packet[4];

    public void reset() {
        savedKey = null;
        fill(packet, null);
    }

    public void resetIfNotReady() {
        if (savedKey == null && getRound3Packet() == null) {
            reset();
        }
    }

    public byte[] getPasswordBytes() {
        if (password == null) {
            Log.l("Context password not set");
            throw new RuntimeException();
        }
        if (passwordBytes == null) {
            passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            if (password.length() == 6) {
                passwordBytes = arrayAppend(PREFIX.bytes, passwordBytes);
            }
        }
        return passwordBytes;
    }

    public BigInteger getPasswordBigInteger() {
        return BigIntegers.fromUnsignedByteArray(getPasswordBytes());
    }

    public Packet getRound1Packet() {
        return packet[1];
    }

    public Packet getRound2Packet() {
        return packet[2];
    }

    public Packet getRound3Packet() {
        return packet[3];
    }

}
