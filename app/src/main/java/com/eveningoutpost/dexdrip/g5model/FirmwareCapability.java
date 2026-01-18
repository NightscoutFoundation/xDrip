package com.eveningoutpost.dexdrip.g5model;

// jamorham

import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.getFirmwareXDetails;
import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.getRawFirmwareVersionString;
import static com.eveningoutpost.dexdrip.models.JoH.emptyString;

import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.google.common.collect.ImmutableSet;

import lombok.val;

public class FirmwareCapability {

    private static final ImmutableSet<String> KNOWN_G5_FIRMWARES = ImmutableSet.of("1.0.0.13", "1.0.0.17", "1.0.4.10", "1.0.4.12", "1.0.4.14", "1.0.4.15");
    private static final ImmutableSet<String> KNOWN_G6_FIRMWARES = ImmutableSet.of("1.6.5.23", "1.6.5.25", "1.6.5.27");
    private static final ImmutableSet<String> KNOWN_G6_REV2_FIRMWARES = ImmutableSet.of("2.18.2.67", "2.18.2.88", "2.18.2.98", "2.24.2.88", "2.27.2.98", "2.27.2.103", "2.27.2.105", "2.27.2.106");
    private static final ImmutableSet<String> KNOWN_G6_REV2_RAW_FIRMWARES = ImmutableSet.of("2.18.2.67");
    private static final ImmutableSet<String> KNOWN_G6_PLUS_FIRMWARES = ImmutableSet.of("2.4.2.88");
    private static final ImmutableSet<String> KNOWN_ONE_FIRMWARES = ImmutableSet.of("30.192.103.34");
    private static final ImmutableSet<String> KNOWN_ALT_FIRMWARES = ImmutableSet.of("29.192.104.59", "32.192.104.82", "32.192.104.109", "32.192.105.64", "32.192.106.0", "44.192.105.72");
    private static final ImmutableSet<String> KNOWN_TIME_TRAVEL_TESTED = ImmutableSet.of("1.6.5.25");
    private static final ImmutableSet<String> KNOWN_ALT2_FIRMWARES = ImmutableSet.of("37.192.105.94");
    private static final ImmutableSet<String> KNOWN_ALT3_FIRMWARES = ImmutableSet.of("63.192.109.41");

    // new G6 firmware versions will need to be added here / above
    static boolean isG6Firmware(final String version) {
        return version != null && (KNOWN_G6_FIRMWARES.contains(version)
                || KNOWN_G6_REV2_FIRMWARES.contains(version)
                || KNOWN_G6_PLUS_FIRMWARES.contains(version)
                || version.startsWith("1.6.5.")
                || version.startsWith("2.18.")
                || version.startsWith("2.24.")
                || version.startsWith("2.27.")
                || version.startsWith("2.4.")
                || isDex1Firmware(version));
    }

    static boolean isDex1Firmware(final String version) {
        return version.startsWith("30.");
    }

    public static boolean isG6Rev2(final String version) {
        return version != null && (KNOWN_G6_REV2_FIRMWARES.contains(version) || version.startsWith("2.18.") || version.startsWith("2.24.") || version.startsWith("2.27."));
    }

    public static boolean isG6Plus(final String version) {
        return version != null && (KNOWN_G6_PLUS_FIRMWARES.contains(version) || version.startsWith("2.4."));
    }

    static boolean isG5Firmware(final String version) {
        return KNOWN_G5_FIRMWARES.contains(version);
    }

    static boolean isFirmwareTimeTravelCapable(final String version) {
        return KNOWN_TIME_TRAVEL_TESTED.contains(version);
    }

    public static boolean isFirmwareTemperatureCapable(final String version) {
        return !isG6Plus(version);
    }

    public static boolean isFirmwareResistanceCapable(final String Version) {
        return !isG6Rev2(Version) && !isG6Plus(Version);
    }

    private static boolean isFirmwarePredictiveCapable(final String version) {
        return isG6Firmware(version);
    }

