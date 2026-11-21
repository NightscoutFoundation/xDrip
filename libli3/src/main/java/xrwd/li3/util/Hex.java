package xrwd.li3.util;

import java.util.Locale;

/**
 * Hex conversion for wire bytes and test vectors.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class Hex {

    private static final char[] DIGITS = "0123456789abcdef".toCharArray();

    private Hex() {
    }

    /** Parses hex, ignoring a {@code 0x} prefix, colons and whitespace. */
    public static byte[] toBytes(String hex) {
        String cleaned = hex.toLowerCase(Locale.ROOT).replace("0x", "").replaceAll("[:\\s]", "");
        if (cleaned.length() % 2 != 0) {
            throw new IllegalArgumentException("Odd number of hex digits: " + cleaned.length());
        }
        byte[] out = new byte[cleaned.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int high = Character.digit(cleaned.charAt(2 * i), 16);
            int low = Character.digit(cleaned.charAt(2 * i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Not a hex digit at " + (2 * i));
            }
            out[i] = (byte) ((high << 4) | low);
        }
        return out;
    }

    /** Lower-case hex, two digits per byte, no separator. */
    public static String of(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            out[2 * i] = DIGITS[(bytes[i] >> 4) & 0xf];
            out[2 * i + 1] = DIGITS[bytes[i] & 0xf];
        }
        return new String(out);
    }
}
