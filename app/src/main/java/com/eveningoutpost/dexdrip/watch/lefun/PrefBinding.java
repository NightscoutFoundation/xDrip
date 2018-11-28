package com.eveningoutpost.dexdrip.watch.lefun;

// jamorham

import android.util.Pair;

import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxSetFeatures;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxSetLocaleFeature;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxSetScreens;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class PrefBinding {

    private static PrefBinding instance;

    @Getter
    private final List<Pair<String, Integer>> items = new ArrayList<>();

    private PrefBinding() {

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

    public static PrefBinding getInstance() {
        if (instance == null) {
            instance = new PrefBinding();
        }
        return instance;
    }

    private void add(final String pref, final int value) {
        items.add(new Pair<>(pref, value));
    }

    public List<Integer> getEnabled(final String prefix) {
        final List<Integer> results = new ArrayList<>();
        for (Pair<String, Integer> pair : items) {
            if (pair.first.startsWith(prefix)) {
                if (Pref.getBooleanDefaultFalse(pair.first)) {
                    results.add(pair.second);
                }
            }
        }
        return results;
    }

    public List<Pair<Integer, Boolean>> getStates(final String prefix) {
        final List<Pair<Integer, Boolean>> results = new ArrayList<>();
        for (Pair<String, Integer> pair : items) {
            if (pair.first.startsWith(prefix)) {
                results.add(new Pair<>(pair.second, Pref.getBooleanDefaultFalse(pair.first)));
            }
        }
        return results;
    }


}
