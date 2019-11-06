package com.eveningoutpost.dexdrip.watch.miband.message;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.watch.miband.Const;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.DISPLAY_ITEM_BIT_CLOCK;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.ENDPOINT_DISPLAY_ITEMS;

public class DisplayControllMessage extends BaseMessage {
    private static final String TAG = "MiBand_DisplayControllMessage";

    public static final int DISPLAY_ITEM_STEPS = 0;
    public static final int DISPLAY_ITEM_DISTANCE = 1;
    public static final int DISPLAY_ITEM_CALORIES = 2;
    public static final int DISPLAY_ITEM_HEART_RATE = 3;
    public static final int DISPLAY_ITEM_BATTERY = 5;

    //0a 01 00 00 01 02 03 04 05  disabled all screens
    //0a 11 00 00 01 02 03 04 05  heart rate

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_3_CONFIGURATION;
    }

    public byte[] getDisplayItemsMessage(List<Integer> pages) {
        UserError.Log.e(TAG, "Setting display items to " + ((pages == null && pages.isEmpty()) ? "none" : pages));
        byte[] data = OperationCodes.COMMAND_CHANGE_SCREENS.clone();

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
