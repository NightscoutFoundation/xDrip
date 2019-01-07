package com.eveningoutpost.dexdrip.G5Model;

// jamorham

import com.google.common.collect.ImmutableSet;

public class FirmwareCapability {

    private static final ImmutableSet<String> KNOWN_G5_FIRMWARES = ImmutableSet.of("1.0.0.13", "1.0.0.17", "1.0.4.10", "1.0.4.12");
    private static final ImmutableSet<String> KNOWN_G6_FIRMWARES = ImmutableSet.of("1.6.5.23", "1.6.5.25");
    private static final ImmutableSet<String> KNOWN_G6_REV2_FIRMWARES = ImmutableSet.of("2.18.2.67");

    // new G6 firmware versions will need to be added here / above
    private static boolean isG6Firmware(final String version) {
        return KNOWN_G6_FIRMWARES.contains(version) || KNOWN_G6_REV2_FIRMWARES.contains(version) || version.startsWith("1.6.5."); // .23 .25 etc
    }

    public static boolean isG6Rev2(final String version) {
        return KNOWN_G6_REV2_FIRMWARES.contains(version) || version.startsWith("2.18.");
    }

    private static boolean isG5Firmware(final String version) {
        return KNOWN_G5_FIRMWARES.contains(version);
    }

    private static boolean isFirmwarePredictiveCapable(final String version) {
        return isG6Firmware(version);
    }

    public static boolean isTransmitterPredictiveCapable(final String tx_id) {
        return isG6Firmware(Ob1G5StateMachine.getRawFirmwareVersionString(tx_id));
    }

    public static boolean isTransmitterG5(final String tx_id) {
        return isG5Firmware(Ob1G5StateMachine.getRawFirmwareVersionString(tx_id));
    }

    public static boolean isTransmitterG6(final String tx_id) {
        return isG6Firmware(Ob1G5StateMachine.getRawFirmwareVersionString(tx_id));
    }

    public static boolean isTransmitterG6Rev2(final String tx_id) {
        return isG6Rev2(Ob1G5StateMachine.getRawFirmwareVersionString(tx_id));
    }

}
