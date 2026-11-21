package xrwd.li3.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The 14-byte historic record: a starting life count and six readings five minutes apart. Ported from
 * RoundWhiteDiscKit {@code DataPlane/HistoricalReadingPage.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class HistoricalPage {

    public static final int SIZE = 14;
    public static final int SPACING = 5;

    private static final int ENTRIES = 6;

    /** One historic reading. */
    public static final class Entry {
        public final int lifeCount;
        /** Null when the sensor stored no value. */
        public final Integer mgdl;

        Entry(int lifeCount, Integer mgdl) {
            this.lifeCount = lifeCount;
            this.mgdl = mgdl;
        }
    }

    private HistoricalPage() {
    }

    /** @throws IllegalArgumentException unless the plaintext is 14 bytes */
    public static List<Entry> parse(byte[] plain) {
        if (plain.length != SIZE) {
            throw new IllegalArgumentException("Historic page is " + plain.length + " bytes, expected " + SIZE);
        }
        int start = u16(plain, 0);
        List<Entry> entries = new ArrayList<>(ENTRIES);
        for (int i = 0; i < ENTRIES; i++) {
            entries.add(new Entry(start + SPACING * i, RealtimeReading.displayMgdl(u16(plain, 2 + 2 * i))));
        }
        return Collections.unmodifiableList(entries);
    }

    private static int u16(byte[] b, int offset) {
        return (b[offset] & 0xff) | (b[offset + 1] & 0xff) << 8;
    }
}
