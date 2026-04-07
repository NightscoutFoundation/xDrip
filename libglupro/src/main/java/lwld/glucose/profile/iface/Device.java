package lwld.glucose.profile.iface;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * JamOrHam
 * <p>
 * Data object for device scan results
 */
@Data
public class Device {
    private final String name;
    private final String address;
    private boolean isPaired = false;
    private String pairingPin;
    @EqualsAndHashCode.Exclude
    private final long firstSeen = System.currentTimeMillis();
}
