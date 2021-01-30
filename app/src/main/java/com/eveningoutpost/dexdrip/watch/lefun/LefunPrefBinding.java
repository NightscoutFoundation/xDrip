package com.eveningoutpost.dexdrip.watch.lefun;

import com.eveningoutpost.dexdrip.watch.PrefBinding;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxSetFeatures;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxSetLocaleFeature;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxSetScreens;

public class LefunPrefBinding extends PrefBinding {
    @Override
    public void initialize() {
        add("lefun_screen_step_counter", TxSetScreens.STEP_COUNTER);
        add("lefun_screen_step_distance", TxSetScreens.STEP_DISTANCE);
        add("lefun_screen_step_calories", TxSetScreens.STEP_CALORIES);
        add("lefun_screen_heart_rate", TxSetScreens.HEART_RATE);
        add("lefun_screen_heart_pressure", TxSetScreens.HEART_PRESSURE);
        add("lefun_screen_find_phone", TxSetScreens.FIND_PHONE);
        add("lefun_screen_mac_address", TxSetScreens.MAC_ADDRESS);

        add("lefun_feature_lift_to_wake", TxSetFeatures.LIFT_TO_WAKE);
        //  add("lefun_feature_sedentary",TxSetFeatures.SEDENTARY_REMINDER);
        //  add("lefun_feature_drinking",TxSetFeatures.DRINKING_REMINDER);
        add("lefun_feature_camera", TxSetFeatures.CAMERA);
        add("lefun_feature_anti_lost", TxSetFeatures.ANTI_LOST);

        add("lefun_locale_12_hour", TxSetLocaleFeature.CLOCK_FORMAT_12_HOUR);
    }
}
