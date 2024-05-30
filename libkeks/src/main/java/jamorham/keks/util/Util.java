package jamorham.keks.util;


import static java.lang.System.arraycopy;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import lombok.val;

/**
 * JamOrHam
 *
 * Generic utility methods
 */

public class Util {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] arrayAppend(byte[] existing, byte[] addon) {
        val newLength = existing.length + addon.length;
        val bigger = Arrays.copyOf(existing, newLength);
        arraycopy(addon, 0, bigger, existing.length, addon.length);
        return bigger;
    }

    public static byte[] arrayReduce(byte[] existing, final int length) {
        if (existing == null) return null;
        val dest = new byte[length];
        arraycopy(existing, 0, dest, 0, length);
        return dest;
    }

    public static byte[] tolerantHexStringToByteArray(String str) {
        return hexStringToByteArray(str.toUpperCase().replaceAll("[^A-F0-9]",""));
    }

    public static byte[] hexStringToByteArray(String str) {
        try {
            str = str.toUpperCase().trim();
            if (str.length() == 0) return null;
            final int len = str.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(str.charAt(i + 1), 16) << 4) + Character.digit(str.charAt(i), 16));
            }
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] intToByteArray(final int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static byte[] getRandomKey() {
        val keybytes = new byte[16];
        val sr = new SecureRandom();
        sr.nextBytes(keybytes);
        return keybytes;
    }

}
