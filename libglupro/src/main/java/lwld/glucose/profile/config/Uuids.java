package lwld.glucose.profile.config;

import static lwld.glucose.profile.config.Flavour.*;
import static lwld.glucose.profile.config.Uuids.Item.*;

import java.util.HashMap;
import java.util.UUID;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * Uuid lookup handler for different flavours of device
 */

public class Uuids {
    public enum Item {
        GLUCOSE_SERVICE,
        DEVICE_INFORMATION_SERVICE,
        RECONNECTION_CONFIGURATION,
        FIRMWARE_CHAR,
        MANUFACTURER_CHAR,
        MODEL_NUMBER_CHAR,
        SERIAL_NUMBER_CHAR,
        HARDWARE_REVISION_CHAR,
        SYSTEM_ID_CHAR,
        GLUCOSE,
        RECORD_ACCESS,
        CONTROL_POINT,
        CGM_FEATURE,
        CGM_STATUS,
        SESSION_START_TIME,
        SESSION_RUN_TIME,
    }

    private static final HashMap<String, UUID> uuidMap = new HashMap<>();

    static {
        uuidMap.put(key(GENERIC, GLUCOSE_SERVICE), UUID.fromString("0000181F-0000-1000-8000-00805f9b34fb"));
        uuidMap.put(key(GENERIC, DEVICE_INFORMATION_SERVICE), UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb"));
        //
        uuidMap.put(key(GENERIC, FIRMWARE_CHAR), UUID.fromString("00002A26-0000-1000-8000-00805f9b34fb"));
        uuidMap.put(key(GENERIC, MANUFACTURER_CHAR), UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb"));
        uuidMap.put(key(GENERIC, MODEL_NUMBER_CHAR), UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb"));
        uuidMap.put(key(GENERIC, SERIAL_NUMBER_CHAR), UUID.fromString("00002A25-0000-1000-8000-00805f9b34fb"));
        uuidMap.put(key(GENERIC, HARDWARE_REVISION_CHAR), UUID.fromString("00002A27-0000-1000-8000-00805f9b34fb"));
        uuidMap.put(key(GENERIC, SYSTEM_ID_CHAR), UUID.fromString("00002A23-0000-1000-8000-00805f9b34fb"));
        //
        uuidMap.put(key(GENERIC, GLUCOSE), UUID.fromString("00002AA7-0000-1000-8000-00805f9b34fb"));
        uuidMap.put(key(GENERIC, RECORD_ACCESS), UUID.fromString("00002A52-0000-1000-8000-00805f9b34fb"));
        uuidMap.put(key(GENERIC, CONTROL_POINT), UUID.fromString("00002AAC-0000-1000-8000-00805f9b34fb"));
        uuidMap.put(key(GENERIC, CGM_FEATURE), UUID.fromString("00002AA8-0000-1000-8000-00805f9b34fb"));
        uuidMap.put(key(GENERIC, CGM_STATUS), UUID.fromString("00002AA9-0000-1000-8000-00805f9b34fb"));
        uuidMap.put(key(GENERIC, SESSION_START_TIME), UUID.fromString("00002AAA-0000-1000-8000-00805f9b34fb"));
        uuidMap.put(key(GENERIC, SESSION_RUN_TIME), UUID.fromString("00002AAB-0000-1000-8000-00805f9b34fb"));

    }

    public static UUID get(Flavour flavour, Item item) {
        val specificKey = key(flavour, item);
        val genericKey = key(GENERIC, item);
        return uuidMap.getOrDefault(specificKey, uuidMap.get(genericKey));
    }

    private static String key(Flavour flavour, Item item) {
        return flavour.name() + "-" + item.name();
    }

}
