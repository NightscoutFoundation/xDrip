package com.eveningoutpost.dexdrip.ui;

/**
 * Created by jamorham on 29/09/2017.
 */

import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.shortTxId;

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
        return DexCollectionType.hasBluetooth() && !usingG7();
    }

    @Override
    public boolean xmitterBattery() {
        return DexCollectionType.usesClassicTransmitterBattery();
    }

    @Override
    public boolean usingG7() {
        try { // True if we are using G7
            return DexCollectionType.getDexCollectionType() == DexCollectionType.DexcomG5 && Pref.getBooleanDefaultFalse("using_g6") && shortTxId();
        } catch (Exception e) { // If there are any unknowns, show that we are not using G7
            return false;
        }
    }
}
