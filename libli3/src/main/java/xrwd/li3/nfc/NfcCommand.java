package xrwd.li3.nfc;

import xrwd.li3.pairing.ReceiverId;

/**
 * The ISO 15693 custom commands that read a Li3's patch info and start it or hand it to a new receiver.
 * Ported from RoundWhiteDiscKit {@code Pairing/NFCActivationCommand.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class NfcCommand {

    /** The sensor's ISO 15693 manufacturer code, sent after every custom command code. */
    private static final int MANUFACTURER_CODE = 0x7a;

    /** Reads the patch info; works in every sensor state. */
    public static final byte[] READ_PATCH_INFO = {0x02, (byte) 0xa1, MANUFACTURER_CODE};

    /** The command that binds the sensor to a receiver ID. */
    public enum Code {
        /** Starts a sensor that has never run. */
        ACTIVATE(0xa0),
        /** Moves a running sensor to the receiver ID given; it must be the ID the sensor was started with. */
        SWITCH_RECEIVER(0xa8);

        public final int value;

        Code(int value) {
            this.value = value;
        }
    }

    private NfcCommand() {
    }

    /** {@code 02 <code> 7a || timeSecondsLE4 || receiverIdLE4 || crcLE2}. */
    public static byte[] command(Code code, long timeSeconds, ReceiverId receiverId) {
        byte[] out = new byte[13];
        out[0] = 0x02;
        out[1] = (byte) code.value;
        out[2] = MANUFACTURER_CODE;
        for (int i = 0; i < 4; i++) {
            out[3 + i] = (byte) (timeSeconds >> (8 * i));
        }
        System.arraycopy(receiverId.littleEndian(), 0, out, 7, 4);
        byte[] body = new byte[8];
        System.arraycopy(out, 3, body, 0, 8);
        int crc = crc16(body);
        out[11] = (byte) crc;
        out[12] = (byte) (crc >> 8);
        return out;
    }

    /** CRC-16 with init 0xffff and polynomial 0x1021 over bit-reversed input bytes, no output reflection. */
    public static int crc16(byte[] data) {
        int crc = 0xffff;
        for (byte b : data) {
            crc ^= (Integer.reverse(b & 0xff) >>> 24) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
            }
            crc &= 0xffff;
        }
        return crc;
    }
}
