package com.eveningoutpost.dexdrip.cgm.nsfollow;

import androidx.annotation.VisibleForTesting;

import java.util.function.LongSupplier;

/**
 * In-memory, per-URL record of Nightscout server capabilities that turned out to be unsupported
 * at runtime — specifically the {@code devicestatus} endpoint, which the Juggluco emulator does
 * not implement (it returns HTTP 400).
 * <p>
 * The flag is "semi-sticky" but <b>self-healing</b>:
 * <ul>
 *   <li>Once marked unsupported for the current server it stays off, so we take the cheap path
 *       without re-probing every poll.</li>
 *   <li>After {@link #DEVICESTATUS_RECHECK_MS} it allows a single re-probe through — if the server
 *       has started supporting the endpoint (changed under our feet) the next success clears the
 *       flag via {@link #markDeviceStatusSupported}; if it still fails it is simply re-marked, so
 *       the cost of healing is at most one request per recheck window.</li>
 *   <li>The whole state resets immediately when the follow URL changes.</li>
 * </ul>
 * State is process-lifetime only; a cold start re-detects at most once.
 *
 * @author Asbjørn Aarrestad
 */
public final class NsServerCapabilities {

    /** How long to suppress devicestatus after a rejection before allowing a self-healing re-probe. */
    static final long DEVICESTATUS_RECHECK_MS = 6 * 60 * 60 * 1000L; // 6 hours

    /** Clock indirection so the recheck window is testable without sleeping. */
    @VisibleForTesting
    static volatile LongSupplier clock = System::currentTimeMillis;

    private static volatile String currentUrl = "";
    /** 0 = supported/never-marked; otherwise the timestamp of the last rejection. */
    private static volatile long deviceStatusUnsupportedAt = 0;

    private NsServerCapabilities() {
    }

    private static synchronized void syncUrl(final String url) {
        final String u = (url == null) ? "" : url;
        if (!u.equals(currentUrl)) {
            currentUrl = u;
            deviceStatusUnsupportedAt = 0;
        }
    }

    public static synchronized boolean supportsDeviceStatus(final String url) {
        syncUrl(url);
        if (deviceStatusUnsupportedAt == 0) {
            return true;
        }
        // Self-heal: once the recheck window elapses, allow a single re-probe through.
        return clock.getAsLong() - deviceStatusUnsupportedAt >= DEVICESTATUS_RECHECK_MS;
    }

    public static synchronized void markDeviceStatusUnsupported(final String url) {
        syncUrl(url);
        deviceStatusUnsupportedAt = clock.getAsLong();
    }

    /** Clears the unsupported flag — called when the endpoint responds successfully again. */
    public static synchronized void markDeviceStatusSupported(final String url) {
        syncUrl(url);
        deviceStatusUnsupportedAt = 0;
    }

    @VisibleForTesting
    public static synchronized void reset() {
        currentUrl = "";
        deviceStatusUnsupportedAt = 0;
        clock = System::currentTimeMillis;
    }
}
