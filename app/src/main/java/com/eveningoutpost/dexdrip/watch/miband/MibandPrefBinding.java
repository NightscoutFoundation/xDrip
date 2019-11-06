package com.eveningoutpost.dexdrip.watch.miband;

import com.eveningoutpost.dexdrip.watch.PrefBinding;
import com.eveningoutpost.dexdrip.watch.miband.message.DisplayControllMessage;

public class MibandPrefBinding extends PrefBinding {
    @Override
    public void initialize() {
        add("miband_screen_step_counter", DisplayControllMessage.DISPLAY_ITEM_STEPS);
        add("miband_screen_step_distance", DisplayControllMessage.DISPLAY_ITEM_DISTANCE);
        add("miband_screen_step_calories", DisplayControllMessage.DISPLAY_ITEM_CALORIES);
        add("miband_screen_heart_rate", DisplayControllMessage.DISPLAY_ITEM_STEPS);
        add("miband_screen_battery", DisplayControllMessage.DISPLAY_ITEM_BATTERY);
    }
}
