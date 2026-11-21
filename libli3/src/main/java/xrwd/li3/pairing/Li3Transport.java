package xrwd.li3.pairing;

import java.io.IOException;
import java.util.UUID;

/**
 * One GATT operation per call, as the pairing handshake needs them. Framing is the caller's: {@link PairingFlow}
 * fragments what it writes and reassembles what it reads, so the transport passes bytes through unchanged.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public interface Li3Transport {

    /** Writes one value to a characteristic and returns once the write is acknowledged. */
    void write(UUID characteristic, byte[] value) throws IOException;

    /**
     * Returns the next notify received on a characteristic, waiting up to {@code timeoutMs} for it.
     *
     * @throws IOException on timeout or a lost link
     */
    byte[] awaitNotify(UUID characteristic, long timeoutMs) throws IOException;
}
