package com.eveningoutpost.dexdrip;

public class HexTestTools {

    private final static char[] hexArray = "0123456789ABCDEF" .toCharArray();

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "<empty>";
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] tolerantHexStringToByteArray(String str) {
        return hexStringToByteArray(str.toUpperCase().replaceAll("[^A-F0-9]", ""));
    }

    public static byte[] hexStringToByteArray(String str) {
        try {
            str = str.toUpperCase().trim();
            if (str.length() == 0) return null;
            final int len = str.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
            }
            return data;
        } catch (Exception e) {
            System.out.println("Exception processing hexString: " + e);
            return null;
        }
    }

    public static byte[] toTwoBytes(final int value) {
        return new byte[]{(byte) (value >> 8), (byte) value};
    }

    public static String reverseString(final String string) {
        final StringBuilder sb = new StringBuilder();
        sb.append(string);
        return sb.reverse().toString();
    }

    public static String leftPadWithZeros(final String string, final int padTo) {
        final StringBuilder sb = new StringBuilder(string);
        while (sb.length() < padTo) {
            sb.insert(0, "0");
        }
        return sb.toString();
    }

    public static String intToPaddedBinary(final int i) {
        return leftPadWithZeros(Integer.toBinaryString(i), 32);
    }
}
