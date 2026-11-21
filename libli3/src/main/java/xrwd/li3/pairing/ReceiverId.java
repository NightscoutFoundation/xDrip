package xrwd.li3.pairing;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import xrwd.li3.util.Hex;

/**
 * The 32-bit receiver ID a Li3 sensor is activated with. The sensor only accepts a switch-receiver command
 * carrying the ID it was started with, so a takeover needs the same fold the starting app used.
 * Ported from RoundWhiteDiscKit {@code Pairing/*ReceiverID.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class ReceiverId {

    /** Which app's derivation turns an account ID into a receiver ID. */
    public enum Fold {
        /** The first of the two official apps: multiply-xor over UTF-16 code units. */
        LI3_APP,
        /** The second official app: sum of big-endian 4-byte words of the UTF-8 bytes. */
        MULTI_SENSOR_APP
    }

    private static final long MASK = 0xffffffffL;

    private final long value;

    public ReceiverId(long value) {
        this.value = value & MASK;
    }

    /** Receiver ID for an account; the account ID is lower-cased first, as both apps do. */
    public static ReceiverId fromAccount(String accountId, Fold fold) {
        String normalized = accountId.toLowerCase(Locale.ROOT);
        return new ReceiverId(fold == Fold.LI3_APP ? multiplyXor(normalized) : wordSum(normalized));
    }

    /** Receiver ID for a start without an account, from a per-install unique ID used exactly as given. */
    public static ReceiverId accountless(String uniqueId) {
        return new ReceiverId(multiplyXor(uniqueId));
    }

    /** Parses the four wire bytes, as shown by {@link #littleEndianHex()}; colons and a 0x prefix are ignored. */
    public static ReceiverId fromLittleEndianHex(String hex) {
        byte[] bytes = Hex.toBytes(hex);
        if (bytes.length != 4) {
            throw new IllegalArgumentException("A receiver ID is 4 bytes, got " + bytes.length);
        }
        long v = 0;
        for (int i = 3; i >= 0; i--) {
            v = (v << 8) | (bytes[i] & 0xff);
        }
        return new ReceiverId(v);
    }

    public long value() {
        return value;
    }

    /** The four bytes as they go on the wire. */
    public byte[] littleEndian() {
        return new byte[]{(byte) value, (byte) (value >> 8), (byte) (value >> 16), (byte) (value >> 24)};
    }

    public String littleEndianHex() {
        return Hex.of(littleEndian());
    }

    /** Both spellings, for logs and the status page: {@code 0x6f0d8378 / 78830d6f}. */
    public String display() {
        return String.format(Locale.ROOT, "0x%08x / %s", value, littleEndianHex());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ReceiverId && ((ReceiverId) o).value == value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public String toString() {
        return display();
    }

    private static long multiplyXor(String s) {
        int h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = (h * 0x811c9dc5) ^ s.charAt(i);
        }
        return h & MASK;
    }

    private static long wordSum(String s) {
        int sum = 0;
        int word = 0;
        int filled = 0;
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            word = (word << 8) | (b & 0xff);
            if (++filled == 4) {
                sum += word;
                word = 0;
                filled = 0;
            }
        }
        if (filled > 0) {
            sum += word;
        }
        return sum & MASK;
    }
}
