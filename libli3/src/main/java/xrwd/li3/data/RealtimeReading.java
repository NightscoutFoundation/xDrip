package xrwd.li3.data;

import java.util.Arrays;

/**
 * The 29-byte realtime record from the glucose characteristic. Ported from RoundWhiteDiscKit
 * {@code DataPlane/RealtimeGlucoseReading.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class RealtimeReading {

    public static final int SIZE = 29;

    private static final int FAST_DATA_OFFSET = 21;

    public enum Trend {
        NOT_DETERMINED, FALLING_QUICKLY, FALLING, STABLE, RISING, RISING_QUICKLY, UNKNOWN
    }

    private final byte[] bytes;

    private RealtimeReading(byte[] bytes) {
        this.bytes = bytes;
    }

    /** @throws IllegalArgumentException unless the plaintext is 29 bytes */
    public static RealtimeReading parse(byte[] plain) {
        if (plain.length != SIZE) {
            throw new IllegalArgumentException("Realtime reading is " + plain.length + " bytes, expected " + SIZE);
        }
        return new RealtimeReading(plain.clone());
    }

    /** Minutes since the sensor started. */
    public int lifeCount() {
        return u16(0);
    }

    /** @return mg/dL as the reader displays it (39 for LO, 501 for HI), or null when there is no value */
    public Integer mgdl() {
        return displayMgdl(u16(15));
    }

    public Trend trend() {
        int code = bytes[14] & 0x07;
        return code <= Trend.RISING_QUICKLY.ordinal() ? Trend.values()[code] : Trend.UNKNOWN;
    }

    /** The current word when its error bit is set, else 0. */
    public int dataQualityError() {
        int word = u16(2);
        return (word & 0x8000) != 0 ? word : 0;
    }

    public int sensorCondition() {
        return (u16(2) >> 13) & 0x03;
    }

    /** A value is present and neither data quality nor sensor condition flags it. Actionability is advisory. */
    public boolean displayable() {
        return mgdl() != null && dataQualityError() == 0 && sensorCondition() == 0;
    }

    public byte[] fastData() {
        return Arrays.copyOfRange(bytes, FAST_DATA_OFFSET, SIZE);
    }

    static Integer displayMgdl(int raw) {
        if (raw >= 1 && raw <= 38) {
            return 39;
        }
        if (raw >= 39 && raw <= 501) {
            return raw;
        }
        if (raw >= 502 && raw <= 999) {
            return 501;
        }
        return null;
    }

    private int u16(int offset) {
        return (bytes[offset] & 0xff) | (bytes[offset + 1] & 0xff) << 8;
    }
}
