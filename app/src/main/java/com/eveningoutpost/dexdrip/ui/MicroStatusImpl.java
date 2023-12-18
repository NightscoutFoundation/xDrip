package com.eveningoutpost.dexdrip.ui;

/**
 * Created by jamorham on 29/09/2017.
 */

import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.getTransmitterID;

import androidx.databinding.BaseObservable;

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
    public boolean bluetooth() { // Dexcom with Bluetooth except G7
        return DexCollectionType.hasBluetooth() && !using_g7();
    }

    @Override
    public boolean xmitterBattery() {
        return DexCollectionType.usesClassicTransmitterBattery();
    }

    @Override
    public boolean using_g7() { // True if we are using G7
        if (DexCollectionType.getDexCollectionType() == DexCollectionType.DexcomG5 && Pref.getBooleanDefaultFalse("using_g6") && getTransmitterID().length() < 6) { // If using G7
            return true;
        }
        return false;
    }
}
