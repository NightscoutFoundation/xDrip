package com.eveningoutpost.dexdrip.ui.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.PrefsViewString;
import com.eveningoutpost.dexdrip.UtilityModels.Unitized;
import com.eveningoutpost.dexdrip.adapters.ObservableBackground;
import com.eveningoutpost.dexdrip.databinding.ActivityNumberWallPreviewBinding;
import com.eveningoutpost.dexdrip.ui.LockScreenWallPaper;
import com.eveningoutpost.dexdrip.ui.NumberGraphic;
import com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil;

import lombok.AllArgsConstructor;

import static com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil.getScreenHeight;
import static com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil.getScreenWidth;

/**
 * jamorham
 *
 * Configuration page for Number Wall feature
 *
 */

public class NumberWallPreview extends AppCompatActivity {

    private static final String TAG = "NumberWallPreview";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityNumberWallPreviewBinding binding = ActivityNumberWallPreviewBinding.inflate(getLayoutInflater());
        binding.setVm(new ViewModel());
        binding.setSprefs(new PrefsViewStringSnapDefaultsRefresh(binding.getVm()));
        setContentView(binding.getRoot());
        JoH.fixActionBar(this);
    }

    // bound view model
    public class ViewModel {

        public static final String PREF_numberwall_x_param = "numberwall_x_param";
        public static final String PREF_numberwall_y_param = "numberwall_y_param";
        public static final String PREF_numberwall_s_param = "numberwall_s_param";

        public ObservableBackground background = new ObservableBackground();
        {
            refreshBitmap();
        }

        // create demo bitmap
        public void refreshBitmap() {
            final BestGlucose.DisplayGlucose dg = new BestGlucose.DisplayGlucose();
            dg.delta_arrow = "\u21C5"; // ⇅
            dg.unitized = Unitized.usingMgDl() ? "123" : "12.3";
            final Bitmap bitmap = BitmapUtil.getTiled(NumberGraphic.getLockScreenBitmapWhite(dg.unitized, dg.delta_arrow, false), getScreenWidth(), getScreenHeight());
            background.setBitmap(bitmap);
            Inevitable.task("refresh-lock-number-wall", 500, LockScreenWallPaper::setIfEnabled);
        }
    }

    // transparent preferences binding with below minimum defaults snaps to default
    @AllArgsConstructor
    public class PrefsViewStringSnapDefaultsRefresh extends PrefsViewString {

        private final ViewModel model;

        @Override
        public String get(Object key) {
            final String original = super.get(key);
            String result = original;
            Integer value = 0;
            try {
                value = Integer.parseInt(result);
            } catch (NumberFormatException e) {
                //
            }
            switch ((String) key) {
                case ViewModel.PREF_numberwall_x_param:
                    if (value < 15) {
                        result = "15";
                    }
                    break;
                case ViewModel.PREF_numberwall_y_param:
                    if (value < 30) {
                        result = "30";
                    }
                    break;
                case ViewModel.PREF_numberwall_s_param:
                    if (value < 10) {
                        result = "10";
                    }
                    break;
            }

            if (!result.equals(original)) {
                UserError.Log.d(TAG, "Snapped: " + key + " " + original + " -> " + result);
                put((String) key, result);
            }
            return result;
        }

        @Override
        public String put(String key, String value) {
            super.put(key, value);
            model.refreshBitmap();
            return value;
        }
    }
}
