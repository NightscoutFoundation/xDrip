package com.eveningoutpost.dexdrip.ui;

/**
 * Created by jamorham on 29/09/2017.
 */

import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.getTransmitterID;

import androidx.databinding.BaseObservable;

import com.eveningoutpost.dexdrip.g5model.FirmwareCapability;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

public class MicroStatusImpl extends BaseObservable implements MicroStatus {


    @Override
    public String gs(String id) {

        switch (id) {

            case "niceCollector":
                return DexCollectionType.getBestCollectorHardwareName();

            case "bestBridgeBattery":
                return DexCollectionType.getBestBridgeBatteryPercentString();

            default:
                return "Unknown id:" + id;
        }
    }

    @Override
    public boolean bluetooth() {
        return DexCollectionType.hasBluetooth();
    }

    @Override
    public boolean xmitterBattery() {
        return DexCollectionType.usesClassicTransmitterBattery();
    }

    @Override
    // This is false only for Dexcom G6 Firefly and G7 as of December 2023.
    // These devices have two common characteristics:
    // 1- xDrip does not maintain a calibration graph for them.  Therefore, resetting calibrations has no impact.
    // 2- They cannot be restarted easily or they cannot be restarted at all.
    public boolean resettableCals() { // Used on the stop sensor menu.
        if (DexCollectionType.getDexCollectionType() == DexCollectionType.DexcomG5 && Pref.getBooleanDefaultFalse("using_g6") && FirmwareCapability.isTransmitterRawIncapable(getTransmitterID())) {
            return false; // When calibrations are not resettable
        }
        return true; // When cals are resettable or when Firmware is not known yet
    }
}
