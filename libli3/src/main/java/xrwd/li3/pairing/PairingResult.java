package xrwd.li3.pairing;

/**
 * The keys a finished handshake leaves behind: kAuth to cache for the next reconnect, and kEnc with ivEnc for the
 * data plane. Holds key material, so {@link #toString()} shows only the stage reached and whether the cache was used.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class PairingResult {

    private final byte[] kAuth;
    private final byte[] kEnc;
    private final byte[] ivEnc;
    private final byte[] sensorKey65;
    private final boolean usedCache;
    private final String lastStage;

    PairingResult(byte[] kAuth, byte[] kEnc, byte[] ivEnc, byte[] sensorKey65, boolean usedCache, String lastStage) {
        this.kAuth = kAuth.clone();
        this.kEnc = kEnc.clone();
        this.ivEnc = ivEnc.clone();
        this.sensorKey65 = sensorKey65 == null ? null : sensorKey65.clone();
        this.usedCache = usedCache;
        this.lastStage = lastStage;
    }

    public byte[] kAuth() {
        return kAuth.clone();
    }

    public byte[] kEnc() {
        return kEnc.clone();
    }

    public byte[] ivEnc() {
        return ivEnc.clone();
    }

    /** @return the static key from the sensor's certificate, {@code 04 || X || Y}; null after a reconnect */
    public byte[] sensorKey65() {
        return sensorKey65 == null ? null : sensorKey65.clone();
    }

    /** @return true when the cached kAuth was accepted and no certificate was exchanged */
    public boolean usedCache() {
        return usedCache;
    }

    public String lastStage() {
        return lastStage;
    }

    @Override
    public String toString() {
        return "PairingResult(stage=" + lastStage + ", cached=" + usedCache + ")";
    }
}
