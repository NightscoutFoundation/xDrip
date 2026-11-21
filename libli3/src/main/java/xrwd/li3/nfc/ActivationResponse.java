package xrwd.li3.nfc;

import java.util.Arrays;
import java.util.Locale;

/**
 * A Li3's reply to activate or switch-receiver: the BLE address, the BLE PIN and the activation time.
 * The PIN is a secret; nothing here puts it in a message or {@code toString}.
 * Ported from RoundWhiteDiscKit {@code Pairing/NFCActivationModels.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class ActivationResponse {

    private static final int LENGTH = 19;

    private final byte[] frame;

    private ActivationResponse(byte[] frame) {
        this.frame = frame;
    }

    /**
     * Parses the 19-byte reply, or the 18-byte form without the leading {@code 00} status byte.
     *
     * @throws IllegalArgumentException on any other length or layout; the message holds no reply bytes
     */
    public static ActivationResponse parse(byte[] raw) {
        byte[] frame = raw;
        if (raw.length == LENGTH - 1 && (raw[0] & 0xff) == 0xa5) {
            frame = new byte[LENGTH];
            System.arraycopy(raw, 0, frame, 1, raw.length);
        }
        if (frame.length != LENGTH || frame[0] != 0x00 || (frame[1] & 0xff) != 0xa5 || frame[2] != 0x00) {
            throw new IllegalArgumentException("Not a Li3 activation reply (" + raw.length + " bytes)");
        }
        return new ActivationResponse(frame);
    }

    /** Most significant byte first, upper case: {@code CC:22:DF:B8:F9:58}. */
    public String bleAddress() {
        StringBuilder sb = new StringBuilder();
        for (int i = 8; i >= 3; i--) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(String.format(Locale.ROOT, "%02X", frame[i] & 0xff));
        }
        return sb.toString();
    }

    public byte[] blePin() {
        return Arrays.copyOfRange(frame, 9, 13);
    }

    /** Zero on a fresh activation; the original start time on a switch-receiver reply. */
    public byte[] activationTimeRaw() {
        return Arrays.copyOfRange(frame, 13, 17);
    }
}
