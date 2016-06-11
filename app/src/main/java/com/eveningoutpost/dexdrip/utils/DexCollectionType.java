package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.Home;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by andy on 01/06/16.
 */
public enum DexCollectionType {

    None("None"),
    BluetoothWixel("BluetoothWixel"),
    DexcomShare("DexcomShare"),
    DexbridgeWixel("DexbridgeWixel"),
    LimiTTer("LimiTTer"),
    WifiBlueToothWixel("WifiBlueToothWixel"),
    WifiWixel("WifiWixel"),
    DexcomG5("DexcomG5"),
    WifiDexBridgeWixel("WifiDexbridgeWixel");

    String internalName;
    private static Map<String, DexCollectionType> mapToInternalName;

    static {
        mapToInternalName = new HashMap<>();

        for (DexCollectionType dct : values()) {
            mapToInternalName.put(dct.internalName, dct);
        }
    }


    DexCollectionType(String name) {
        this.internalName = name;
    }


    public static DexCollectionType getType(String dexCollectionType) {

        if (mapToInternalName.containsKey(dexCollectionType))
            return mapToInternalName.get(dexCollectionType);
        else
            return None;
    }

    public static DexCollectionType getDexCollectionType()
    {
        return getType(Home.getPreferencesStringWithDefault("dex_collection_method", "BluetoothWixel"));
    }
}
