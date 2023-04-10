package com.eveningoutpost.dexdrip.g5model;


import java.util.HashMap;
import java.util.UUID;

/**
 * Created by joeginley on 3/16/16.
 */
public class BluetoothServices {

    //Transmitter Service UUIDs
    public static final UUID DeviceInfo = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    //iOS uses FEBC?
    public static final UUID Advertisement = UUID.fromString("0000FEBC-0000-1000-8000-00805F9B34FB");
    public static final UUID CGMService = UUID.fromString("F8083532-849E-531C-C594-30F1F86A4EA5");
    public static final UUID ServiceB = UUID.fromString("F8084532-849E-531C-C594-30F1F86A4EA5");

    //DeviceInfoCharacteristicUUID, Read, DexcomUN
    public static final UUID ManufacturerNameString = UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB");

    //CGMServiceCharacteristicUUID
    public static final UUID Communication = UUID.fromString("F8083533-849E-531C-C594-30F1F86A4EA5");
    public static final UUID Control = UUID.fromString("F8083534-849E-531C-C594-30F1F86A4EA5");
    public static final UUID Authentication = UUID.fromString("F8083535-849E-531C-C594-30F1F86A4EA5");
    public static final UUID ProbablyBackfill = UUID.fromString("F8083536-849E-531C-C594-30F1F86A4EA5");
    public static final UUID ExtraData = UUID.fromString("F8083538-849E-531C-C594-30F1F86A4EA5");

    //ServiceBCharacteristicUUID
    public static final UUID CharacteristicE = UUID.fromString("F8084533-849E-531C-C594-30F1F86A4EA5");
    public static final UUID CharacteristicF = UUID.fromString("F8084534-849E-531C-C594-30F1F86A4EA5");

    //CharacteristicDescriptorUUID
    public static final UUID CharacteristicUpdateNotification = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    public static final UUID Mask16 = UUID.fromString("0000FFFF-0000-0000-0000-000000000000");

    private static final HashMap<UUID, String> mapToName = new HashMap<>();


    static {
        mapToName.put(DeviceInfo, "DeviceInfo");
        mapToName.put(Advertisement, "Advertisement");
        mapToName.put(CGMService, "CGMService");
        mapToName.put(ServiceB, "ServiceB");
        mapToName.put(ManufacturerNameString, "ManufacturerNameString");
        mapToName.put(Communication, "Communication");
        mapToName.put(Control, "Control");
        mapToName.put(Authentication, "Authentication");
        mapToName.put(ExtraData, "Extra Data");
        mapToName.put(ProbablyBackfill, "ProbablyBackfill");
        mapToName.put(CharacteristicE, "CharacteristicE");
        mapToName.put(CharacteristicF, "CharacteristicF");
        mapToName.put(CharacteristicUpdateNotification, "CharacteristicUpdateNotification");
    }


    public static String getUUIDName(UUID uuid) {
        if (uuid == null) return "null";
        if (mapToName.containsKey(uuid)) {
            return mapToName.get(uuid);
        } else {
            return "Unknown uuid: " + uuid.toString();
        }
    }

}
