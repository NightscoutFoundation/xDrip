package xrwd.li3.data;

/**
 * Plaintexts for the patch-control characteristic. Only the historic backfill request is ported: nothing here reads
 * the event log or factory data, and the shutdown command ends the sensor for good. Ported from RoundWhiteDiscKit
 * {@code DataPlane/PatchControlCommand.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class PatchControl {

    /** The descriptor patch-control writes are sealed under. */
    public static final DataPlaneCrypto.PacketKind WRITE_KIND = DataPlaneCrypto.PacketKind.KIND0;

    private PatchControl() {
    }

    /** Asks for historic pages from a life count onward: {@code 01 00 01 lifeCountLE2 00 00}. */
    public static byte[] backfillFrom(int lifeCount) {
        return new byte[]{0x01, 0x00, 0x01, (byte) lifeCount, (byte) (lifeCount >> 8), 0x00, 0x00};
    }
}
