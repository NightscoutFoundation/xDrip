package com.eveningoutpost.dexdrip.ui;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil;
import com.eveningoutpost.dexdrip.xdrip;

import static android.app.WallpaperManager.FLAG_LOCK;
import static com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil.getScreenHeight;
import static com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil.getScreenWidth;

/**
 * jamorham
 *
 * Show numbers on Lock Screen wall paper
 * only works on Android 7 or above
 */

public class LockScreenWallPaper {

    private static final String TAG = "WallPaper";
    private static final String PREF = "number_wall_on_lockscreen";

    public static void setIfEnabled() {
        if (isEnabled()) {
            set();
        }
    }

    public static void set() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
            if (dg != null && !dg.isReallyStale()) {
                final Bitmap bitmap = NumberGraphic.getLockScreenBitmapWhite(dg.unitized, dg.delta_arrow, dg.isStale());
                setBitmap(bitmap);
            } else {
                setBitmap(null);
            }
        }
    }

    public static void handlePreference() {
        if (!isEnabled()) {
            disable();
        } else {
            set();
        }
    }

    public static void disable() {
        setBitmap(null);
    }



    @SuppressLint("WrongConstant")
    public static void setBitmap(final Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                final WallpaperManager wallpaperManager = WallpaperManager.getInstance(xdrip.getAppContext());
                wallpaperManager.setBitmap(BitmapUtil.getTiled(bitmap, getScreenWidth(), getScreenHeight()), null, false, FLAG_LOCK);
            } catch (Exception e) {
                UserError.Log.e(TAG, "Failed to set wallpaper: " + e);
            }
        }
    }

    private static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse(PREF);
    }

    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        switch (key) {
            case PREF:
                Inevitable.task("number-wall-handle-pref",300, LockScreenWallPaper::handlePreference);
                break;
        }
    };

}
