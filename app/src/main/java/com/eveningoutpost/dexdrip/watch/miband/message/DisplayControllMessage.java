package com.eveningoutpost.dexdrip.watch.miband.message;

import com.eveningoutpost.dexdrip.watch.miband.Const;

import java.util.List;
import java.util.UUID;

public abstract class DisplayControllMessage extends BaseMessage {
    private static final String TAG = DisplayControllMessage.class.getSimpleName();

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_3_CONFIGURATION;
    }

    public abstract  byte[] getDisplayItemsCmd(List<Integer> pages);
}
