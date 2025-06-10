package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.services.DexCollectionService;
import com.eveningoutpost.dexdrip.services.DexShareCollectionService;
import com.eveningoutpost.dexdrip.services.DoNothingService;
import com.eveningoutpost.dexdrip.services.G5CollectionService;
import com.eveningoutpost.dexdrip.services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.services.UiBasedCollector;
import com.eveningoutpost.dexdrip.services.WifiCollectionService;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.cgm.medtrum.MedtrumCollectionService;
import com.eveningoutpost.dexdrip.cgm.nsfollow.NightscoutFollowService;
import com.eveningoutpost.dexdrip.cgm.sharefollow.ShareFollowService;
import com.eveningoutpost.dexdrip.cgm.webfollow.WebFollowService;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.CareLinkFollowService;

import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.shortTxId;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import lombok.Getter;

/**
 * Created by andy on 01/06/16.
 */
public enum DexCollectionType {

    None("None"),
    BluetoothWixel("BluetoothWixel"),
    DexcomShare("DexcomShare"),
    DexbridgeWixel("DexbridgeWixel"),
    LimiTTer("LimiTTer"),
    LimiTTerWifi("LimiTTerWifi"),
    LibreWifi("LibreWifi"),
    WifiBlueToothWixel("WifiBlueToothWixel"),
    WifiWixel("WifiWixel"),
    DexcomG5("DexcomG5"),
    DexcomG6("DexcomG6"), // currently pseudo
    WifiDexBridgeWixel("WifiDexbridgeWixel"),
    Follower("Follower"),
    LibreAlarm("LibreAlarm"),
    NSEmulator("NSEmulator"),
    NSFollow("NSFollower"),
    SHFollow("SHFollower"),
    WebFollow("WebFollower"),
    CLFollow("CLFollower"),
    Medtrum("Medtrum"),
    UiBased("UiBased"),
    Disabled("Disabled"),
    Mock("Mock"),
    Manual("Manual"),
    LibreReceiver("LibreReceiver"),
    AidexReceiver("AidexReceiver");

    @Getter
    String internalName;
    private static final Map<String, DexCollectionType> mapToInternalName;
    private static final HashSet<DexCollectionType> usesBluetooth = new HashSet<>();
    private static final HashSet<DexCollectionType> usesBtWixel = new HashSet<>();
    private static final HashSet<DexCollectionType> usesWifi = new HashSet<>();
    private static final HashSet<DexCollectionType> usesXbridge = new HashSet<>();
    private static final HashSet<DexCollectionType> usesFiltered = new HashSet<>();
    private static final HashSet<DexCollectionType> usesLibre = new HashSet<>();
    private static final HashSet<DexCollectionType> isPassive = new HashSet<>();
    private static final HashSet<DexCollectionType> usesBattery = new HashSet<>();
    private static final HashSet<DexCollectionType> usesDexcomRaw = new HashSet<>();
    private static final HashSet<DexCollectionType> usesTransmitterBattery = new HashSet<>();

    public static final String DEX_COLLECTION_METHOD = "dex_collection_method";

    public static boolean does_have_filtered = false; // TODO this could get messy with GC


    static {
        mapToInternalName = new HashMap<>();

        for (DexCollectionType dct : values()) {
            mapToInternalName.put(dct.internalName, dct);
        }

        Collections.addAll(usesBluetooth, BluetoothWixel, DexcomShare, DexbridgeWixel, LimiTTer, WifiBlueToothWixel, DexcomG5, WifiDexBridgeWixel, LimiTTerWifi, Medtrum);
        Collections.addAll(usesBtWixel, BluetoothWixel, LimiTTer, WifiBlueToothWixel, LimiTTerWifi); // Name is misleading here, should probably be using dexcollectionservice
        Collections.addAll(usesWifi, WifiBlueToothWixel, WifiWixel, WifiDexBridgeWixel, Mock, LimiTTerWifi, LibreWifi);
        Collections.addAll(usesXbridge, DexbridgeWixel, WifiDexBridgeWixel);
        Collections.addAll(usesFiltered, DexbridgeWixel, WifiDexBridgeWixel, DexcomG5, WifiWixel, Follower, Mock); // Bluetooth and Wifi+Bluetooth need dynamic mode
        Collections.addAll(usesLibre, LimiTTer, LibreAlarm, LimiTTerWifi, LibreWifi, LibreReceiver);
        Collections.addAll(isPassive, NSEmulator, NSFollow, SHFollow, WebFollow, LibreReceiver, UiBased, CLFollow, AidexReceiver);
        Collections.addAll(usesBattery, BluetoothWixel, DexbridgeWixel, WifiBlueToothWixel, WifiDexBridgeWixel, Follower, LimiTTer, LibreAlarm, LimiTTerWifi, LibreWifi); // parakeet separate
        Collections.addAll(usesDexcomRaw, BluetoothWixel, DexbridgeWixel, WifiWixel, WifiBlueToothWixel, DexcomG5, WifiDexBridgeWixel, Mock);
        Collections.addAll(usesTransmitterBattery, WifiWixel, BluetoothWixel, DexbridgeWixel, WifiBlueToothWixel, WifiDexBridgeWixel); // G4 transmitter battery
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
        return getType(Pref.getString(DEX_COLLECTION_METHOD, "BluetoothWixel"));
    }

