package xrwd.li3.ble;

/**
 * Joins the two-part glucose notify: a reading arrives as a 15-byte first half and a 20-byte second half that carries
 * the sequence trailer, and the CCM tag only verifies over both. Any other notify passes through whole. Ported from
 * RoundWhiteDiscKit {@code DataPlane/DataPlaneDecoder.swift} {@code DataPlaneNotificationAssembler} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class GlucoseNotifyAssembler {

    private static final int FIRST_HALF_SIZE = 15;

    private byte[] firstHalf;

    /** @return the joined frame, or null while holding a first half */
    public byte[] add(byte[] fragment) {
        if (firstHalf != null) {
            byte[] joined = new byte[firstHalf.length + fragment.length];
            System.arraycopy(firstHalf, 0, joined, 0, firstHalf.length);
            System.arraycopy(fragment, 0, joined, firstHalf.length, fragment.length);
            firstHalf = null;
            return joined;
        }
        if (fragment.length == FIRST_HALF_SIZE) {
            firstHalf = fragment.clone();
            return null;
        }
        return fragment;
    }

    /** Drops a held first half, for a reconnect. */
    public void reset() {
        firstHalf = null;
    }
}
