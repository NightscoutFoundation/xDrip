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

/**
 * jamorham
 *
 * Show numbers on Lock Screen wall paper
 * only works on Android 7 or above
 */

public class LockScreenWallPaper {

    private static final String TAG = "WallPaper";
    private static final String PREF_ENABLED = "number_wall_on_lockscreen";

    private static final String PREF_TIME_START = "number_wall_start_time";
    private static final String PREF_TIME_STOP = "number_wall_stop_time";

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (TimeRangeUtils.areWeInTimePeriod("number_wall")) {
                final BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
                if (dg != null && !dg.isReallyStale()) {
                    final Bitmap bitmap = NumberGraphic.getLockScreenBitmap(dg.unitized, dg.delta_arrow, dg.isStale());
                    setBitmap(bitmap);
                } else {
                    // data too stale
                    setBitmap(null);
                }
            } else {
                // not time now
                setBitmap(null);
            }
        } else {
            if (JoH.ratelimit("lockscreen-wallpaper", 3600)) {
                UserError.Log.e(TAG, "Insufficient android version to set lockscreen image");
            }
        } // if sufficient android version
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