    static boolean isFirmwareRawCapable(final String version) {
        return version == null
                || version.equals("")
                || KNOWN_G5_FIRMWARES.contains(version)
                || KNOWN_G6_FIRMWARES.contains(version)
                || KNOWN_G6_REV2_RAW_FIRMWARES.contains(version);
    }

    static boolean isFirmwarePreemptiveRestartCapable(final String version) {
        return isFirmwareRawCapable(version); // hang off this for now as they are currently the same
    }

    static boolean isAltOrAlt2OrAlt3Firmware(final String version) {
        return KNOWN_ALT_FIRMWARES.contains(version) || KNOWN_ALT2_FIRMWARES.contains(version) || KNOWN_ALT3_FIRMWARES.contains(version);
    }

    static boolean isAlt2Firmware(final String version) {
        return KNOWN_ALT2_FIRMWARES.contains(version);
    }

    static boolean isAlt3Firmware(final String version) {
        return KNOWN_ALT3_FIRMWARES.contains(version);
    }

    public static boolean isTransmitterPredictiveCapable(final String tx_id) {
        return isG6Firmware(getRawFirmwareVersionString(tx_id));
    }

    public static boolean isTransmitterModified(final String tx_id) {
        val vr1 = (VersionRequest1RxMessage) getFirmwareXDetails(tx_id, 1);
        if (vr1 != null) {
            return vr1.max_runtime_days >= 180;
        }
        return false;
    }

    public static boolean isTransmitterStandardFirefly(final String tx_id) { // Firefly that has not been modified
        if (!isTransmitterModified(tx_id) && isTransmitterRawIncapable(tx_id)) {
            return true;
        }
        return false;
    }

    public static boolean isDeviceAltOrAlt2OrAlt3(final String tx_id) {
        return isAltOrAlt2OrAlt3Firmware(getRawFirmwareVersionString(tx_id));
    }

    public static boolean isDeviceAlt2(final String tx_id) {
        return isAlt2Firmware(getRawFirmwareVersionString(tx_id));
    }

    public static boolean isDeviceAlt3(final String tx_id) {
        return isAlt3Firmware(getRawFirmwareVersionString(tx_id));
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
        return isFirmwareRawCapable(getRawFirmwareVersionString(tx_id))
                || isTransmitterModified(tx_id);
    }

    public static boolean doWeHaveVersion(final String tx_id) {
        val firmware_version = getRawFirmwareVersionString(tx_id);
        return !emptyString(firmware_version) && !firmware_version.equals("error");
    }

    public static boolean isTransmitterRawIncapable(final String tx_id) {
        val firmware_version = getRawFirmwareVersionString(tx_id);
        return doWeHaveVersion(tx_id) && isKnownFirmware(firmware_version) && !isFirmwareRawCapable(firmware_version);
    }

    public static boolean isTransmitterPreemptiveRestartCapable(final String tx_id) {
        return isFirmwarePreemptiveRestartCapable(getRawFirmwareVersionString(tx_id));
    }

    public static boolean isKnownFirmware(final String version) {
        return (version == null || version.equals("")
                || KNOWN_G5_FIRMWARES.contains(version)
                || KNOWN_G6_FIRMWARES.contains(version)
                || KNOWN_ONE_FIRMWARES.contains(version)
                || KNOWN_ALT_FIRMWARES.contains(version)
                || KNOWN_G6_REV2_FIRMWARES.contains(version)
                || KNOWN_G6_PLUS_FIRMWARES.contains(version)
                || KNOWN_ALT2_FIRMWARES.contains(version)
                || KNOWN_ALT3_FIRMWARES.contains(version));
    }

    public static boolean isTransmitterKnownFirmware(final String tx_id) {
        final String version = getRawFirmwareVersionString(tx_id);
        return isKnownFirmware(version);
    }

    static long getWarmupPeriodForVersion(final String version) {
        return isG6Plus(version) ? Constants.HOUR_IN_MS : Constants.HOUR_IN_MS * 2;
    }

    public static long getWarmupPeriod(final String tx_id) {
        return getWarmupPeriodForVersion(getRawFirmwareVersionString(tx_id));
    }
}
