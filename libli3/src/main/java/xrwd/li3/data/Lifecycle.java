package xrwd.li3.data;

/**
 * Where a sensor is in its life, by minutes since start. Ported from RoundWhiteDiscKit
 * {@code DataPlane/SensorLifecycle.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class Lifecycle {

    public static final int DEFAULT_WARMUP_MINUTES = 60;
    /** The longest a Li3 is worn, 15 days, for a sensor whose own wear is not known. */
    public static final int DEFAULT_WEAR_MINUTES = 15 * 24 * 60;

    public enum Phase {
        WARMUP, ACTIVE, EXPIRED
    }

    private final int warmupMinutes;
    private final int wearMinutes;

    public Lifecycle(int warmupMinutes, int wearMinutes) {
        this.warmupMinutes = Math.max(0, warmupMinutes);
        this.wearMinutes = Math.max(0, wearMinutes);
    }

    public Phase phase(int lifeCount) {
        int elapsed = Math.max(0, lifeCount);
        if (elapsed >= wearMinutes) {
            return Phase.EXPIRED;
        }
        return elapsed < warmupMinutes ? Phase.WARMUP : Phase.ACTIVE;
    }

    /** Readings count from the end of warm-up until, not including, the end of wear. */
    public boolean usable(int lifeCount) {
        return phase(lifeCount) == Phase.ACTIVE;
    }

    public int remainingWarmupMinutes(int lifeCount) {
        return Math.max(0, warmupMinutes - Math.max(0, lifeCount));
    }

    public int remainingWearMinutes(int lifeCount) {
        return Math.max(0, wearMinutes - Math.max(0, lifeCount));
    }
}
