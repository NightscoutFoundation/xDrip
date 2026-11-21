package xrwd.li3.ble;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * The Li3 GATT services and characteristics. Every UUID is {@code 0898XXXX-EF89-11E9-81B4-2A2AE2DBCCE4}.
 * Ported from RoundWhiteDiscKit {@code BLE/*SensorGATT.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class Gatt {

    /** The data service; the one to scan for. */
    public static final UUID SERVICE = uuid(0x10CC);
    public static final UUID SECURITY = uuid(0x203A);
    public static final UUID DEBUG = uuid(0x2400);

    public static final UUID GLUCOSE_DATA = uuid(0x177A);
    public static final UUID PATCH_STATUS = uuid(0x1482);
    public static final UUID PATCH_CONTROL = uuid(0x1338);
    public static final UUID HISTORIC_DATA = uuid(0x195A);
    public static final UUID EVENT_LOG = uuid(0x1BEE);
    public static final UUID CLINICAL_DATA = uuid(0x1AB8);
    public static final UUID FACTORY_DATA = uuid(0x1D24);

    public static final UUID SEC_CERT_DATA = uuid(0x23FA);
    public static final UUID SEC_CHALLENGE_DATA = uuid(0x22CE);
    public static final UUID SEC_COMMAND_RESPONSE = uuid(0x2198);

    /** Enabled after the handshake; the first CCCD write on these is what starts the sensor streaming. */
    public static final List<UUID> DATA_PLANE_NOTIFYING = Collections.unmodifiableList(Arrays.asList(
            PATCH_CONTROL, EVENT_LOG, FACTORY_DATA, GLUCOSE_DATA, PATCH_STATUS));

    /** The only characteristics that must be notifying before the handshake runs. */
    public static final List<UUID> HANDSHAKE_NOTIFYING = Collections.unmodifiableList(Arrays.asList(
            SEC_CERT_DATA, SEC_CHALLENGE_DATA, SEC_COMMAND_RESPONSE));

    private Gatt() {
    }

    /** True for the characteristics that carry handshake traffic. */
    public static boolean isSecurity(UUID characteristic) {
        return HANDSHAKE_NOTIFYING.contains(characteristic);
    }

    private static UUID uuid(int slot) {
        return UUID.fromString(String.format("0898%04X-EF89-11E9-81B4-2A2AE2DBCCE4", slot));
    }
}
