package com.eveningoutpost.dexdrip.watch.miband;

import com.eveningoutpost.dexdrip.watch.PrefBinding;
import com.eveningoutpost.dexdrip.watch.miband.message.DisplayControllMessageMiband3_4;
import com.eveningoutpost.dexdrip.watch.miband.message.FeaturesControllMessage;

public class Miband3_4PrefBinding extends PrefBinding {
    @Override
    public void initialize() {
        add("miband_screen_notifications", DisplayControllMessageMiband3_4.DISPLAY_ITEM_NOTIFICATIONS);
        add("miband_screen_weather", DisplayControllMessageMiband3_4.DISPLAY_ITEM_WEATHER);
        add("miband_screen_activity", DisplayControllMessageMiband3_4.DISPLAY_ITEM_ACTIVITY);
        add("miband_screen_more", DisplayControllMessageMiband3_4.DISPLAY_ITEM_MORE);
        add("miband_screen_status", DisplayControllMessageMiband3_4.DISPLAY_ITEM_STATUS);
        add("miband_screen_heart_rate", DisplayControllMessageMiband3_4.DISPLAY_ITEM_HEART_RATE);
        add("miband_screen_timer", DisplayControllMessageMiband3_4.DISPLAY_ITEM_TIMER);
        add("miband_screen_nfc", DisplayControllMessageMiband3_4.DISPLAY_ITEM_NFC);

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
