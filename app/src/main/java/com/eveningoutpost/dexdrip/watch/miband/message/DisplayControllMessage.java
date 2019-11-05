package com.eveningoutpost.dexdrip.watch.miband.message;

import com.eveningoutpost.dexdrip.watch.miband.Const;

import java.util.Set;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.DISPLAY_ITEM_BIT_CLOCK;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.ENDPOINT_DISPLAY_ITEMS;

public class DisplayControllMessage extends BaseMessage {


    public static final byte[] COMMAND_CHANGE_SCREENS = new byte[]{ENDPOINT_DISPLAY_ITEMS, DISPLAY_ITEM_BIT_CLOCK, 0x30, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00};

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_3_CONFIGURATION;
    }

    protected byte[] getDisplayItemsMessage( ) {/*
        Set<String> pages = HuamiCoordinator.getDisplayItems(gbDevice.getAddress());
        LOG.info("Setting display items to " + (pages == null ? "none" : pages));

        byte[] data = COMMAND_CHANGE_SCREENS.clone();

        if (pages != null) {
            if (pages.contains(MiBandConst.PREF_MI2_DISPLAY_ITEM_STEPS)) {
                data[OperationCodes.SCREEN_CHANGE_BYTE] |= OperationCodes.DISPLAY_ITEM_BIT_STEPS;
            }
            if (pages.contains(MiBandConst.PREF_MI2_DISPLAY_ITEM_DISTANCE)) {
                data[OperationCodes.SCREEN_CHANGE_BYTE] |= OperationCodes.DISPLAY_ITEM_BIT_DISTANCE;
            }
            if (pages.contains(MiBandConst.PREF_MI2_DISPLAY_ITEM_CALORIES)) {
                data[OperationCodes.SCREEN_CHANGE_BYTE] |= OperationCodes.DISPLAY_ITEM_BIT_CALORIES;
            }
            if (pages.contains(MiBandConst.PREF_MI2_DISPLAY_ITEM_HEART_RATE)) {
                data[OperationCodes.SCREEN_CHANGE_BYTE] |= OperationCodes.DISPLAY_ITEM_BIT_HEART_RATE;
            }
            if (pages.contains(MiBandConst.PREF_MI2_DISPLAY_ITEM_BATTERY)) {
                data[OperationCodes.SCREEN_CHANGE_BYTE] |= OperationCodes.DISPLAY_ITEM_BIT_BATTERY;
            }
        }

        builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), data);
        return this;*/
        return getBytes();
    }
}
