package com.eveningoutpost.dexdrip.UtilityModels;

// jamorham

// TODO check this reference handling

import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble.RxBleClient;

public class RxBleProvider
{
    private static volatile RxBleClient rxBleClient;

    public static synchronized RxBleClient getSingleton() {
        if (rxBleClient == null) {
            rxBleClient = RxBleClient.create(xdrip.getAppContext());
        }
        return rxBleClient;
    }

}
