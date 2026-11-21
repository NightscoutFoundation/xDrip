package xrwd.li3.data;

/**
 * The 12-byte patch-status record. Ported from RoundWhiteDiscKit {@code DataPlane/PatchStatus.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class PatchStatus {

    public static final int SIZE = 12;

    /** What the sensor asks of the wearer, from its error code or, without one, its patch state. */
    public enum Attention {
        NONE, CHECK_SENSOR, SENSOR_ENDED, REPLACE_SENSOR, UNKNOWN
    }

    private final int errorCode;
    private final int state;
    private final int currentLifeCount;

    private PatchStatus(int errorCode, int state, int currentLifeCount) {
        this.errorCode = errorCode;
        this.state = state;
        this.currentLifeCount = currentLifeCount;
    }

    /** @throws IllegalArgumentException unless the plaintext is 12 bytes */
    public static PatchStatus parse(byte[] plain) {
        if (plain.length != SIZE) {
            throw new IllegalArgumentException("Patch status is " + plain.length + " bytes, expected " + SIZE);
        }
        return new PatchStatus(s16(plain, 2), plain[7], s16(plain, 8));
    }

    /** Minutes since the sensor started, as the lifecycle reads it. */
    public int currentLifeCount() {
        return currentLifeCount;
    }

    public int state() {
        return state;
    }

    public int errorCode() {
        return errorCode;
    }

    public Attention attention() {
        switch (errorCode) {
            case 0:
                return stateAttention();
            case 3:
            case 7:
                return Attention.CHECK_SENSOR;
            case 5:
            case 6:
                return Attention.SENSOR_ENDED;
            case 8:
                return Attention.REPLACE_SENSOR;
            default:
                return Attention.UNKNOWN;
        }
    }

    private Attention stateAttention() {
        switch (state) {
            case 3:
                return Attention.CHECK_SENSOR;
            case 5:
            case 6:
                return Attention.SENSOR_ENDED;
            case 7:
            case 8:
                return Attention.REPLACE_SENSOR;
            default:
                return Attention.NONE;
        }
    }

    private static int s16(byte[] b, int offset) {
        return (short) ((b[offset] & 0xff) | (b[offset + 1] & 0xff) << 8);
    }
}
