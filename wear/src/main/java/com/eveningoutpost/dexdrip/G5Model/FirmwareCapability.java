package com.eveningoutpost.dexdrip.G5Model;

// jamorham

import com.google.common.collect.ImmutableSet;

import static com.eveningoutpost.dexdrip.G5Model.Ob1G5StateMachine.getRawFirmwareVersionString;

public class FirmwareCapability {

    private static final ImmutableSet<String> KNOWN_G5_FIRMWARES = ImmutableSet.of("1.0.0.13", "1.0.0.17", "1.0.4.10", "1.0.4.12");
    private static final ImmutableSet<String> KNOWN_G6_FIRMWARES = ImmutableSet.of("1.6.5.23", "1.6.5.25", "1.6.5.27");
    private static final ImmutableSet<String> KNOWN_G6_REV2_FIRMWARES = ImmutableSet.of("2.18.2.67", "2.18.2.88");
    private static final ImmutableSet<String> KNOWN_TIME_TRAVEL_TESTED = ImmutableSet.of("1.6.5.25");

    // new G6 firmware versions will need to be added here / above
    private static boolean isG6Firmware(final String version) {
        return version != null && (KNOWN_G6_FIRMWARES.contains(version) || KNOWN_G6_REV2_FIRMWARES.contains(version) || version.startsWith("1.6.5."));
    }

    public static boolean isG6Rev2(final String version) {
        return KNOWN_G6_REV2_FIRMWARES.contains(version) || version.startsWith("2.18.");
    }

    private static boolean isG5Firmware(final String version) {
        return KNOWN_G5_FIRMWARES.contains(version);
    }

    private static boolean isFirmwareTimeTravelCapable(final String version) {
        return KNOWN_TIME_TRAVEL_TESTED.contains(version);
    }

    private static boolean isFirmwarePredictiveCapable(final String version) {
        return isG6Firmware(version);
    }

    private static boolean isFirmwareRawCapable(final String version) {
        return !version.equals("2.18.2.88"); // TODO will need changing in future and logic inverting
    }

    private static boolean isFirmwarePreemptiveRestartCapable(final String version) {
        return !version.equals("2.18.2.88"); // TODO will need changing in future and logic inverting)
    }

    public static boolean isTransmitterPredictiveCapable(final String tx_id) {
        return isG6Firmware(getRawFirmwareVersionString(tx_id));
    }

    public static boolean isTransmitterG5(final String tx_id) {
        return isG5Firmware(getRawFirmwareVersionString(tx_id));
    }

    public static boolean isTransmitterG6(final String tx_id) {
        return isG6Firmware(getRawFirmwareVersionString(tx_id));
    }

    public static boolean isTransmitterG6Rev2(final String tx_id) {
        return isG6Rev2(getRawFirmwareVersionString(tx_id));
    }

    public static boolean isTransmitterTimeTravelCapable(final String tx_id) {
        return isFirmwareTimeTravelCapable(getRawFirmwareVersionString(tx_id));
    }

    public static boolean isTransmitterRawCapable(final String tx_id) {
        return isFirmwareRawCapable(getRawFirmwareVersionString(tx_id));
    }

    public static boolean isTransmitterPreemptiveRestartCapable(final String tx_id) {
        return isFirmwarePreemptiveRestartCapable(getRawFirmwareVersionString(tx_id));
    }
}
