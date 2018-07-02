package com.eveningoutpost.dexdrip.G5Model;

// jamorham

public class FirmwareCapability {

    // new G6 firmware versions will need to be added here
    public static boolean isG6Firmware(String version) {
        return version.startsWith("1.6.5."); // .23 .25 etc
    }

    public static boolean isG5Firmware(String version) {
        return !isG6Firmware(version);
    }

    public static boolean isFirmwarePredictiveCapable(String version) {
        return isG6Firmware(version);
    }

    public static boolean isTransmitterPredictiveCapable(String tx_id) {
        return isG6Firmware(Ob1G5StateMachine.getRawFirmwareVersionString(tx_id));
    }

    public static boolean isTransmitterG6(String tx_id) {
        return isG6Firmware(Ob1G5StateMachine.getRawFirmwareVersionString(tx_id));
    }

}
