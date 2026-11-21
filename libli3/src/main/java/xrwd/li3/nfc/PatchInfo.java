package xrwd.li3.nfc;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * A Li3's reply to {@link NfcCommand#READ_PATCH_INFO}: serial, state, region and lifetimes.
 * Ported from RoundWhiteDiscKit {@code Pairing/NFCActivationModels.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class PatchInfo {

    /** The region code in the patch info (per DiaBLE). Only US has been seen on hardware. */
    public enum Region {
        EUROPE(1), US(2), AU_CA(4), REST_OF_WORLD(8), UNKNOWN(-1);

        public final int code;

        Region(int code) {
            this.code = code;
        }

        static Region of(int code) {
            for (Region r : values()) {
                if (r.code == code) {
                    return r;
                }
            }
            return UNKNOWN;
        }
    }

    private static final int MARKER = 0xa5;
    private static final int MIN_LENGTH = 29;

    private final byte[] frame;

    private PatchInfo(byte[] frame) {
        this.frame = frame;
    }

    /**
     * Parses the reply with or without its leading {@code 00} status byte; a repeated {@code a5} pad, as some
     * readers return it, is collapsed first.
     *
     * @throws IllegalArgumentException when the reply is too short or has no {@code a5} marker
     */
    public static PatchInfo parse(byte[] raw) {
        byte[] frame = normalize(raw);
        if (frame.length < MIN_LENGTH || frame[0] != 0x00 || (frame[1] & 0xff) != MARKER) {
            throw new IllegalArgumentException("Not a Li3 patch info reply (" + raw.length + " bytes)");
        }
        return new PatchInfo(frame);
    }

    private static byte[] normalize(byte[] raw) {
        byte[] frame = raw;
        if (frame.length > 0 && (frame[0] & 0xff) == MARKER) {
            frame = new byte[raw.length + 1];
            System.arraycopy(raw, 0, frame, 1, raw.length);
        }
        if (frame.length < 2 || frame[0] != 0x00 || (frame[1] & 0xff) != MARKER) {
            return frame;
        }
        int bodyStart = 2;
        while (bodyStart < frame.length && (frame[bodyStart] & 0xff) == MARKER) {
            bodyStart++;
        }
        if (bodyStart == 2) {
            return frame;
        }
        byte[] out = new byte[2 + frame.length - bodyStart];
        out[1] = (byte) MARKER;
        System.arraycopy(frame, bodyStart, out, 2, frame.length - bodyStart);
        return out;
    }

    public int securityVersion() {
        return u16(3);
    }

    public Region region() {
        return Region.of(u16(5));
    }

    public int generation() {
        return u16(7);
    }

    public int wearMinutes() {
        return u16(9);
    }

    /** Dotted, most significant byte first: {@code 1.4.2.30}. */
    public String firmware() {
        return String.format(Locale.ROOT, "%d.%d.%d.%d", u8(14), u8(13), u8(12), u8(11));
    }

    public int productType() {
        return u8(15);
    }

    /** Stored in units of five minutes. */
    public int warmupMinutes() {
        return u8(16) * 5;
    }

    public int stateByte() {
        return u8(17);
    }

    public String serial() {
        return new String(Arrays.copyOfRange(frame, 18, 27), StandardCharsets.US_ASCII);
    }

    /** A sensor that has never run (state 0x01) is activated; any other is switched to the new receiver. */
    public NfcCommand.Code recommended() {
        return stateByte() == 0x01 ? NfcCommand.Code.ACTIVATE : NfcCommand.Code.SWITCH_RECEIVER;
    }

    private int u8(int offset) {
        return frame[offset] & 0xff;
    }

    private int u16(int offset) {
        return u8(offset) | (u8(offset + 1) << 8);
    }
}
