package com.eveningoutpost.dexdrip.ui;

/**
 * Created by jamorham on 29/09/2017.
 */

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
    public boolean bluetooth() {
        return DexCollectionType.hasBluetooth();
    }

    @Override
    public boolean xmitterBattery() {
        return DexCollectionType.usesClassicTransmitterBattery();
    }
}
