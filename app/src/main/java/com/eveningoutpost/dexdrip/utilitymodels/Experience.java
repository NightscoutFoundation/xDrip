package com.eveningoutpost.dexdrip.utilitymodels;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import androidx.annotation.StringRes;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utils.Preferences;
import com.eveningoutpost.dexdrip.utils.SdcardImportExport;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.util.Locale;

/**
 * Created by jamorham on 22/01/2017.
 * <p>
 * Gauge a user's experience level
 */

public class Experience {

    private static final String TAG = "xdrip-Experience";
    private static final String MARKER = "xdrip-plus-installed-time";

    // caches
    private static boolean got_data = false;
    private static boolean not_newbie = false;

    private static STEP_STATE stepState = null;

    // only call this once
    public static boolean isNewbie() {
        if (not_newbie) return false;
        final long installed_time = Pref.getLong(MARKER, -1);
        if (installed_time > 0) {
            UserError.Log.d(TAG, "First Installed " + JoH.niceTimeSince(installed_time) + " ago");
            not_newbie = true;
            return false;
        } else {
            // probably newbie

            if (gotData()) return false;
            UserError.Log.d(TAG, "Looks like a Newbie");
            return true;
        }
    }

    public static void newbieSetupComplete() {
        Pref.setLong(MARKER, JoH.tsl());
    }

    public static boolean installedForAtLeast(long millis) {
        final long installed_time = Pref.getLong(MARKER, -1);
        return installed_time > 0 && (JoH.msSince(installed_time) > millis);
    }

    public static boolean ageOfThisBuildAtLeast(long millis) {
        return JoH.msSince(BuildConfig.buildTimestamp) > millis;
    }

    public static boolean gotData() {
        if (got_data) return true;
        if (BgReading.last(true) != null) {
            got_data = true;
            return true;
        } else {
            return false;
        }
    }

    public static boolean backupAvailable() {
        final String backup_file = Pref.getString("last-saved-database-zip", "");
        if (backup_file.length() > 0) {
            if (new File(backup_file).exists()) {
                return true;
            }
        }
        return false;
    }


    private static final ImmutableSet<String> mmol_countries = ImmutableSet.of("AU", "CA", "CN", "HK", "MO", "TW", "HR", "CZ", "DE", "DK", "FI", "HK", "HU", "IS", "IE", "JM", "KZ", "YK", "LV", "LT", "MY", "MT", "NL", "AN", "NZ", "NO", "RU", "SK", "SI", "ZA", "SE", "CH", "GB");

    private static String defaultUnits() {
        try {
            final String country = Locale.getDefault().getCountry();
            final String units = mmol_countries.contains(country) ? "mmol/l" : "mg/dl";
            UserError.Log.d(TAG, "Country: " + country + " default units: " + units);
            return units;
        } catch (Exception e) {
            UserError.Log.e(TAG, "Exception trying to determine locale units: " + e);
            return "mg/dl";
        }
    }

    public static boolean defaultUnitsAreMmol() {
        return defaultUnits().equals("mmol/l");
    }

    // return true when done
    public static boolean processSteps(final Activity activity) {

        if (stepState == null) advanceStep();
        UserError.Log.d(TAG, "Step: " + stepState);
        switch (stepState) {
            case CHECK_BACKUP:
                if (!SdcardImportExport.handleBackup(activity)) {
                    skipStep(activity);
                }
                break;
            case CHECK_UNITS:
                if (defaultUnitsAreMmol() && Unitized.usingMgDl()) {

                    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(R.string.glucose_units_mmol_or_mgdl);
                    builder.setMessage(R.string.is_your_typical_glucose_value);

                    builder.setNegativeButton("5.5", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            advanceStep();
                            dialog.dismiss();
                            Pref.setString("units", "mmol");
                            Preferences.handleUnitsChange(null, "mmol", null);
                            Home.staticRefreshBGCharts();
                            JoH.static_toast_long(getString(R.string.settings_updated_to_mmol));

                        }
                    });

                    builder.setPositiveButton("100", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            advanceStep();
                            Home.staticRefreshBGCharts();
                            dialog.dismiss();
                        }
                    });

                    ((Home) activity).dialog = builder.create();
                    ((Home) activity).dialog.show();
                    break;

                } else {
                    // no change needed
                    skipStep(activity);
                }
                break;
            case DONE:
                newbieSetupComplete();
                return true;

            default:
                return true;
        }
        return false;
    }

    private static String getString(@StringRes final int id) {
        return xdrip.gs(id);
    }

    public static void skipStep(final Activity activity) {
        advanceStep();
        processSteps(activity);
    }

    public static void advanceStep() {
        stepState = STEP_STATE.next(stepState);
    }


    enum STEP_STATE {
        CHECK_BACKUP,
        CHECK_UNITS,
        DONE;


        STEP_STATE next() {
            return next(this);
        }

        static STEP_STATE next(STEP_STATE current) {
            STEP_STATE previous = null;
            for (STEP_STATE step : STEP_STATE.values()) {
                if ((current == null) || current.equals(previous)) return step;
                previous = step;
            }
            return null;
        }
    }

}
