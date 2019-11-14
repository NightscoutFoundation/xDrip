package com.eveningoutpost.dexdrip.watch.miband;

import com.eveningoutpost.dexdrip.watch.PrefBinding;
import com.eveningoutpost.dexdrip.watch.miband.message.DisplayControllMessageMiBand2;
import com.eveningoutpost.dexdrip.watch.miband.message.FeaturesControllMessage;

public class Miband2PrefBinding extends PrefBinding {
    @Override
    public void initialize() {
        add("miband_screen_step_counter", DisplayControllMessageMiBand2.DISPLAY_ITEM_STEPS);
        add("miband_screen_step_distance", DisplayControllMessageMiBand2.DISPLAY_ITEM_DISTANCE);
        add("miband_screen_step_calories", DisplayControllMessageMiBand2.DISPLAY_ITEM_CALORIES);
        add("miband_screen_heart_rate", DisplayControllMessageMiBand2.DISPLAY_ITEM_HEART_RATE);
        add("miband_screen_battery", DisplayControllMessageMiBand2.DISPLAY_ITEM_BATTERY);

        add("miband_feature_lift_to_wake", FeaturesControllMessage.FEATURE_DISPLAY_ON_LIFT_WRIST);
        add("miband_feature_anti_lost", FeaturesControllMessage.FEATURE_ANTI_LOST);
        add("miband_feature_locale_24_hour", FeaturesControllMessage.FEATURE_CLOCK_FORMAT);
        add("miband_feature_show_date", FeaturesControllMessage.FEATURE_SHOW_DATE);
        add("miband_feature_switch_display_on_wrist", FeaturesControllMessage.FEATURE_SWITCH_DISPLAY_ON_LIFT_WRIST);
        add("miband_feature_units", FeaturesControllMessage.FEATURE_UNITS);
        add("miband_feature_goal_notification", FeaturesControllMessage.FEATURE_GOAL_NOTIFICATION);
        add("miband_feature_visibility", FeaturesControllMessage.FEATURE_VISISBILITY);

    }
}
