package lwld.glucose.profile.packet;

/**
 * JamOrHam
 * <p>
 * CRC utilities
 */

public class CRC {

    public static int crc16Mcrf4xx(byte[] data) {
        int crc = 0xFFFF;
        final int POLY = 0x8408; // reflected 0x1021

        for (byte b : data) {
            crc ^= (b & 0xFF);

            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >>> 1) ^ POLY;
                } else {
                    crc >>>= 1;
                }
            }
        }

        // NO final XOR for MCRF4XX
        return crc & 0xFFFF;
    }

    public static byte[] appendCrc(byte[] buffer) {
        if (buffer == null) return null;
        int crc = crc16Mcrf4xx(buffer);

        byte[] out = new byte[buffer.length + 2];
        System.arraycopy(buffer, 0, out, 0, buffer.length);

        // Little-endian CRC
        out[out.length - 2] = (byte) (crc & 0xFF);
        out[out.length - 1] = (byte) ((crc >> 8) & 0xFF);

        return out;
    }

}
