package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.Services.DexShareCollectionService;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;
import com.eveningoutpost.dexdrip.Services.Ob1G5CollectionService;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
    WifiDexBridgeWixel("WifiDexbridgeWixel"),
    Follower("Follower"),
    LibreAlarm("LibreAlarm"),
    NSEmulator("NSEmulator"),
    Disabled("Disabled"),
    Manual("Manual");

    String internalName;
    private static final Map<String, DexCollectionType> mapToInternalName;
    private static final HashSet<DexCollectionType> usesBluetooth = new HashSet<>();
    private static final HashSet<DexCollectionType> usesBtWixel = new HashSet<>();
    private static final HashSet<DexCollectionType> usesWifi = new HashSet<>();
    private static final HashSet<DexCollectionType> usesXbridge = new HashSet<>();
    private static final HashSet<DexCollectionType> usesFiltered = new HashSet<>();
    private static final HashSet<DexCollectionType> usesLibre = new HashSet<>();
    private static final HashSet<DexCollectionType> usesBattery = new HashSet<>();
    private static final HashSet<DexCollectionType> usesDexcomRaw = new HashSet<>();

    public static final String DEX_COLLECTION_METHOD = "dex_collection_method";

    public static boolean does_have_filtered = false; // TODO this could get messy with GC


    static {
        mapToInternalName = new HashMap<>();

        for (DexCollectionType dct : values()) {
            mapToInternalName.put(dct.internalName, dct);
        }

        Collections.addAll(usesBluetooth, BluetoothWixel, DexcomShare, DexbridgeWixel, LimiTTer, WifiBlueToothWixel, DexcomG5, WifiDexBridgeWixel);
        Collections.addAll(usesBtWixel, BluetoothWixel, LimiTTer, WifiBlueToothWixel);
        Collections.addAll(usesWifi, WifiBlueToothWixel,WifiWixel,WifiDexBridgeWixel);
        Collections.addAll(usesXbridge, DexbridgeWixel,WifiDexBridgeWixel);
        Collections.addAll(usesFiltered, DexbridgeWixel, WifiDexBridgeWixel, DexcomG5, WifiWixel, Follower); // Bluetooth and Wifi+Bluetooth need dynamic mode
        Collections.addAll(usesLibre, LimiTTer, LibreAlarm);
        Collections.addAll(usesBattery, BluetoothWixel, DexbridgeWixel, WifiBlueToothWixel, WifiDexBridgeWixel, Follower, LimiTTer, LibreAlarm); // parakeet separate
        Collections.addAll(usesDexcomRaw, BluetoothWixel, DexbridgeWixel, WifiBlueToothWixel, DexcomG5, WifiDexBridgeWixel);
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

    public static DexCollectionType getDexCollectionType() {
        return getType(Home.getPreferencesStringWithDefault(DEX_COLLECTION_METHOD, "BluetoothWixel"));
    }

    public static void setDexCollectionType(DexCollectionType t) {
        Home.setPreferencesString(DEX_COLLECTION_METHOD, t.internalName);
    }

    public static boolean hasBluetooth() {
        return usesBluetooth.contains(getDexCollectionType());
    }

    public static boolean hasBtWixel() { return usesBtWixel.contains(getDexCollectionType()); }

    public static boolean hasXbridgeWixel() {
        return usesXbridge.contains(getDexCollectionType());
    }

    public static boolean hasWifi() {
        return usesWifi.contains(getDexCollectionType());
    }

    public static boolean hasLibre() { return usesLibre.contains(getDexCollectionType()); }

    public static boolean hasLibre(DexCollectionType t) { return usesLibre.contains(t); }

    public static boolean hasBattery() { return usesBattery.contains(getDexCollectionType()); }

    public static boolean hasSensor() {
        return getDexCollectionType() != DexCollectionType.Manual;
    }

    public static boolean hasDexcomRaw() { return hasDexcomRaw(getDexCollectionType()); }

    public static boolean usesDexCollectionService(DexCollectionType type) { return usesBtWixel.contains(type) || usesXbridge.contains(type) || type.equals(LimiTTer); }

    public static boolean hasDexcomRaw(DexCollectionType type) {
        return usesDexcomRaw.contains(type);
    }

    public static boolean isFlakey() { return getDexCollectionType() == DexCollectionType.DexcomG5; }

    public static boolean hasFiltered() {
        return does_have_filtered || usesFiltered.contains(getDexCollectionType());
    }

    public static Class<?> getCollectorServiceClass() {
        switch (getDexCollectionType()) {
            case DexcomG5:
                if (Home.getPreferencesBooleanDefaultFalse(Ob1G5CollectionService.OB1G5_PREFS)) {
                    return Ob1G5CollectionService.class;
                } else {
                    return G5CollectionService.class;
                }
            case DexcomShare:
                return DexShareCollectionService.class;
            default:
                return DexCollectionService.class;
        }
    }

}
