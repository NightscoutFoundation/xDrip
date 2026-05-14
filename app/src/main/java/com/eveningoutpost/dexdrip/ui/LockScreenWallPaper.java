package com.eveningoutpost.dexdrip.ui;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.preference.Preference;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.ColorCache;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.ui.activities.NumberWallPreview;
import com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil;
import com.eveningoutpost.dexdrip.utils.time.TimeRangeUtils;
import com.eveningoutpost.dexdrip.xdrip;

import static android.app.WallpaperManager.FLAG_LOCK;
import static com.eveningoutpost.dexdrip.ui.NumberGraphic.isLockScreenBitmapTiled;
import static com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil.getScreenHeight;
import static com.eveningoutpost.dexdrip.ui.helpers.BitmapUtil.getScreenWidth;
import static com.eveningoutpost.dexdrip.utilitymodels.ColorCache.getCol;

/**
 * jamorham
 *
 * Show numbers on Lock Screen wall paper
 * only works on Android 7 or above
 */

public class LockScreenWallPaper {

    private static final String TAG = "WallPaper";
    private static final String PREF_ENABLED = "number_wall_on_lockscreen";
    private static final String PREF_FORCE_TEXT_COLOR = "force_lock_screen_text_color";
    private static final String PREF_TEXT_COLOR = "color_number_wall";
    private static final String PREF_TEXT_SHADOW_COLOR = "color_number_wall_shadow";

    private static final String PREF_TIME_START = "number_wall_start_time";
    private static final String PREF_TIME_STOP = "number_wall_stop_time";
    private static int textColor = getCol(ColorCache.X.color_number_wall);

    private static long lastPoll;

    // check/update if not done so recently
    public static void timerPoll() {
        if (JoH.msSince(lastPoll) > Constants.MINUTE_IN_MS * 10) {
            setIfEnabled();
        }
    }

    // if feature is enabled then set() the screen
    public static void setIfEnabled() {
        lastPoll = JoH.tsl();
        if (isEnabled()) {
            set();
        }
    }

    // set the graphic or set blank/image background if out of time period or data not good
    private static void set() {
        if (TimeRangeUtils.areWeInTimePeriod("number_wall")) {
            final BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
            if (dg != null && !dg.isReallyStale()) {
                if (Pref.getBoolean("force_lock_screen_text_color", true)) {
                    // Always use user-chosen color
                    textColor = getCol(ColorCache.X.color_number_wall);
                } else {
                    // Dynamic color based on glucose value
                    if (dg.isHigh()) {
                        textColor = getCol(ColorCache.X.color_high_bg_values);
                    } else if (dg.isLow()) {
                        textColor = getCol(ColorCache.X.color_low_bg_values);
                    } else {
                        textColor = getCol(ColorCache.X.color_inrange_bg_values);
                    }
                }
                final Bitmap bitmap = NumberGraphic.getLockScreenBitmap(dg.unitized, dg.delta_arrow, dg.isStale(), textColor);
                setBitmap(bitmap);
            } else {
                // data too stale
                setBitmap(null);
            }
        } else {
            // not time now
            setBitmap(null);
        }

    }

    public static int getTextColor() {
        return textColor;
    }

    public static void handlePreference() {
        if (!isEnabled()) {
            disable();
        } else {
            set();
        }
    }

    private static void disable() {
        setBitmap(null);
    }


    @SuppressLint("WrongConstant")
    public static void setBitmap(final Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                final WallpaperManager wallpaperManager = WallpaperManager.getInstance(xdrip.getAppContext());
                final Bitmap wallpaper = BitmapUtil.getTiled(bitmap, getScreenWidth(), getScreenHeight(), isLockScreenBitmapTiled(), Pref.getString(NumberWallPreview.ViewModel.PREF_numberwall_background, null));
                wallpaperManager.setBitmap(wallpaper, null, false, FLAG_LOCK);
                wallpaper.recycle();
            } catch (Exception e) {
                UserError.Log.e(TAG, "Failed to set wallpaper: " + e);
            }
        }
    }

    private static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse(PREF_ENABLED);
    }


    public static class PrefListener {

        private Preference summaryPreference;

        public void setSummaryPreference(Preference pref) {
            summaryPreference = pref;
            updateSummary();

        }

        public final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
            switch (key) {
                case PREF_ENABLED:
                case PREF_FORCE_TEXT_COLOR:
                case PREF_TEXT_COLOR:
                case PREF_TEXT_SHADOW_COLOR:
                    hardRefreshFromPreferenceChange();
                    break;

                case PREF_TIME_START:
                case PREF_TIME_STOP:
                    updateSummary();
                    hardRefreshFromPreferenceChange();
                    break;
            }
        };

        private static void hardRefreshFromPreferenceChange() {
            Inevitable.task("number-wall-handle-pref", 500, LockScreenWallPaper::handlePreference);

        }

        private void updateSummary() {
            if (summaryPreference != null) {
                summaryPreference.setTitle(summaryPreference.getTitle().toString().replaceAll("  \\(.+\\)$", "") + "  (" + TimeRangeUtils.getNiceStartStopString("number_wall") + ")");
            }
            Inevitable.task("update-number-wall", 1000, () -> setIfEnabled());
        }

    }
}