    public static void setDexCollectionType(DexCollectionType t) {
        Pref.setString(DEX_COLLECTION_METHOD, t.internalName);
    }

    public static boolean isG7() {
        return DexCollectionType.getBestCollectorHardwareName().equals("G7");
    }

    public static boolean hasBluetooth() {
        return usesBluetooth.contains(getDexCollectionType());
    }

    public static boolean hasBtWixel() {
        return usesBtWixel.contains(getDexCollectionType());
    }

    public static boolean hasXbridgeWixel() {
        return usesXbridge.contains(getDexCollectionType());
    }

    public static boolean hasWifi() {
        return usesWifi.contains(getDexCollectionType());
    }

    public static boolean hasLibre() {
        return usesLibre.contains(getDexCollectionType());
    }

    public static boolean hasLibre(DexCollectionType t) {
        return usesLibre.contains(t);
    }

    public static boolean hasBattery() {
        return usesBattery.contains(getDexCollectionType());
    }

    public static boolean hasSensor() {
        return getDexCollectionType() != DexCollectionType.Manual;
    }

    public static boolean hasDexcomRaw() {
        return hasDexcomRaw(getDexCollectionType());
    }

    public static boolean usesDexCollectionService(DexCollectionType type) {
        return usesBtWixel.contains(type) || usesXbridge.contains(type) || type.equals(LimiTTer);
    }

    public static boolean usesClassicTransmitterBattery() {
        return usesTransmitterBattery.contains(getDexCollectionType());
    }

    public static boolean hasDexcomRaw(DexCollectionType type) {
        return usesDexcomRaw.contains(type);
    }

    public static boolean isFlakey() {
        return getDexCollectionType() == DexCollectionType.DexcomG5;
    }

    public static boolean hasFiltered() {
        return does_have_filtered || usesFiltered.contains(getDexCollectionType());
    }

    // Non calibrable means that raw values are used with oop2
    public static boolean isLibreOOPNonCalibratebleAlgorithm(DexCollectionType collector) {
        if (collector == null) {
            collector = DexCollectionType.getDexCollectionType();
        }
        return hasLibre(collector) &&
                (Pref.getBooleanDefaultFalse("external_blukon_algorithm") ||
                        Pref.getString("calibrate_external_libre_2_algorithm_type", "calibrate_raw").equals("no_calibration"));
    }

    public static Class<?> getCollectorServiceClass() {
        return getCollectorServiceClass(getDexCollectionType());
    }

    public static Class<?> getCollectorServiceClass(final DexCollectionType type) {
        switch (type) {
            case DexcomG5:
                if (Pref.getBooleanDefaultFalse(Ob1G5CollectionService.OB1G5_PREFS)) {
                    return Ob1G5CollectionService.class;
                } else {
                    return G5CollectionService.class;
                }
            case DexcomShare:
                return DexShareCollectionService.class;
            case WifiWixel:
            case Mock:
                return WifiCollectionService.class;
            case Medtrum:
                return MedtrumCollectionService.class;
            case Follower:
            case LibreReceiver:
                return DoNothingService.class;
            case NSFollow:
                return NightscoutFollowService.class;
            case SHFollow:
                return ShareFollowService.class;
            case WebFollow:
                return WebFollowService.class;
            case UiBased:
                return UiBasedCollector.class;
            case CLFollow:
                return CareLinkFollowService.class;
            default:
                return DexCollectionService.class;
        }
    }

