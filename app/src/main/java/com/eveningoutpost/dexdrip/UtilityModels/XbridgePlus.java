package com.eveningoutpost.dexdrip.UtilityModels;

/**
 * Created by jamorham on 27/03/2017.
 */

/*
 Helper class to support xBridge protocol extensions
 */

public class XbridgePlus {

    public static byte[] sendDataRequestPacket() {
        return new byte[]{0x0c, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    }

    public static boolean isXbridgePacket() {
        return false; // not implemented yet
    }

    public static boolean isXbridgeExtensionPacket() {
        return false; // not implemented yet
    }


}
