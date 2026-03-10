package com.eveningoutpost.dexdrip.g5model;

import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.getTransmitterID;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.DexcomG5;

import com.eveningoutpost.dexdrip.utils.DexCollectionType;

/**
 * Created by joeginley on 3/19/16.
 */
public class Transmitter {
    public String transmitterId = "";

    public Transmitter(String id) {
        transmitterId = id;
    }

    public void authenticate() {

    }

    // True if G6 is being used and the G6 transmitter ID has more than 6 characters and the last 2 are -1
    public static boolean isDexTxIdEndingWithDash1() {
        return DexCollectionType.getDexCollectionType() == DexcomG5 && getTransmitterID().length() > 6 && getTransmitterID().endsWith("-1");
    }

}
