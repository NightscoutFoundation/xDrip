package com.eveningoutpost.dexdrip.ui;

/**
 * Created by jamorham on 29/09/2017.
 */

import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getBestCollectorHardwareName;

import androidx.databinding.BaseObservable;

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
        return DexCollectionType.hasBluetooth() && !getBestCollectorHardwareName().equals("G7");
    }

    @Override
    public boolean sessionStartTime() { // This is false only if we are using G7
        return !getBestCollectorHardwareName().equals("G7");
    }

    @Override
    public boolean xmitterBattery() {
        return DexCollectionType.usesClassicTransmitterBattery();
    }

}
