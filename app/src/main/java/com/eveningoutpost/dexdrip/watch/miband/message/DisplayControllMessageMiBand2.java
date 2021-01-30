package com.eveningoutpost.dexdrip.watch.miband.message;

import com.eveningoutpost.dexdrip.watch.miband.Const;

import java.util.List;
import java.util.UUID;

public class DisplayControllMessageMiBand2 extends DisplayControllMessage {
    private static final String TAG = DisplayControllMessageMiBand2.class.getSimpleName();

    public static final int DISPLAY_ITEM_STEPS = 0;
    public static final int DISPLAY_ITEM_DISTANCE = 1;
    public static final int DISPLAY_ITEM_CALORIES = 2;
    public static final int DISPLAY_ITEM_HEART_RATE = 3;
    public static final int DISPLAY_ITEM_BATTERY = 4;

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_3_CONFIGURATION;
    }

    public byte[] getDisplayItemsCmd(List<Integer> pages) {
        byte[] data = OperationCodes.COMMAND_CHANGE_SCREENS_MIBAND2.clone();

        if (pages != null && !pages.isEmpty()) {
            if (pages.contains(DISPLAY_ITEM_STEPS)) {
                data[OperationCodes.SCREEN_CHANGE_BYTE] |= OperationCodes.DISPLAY_ITEM_BIT_STEPS;
           }
            if (pages.contains(DISPLAY_ITEM_DISTANCE)) {
                data[OperationCodes.SCREEN_CHANGE_BYTE] |= OperationCodes.DISPLAY_ITEM_BIT_DISTANCE;
            }
            if (pages.contains(DISPLAY_ITEM_CALORIES)) {
                data[OperationCodes.SCREEN_CHANGE_BYTE] |= OperationCodes.DISPLAY_ITEM_BIT_CALORIES;
            }
            if (pages.contains(DISPLAY_ITEM_HEART_RATE)) {
                data[OperationCodes.SCREEN_CHANGE_BYTE] |= OperationCodes.DISPLAY_ITEM_BIT_HEART_RATE;
            }
            if (pages.contains(DISPLAY_ITEM_BATTERY)) {
                data[OperationCodes.SCREEN_CHANGE_BYTE] |= OperationCodes.DISPLAY_ITEM_BIT_BATTERY;
            }
        }
        init(data.length);
        putData(data);
        return getBytes();
    }
}