    // using reflection to access static methods, could cache if needed maybe

    public static Boolean getServiceRunningState() {
        final Boolean result = getPhoneServiceRunningState();
        // if phone running don't bother checking wear
        if ((result != null) && result) return true;
        return getWatchServiceRunningState();
    }

    public static Boolean getPhoneServiceRunningState() {
        try {
            // TODO handle wear collection
            final Method method = getCollectorServiceClass().getMethod("isRunning");
            return (Boolean) method.invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean getLocalServiceCollectingState() {
        try {
            final Method method = getCollectorServiceClass().getMethod("isCollecting");
            return (boolean) method.invoke(null);
        } catch (Exception e) {
            return false; // default to not blocking a restart
        }
    }


    public static Boolean getWatchServiceRunningState() {
        if (Pref.getBooleanDefaultFalse("wear_sync") &&
                Pref.getBooleanDefaultFalse("enable_wearG5")) {
            try {
                final Method method = getCollectorServiceClass().getMethod("isWatchRunning");
                return (Boolean) method.invoke(null);
            } catch (Exception e) {
                return null; // probably method not found
            }
        } else {
            return false; // hopefully this is sufficient to know that the service is definitely not running
        }
    }

    public static String getBestCollectorHardwareName() {
        final DexCollectionType dct = getDexCollectionType();
        switch (dct) {
            case NSEmulator:
            case AidexReceiver:
            case LibreReceiver:
                return "Other App";
            case WifiWixel:
                return "Network G4";
            case LimiTTer:
                return DexCollectionService.getBestLimitterHardwareName();
            case LimiTTerWifi:
                return "Network " + DexCollectionService.getBestLimitterHardwareName();
            case WifiDexBridgeWixel:
                return "Network G4 and xBridge";
            case WifiBlueToothWixel:
                return "Network G4 and Classic xDrip";
            case DexcomG5:
                if (Ob1G5CollectionService.usingNativeMode()) {
                    return Ob1G5CollectionService.usingG6() ? (shortTxId() ? "G7" : "G6 Native") : "G5 Native";
                }
                return dct.name();
            case LibreWifi:
                return "Network libre";
            case NSFollow:
                return "Nightscout";
            case SHFollow:
                return "Share";
            case UiBased:
                return "UI Based";

            case CLFollow:
                return "CareLink";
            default:
                return dct.name();
        }
    }

    public static int getBestBridgeBatteryPercent() {
        if (DexCollectionType.hasBattery()) {
            final DexCollectionType dct = getDexCollectionType();
            // TODO this logic needs double checking for multi collector types and others
            switch (dct) {
                default:
                    return Pref.getInt("bridge_battery", -1);
            }
        } else if (DexCollectionType.hasWifi()) {
            return Pref.getInt("parakeet_battery", -3);
        } else {
            return -2;
        }
    }

    public static String getBestBridgeBatteryPercentString() {
        final int battery = getBestBridgeBatteryPercent();
        if (battery > 0) {
            return "" + battery;
        } else {
            return "";
        }
    }

    public boolean isPassive() {
        return isPassive.contains(this);
    }

    public long getSamplePeriod() {
        return getCollectorSamplePeriod(this);
    }

    private static final boolean libreOneMinute = Pref.getBooleanDefaultFalse("libre_one_minute")
            && Pref.getBooleanDefaultFalse("engineering_mode");

    public static long getCollectorSamplePeriod(final DexCollectionType type) {
        switch (type) {
            case LibreReceiver:
                return libreOneMinute ? 60_000 : 300_000;
            default:
                return 300_000; // 5 minutes
        }
    }

    public static long getCurrentSamplePeriod() {
        return getDexCollectionType().getSamplePeriod();
    }

    public static long getCurrentDeduplicationPeriod() {
        final long period = getDexCollectionType().getSamplePeriod();
        return period - (period / 6); // TODO this needs more validation
    }

    public static int getCurrentSamplesForPeriod(final long periodMs) {
        return (int) (periodMs / getDexCollectionType().getSamplePeriod());
    }


}
