package com.eveningoutpost.dexdrip.G5Model;

/**
 * Created by joeginley on 3/16/16.
 */
public class BluetoothServices {

    //Transmitter Service UUIDs
    public static final String DeviceInfo = "180A";
    //iOS uses FEBC?
    public static final String Advertisement = "0000FEBC-0000-1000-8000-00805F9B34FB";
    public static final String CGMService = "F8083532-849E-531C-C594-30F1F86A4EA5";
    public static final String ServiceB = "F8084532-849E-531C-C594-30F1F86A4EA5";

    //DeviceInfoCharacteristicUUID, Read, DexcomUN
    public static final String ManufacturerNameString = "2A29";

    //CGMServiceCharacteristicUUID
    public static final String Communication = "F8083533-849E-531C-C594-30F1F86A4EA5";
    public static final String Control = "F8083534-849E-531C-C594-30F1F86A4EA5";
    public static final String Authentication = "F8083535-849E-531C-C594-30F1F86A4EA5";
    public static final String ProbablyBackfill = "F8083536-849E-531C-C594-30F1F86A4EA5";

    //ServiceBCharacteristicUUID
    public static final String CharacteristicE = "F8084533-849E-531C-C594-30F1F86A4EA5";
    public static final String CharacteristicF = "F8084534-849E-531C-C594-30F1F86A4EA5";

}
