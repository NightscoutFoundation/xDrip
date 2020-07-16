package com.eveningoutpost.dexdrip.watch.miband.message;

import com.eveningoutpost.dexdrip.watch.miband.Const;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.COMMAND_NIGHT_MODE_OFF;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.COMMAND_NIGHT_MODE_SCHEDULED;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.COMMAND_NIGHT_MODE_SUNSET;

public class DisplayControllMessageMiband3_4 extends DisplayControllMessage {
    private static final String TAG = DisplayControllMessageMiband3_4.class.getSimpleName();

    public enum NightMode {
        Off,
        Sunset,
        Sheduled;
    }

    public static final int DISPLAY_ITEM_NOTIFICATIONS = 0;
    public static final int DISPLAY_ITEM_WEATHER = 1;
    public static final int DISPLAY_ITEM_ACTIVITY = 2;
    public static final int DISPLAY_ITEM_MORE = 3;
    public static final int DISPLAY_ITEM_STATUS = 4;
    public static final int DISPLAY_ITEM_HEART_RATE = 5;
    public static final int DISPLAY_ITEM_TIMER = 6;
    public static final int DISPLAY_ITEM_NFC = 7;

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_3_CONFIGURATION;
    }

    public byte[] getDisplayItemsCmd(List<Integer> pages) {
        byte[] data = OperationCodes.COMMAND_CHANGE_SCREENS_MIDAND3_4.clone();

        byte pos = 1;
        if (pages != null && !pages.isEmpty()) {
            if (pages.contains(DISPLAY_ITEM_NOTIFICATIONS)) {
                data[1] |= 0x02;
                data[4] = pos++;
            }
            if (pages.contains(DISPLAY_ITEM_WEATHER)) {
                data[1] |= 0x04;
                data[5] = pos++;
            }
            if (pages.contains(DISPLAY_ITEM_ACTIVITY)) {
                data[1] |= 0x08;
                data[6] = pos++;
            }
            if (pages.contains(DISPLAY_ITEM_MORE)) {
                data[1] |= 0x10;
                data[7] = pos++;
            }
            if (pages.contains(DISPLAY_ITEM_STATUS)) {
                data[1] |= 0x20;
                data[8] = pos++;
            }
            if (pages.contains(DISPLAY_ITEM_HEART_RATE)) {
                data[1] |= 0x40;
                data[9] = pos++;
            }
            if (pages.contains(DISPLAY_ITEM_TIMER)) {
                data[1] |= 0x80;
                data[10] = pos++;
            }
            if (pages.contains(DISPLAY_ITEM_NFC)) {
                data[2] |= 0x01;
                data[11] = pos++;
            }
        }
        for (int i = 4; i <= 11; i++) {
            if (data[i] == 0) {
                data[i] = pos++;
            }
        }
        init(data.length);
        putData(data);
        return getBytes();
    }

    public byte[] setNightModeCmd(NightMode nightMode, Date start, Date end) {
        byte[] data = null;
        switch (nightMode) {
            case Sunset:
                data = COMMAND_NIGHT_MODE_SUNSET;
                break;
            case Off:
                data = COMMAND_NIGHT_MODE_OFF;
                break;
            case Sheduled:
                data = COMMAND_NIGHT_MODE_SCHEDULED.clone();

                Calendar calendar = GregorianCalendar.getInstance();
                calendar.setTime(start);
                data[2] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                data[3] = (byte) calendar.get(Calendar.MINUTE);

                calendar.setTime(end);
                data[4] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                data[5] = (byte) calendar.get(Calendar.MINUTE);

                break;
        }
        return data;
    }

}
