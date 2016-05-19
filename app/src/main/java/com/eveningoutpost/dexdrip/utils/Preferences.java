package com.eveningoutpost.dexdrip.utils;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.text.InputFilter;
import android.text.TextUtils;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Profile;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.ParakeetHelper;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.MissedReadingService;
import com.eveningoutpost.dexdrip.Services.PlusSyncService;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.PebbleSync;
import com.eveningoutpost.dexdrip.UtilityModels.UpdateActivity;
import com.eveningoutpost.dexdrip.WidgetUpdateService;
import com.eveningoutpost.dexdrip.xDripWidget;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.nightscout.core.barcode.NSBarcodeConfig;

import net.tribe7.common.base.Joiner;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class Preferences extends PreferenceActivity {
    private static final String TAG = "jamorham PREFS";
    private static byte[] staticKey;
    private AllPrefsFragment preferenceFragment;

    private static Preference units_pref;
    private static String static_units;
    private static Preference profile_insulin_sensitivity_default;

    private void refreshFragments() {
        preferenceFragment = new AllPrefsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                preferenceFragment).commit();
    }


    public interface OnServiceTaskCompleted {
        void onTaskCompleted(byte[] result);
    }

    public class ServiceCallback implements OnServiceTaskCompleted {
        @Override
        public void onTaskCompleted(byte[] result) {
            if (result.length > 0) {
                if ((staticKey == null) || (staticKey.length != 16)) {
                    toast("Error processing security key");
                } else {
                    byte[] plainbytes = JoH.decompressBytesToBytes(CipherUtils.decryptBytes(result, staticKey));
                    staticKey=null;
                    Log.d(TAG, "Plain bytes size: " + plainbytes.length);
                    if (plainbytes.length > 0) {
                        SdcardImportExport.storePreferencesFromBytes(plainbytes, getApplicationContext());
                    } else {
                        toast("Error processing data - empty");
                    }
                }
            } else {
                toast("Error processing settings - no data - try again?");
            }
        }
    }


    private void toast(final String msg) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
            android.util.Log.d(TAG, "Toast msg: " + msg);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Couldn't display toast: " + msg);
        }
    }

    private void installxDripPlusPreferencesFromQRCode(SharedPreferences prefs, String data) {
        Log.d(TAG, "installing preferences from QRcode");
        try {
            Map<String, String> prefsmap = DisplayQRCode.decodeString(data);
            if (prefsmap != null) {
                if (prefsmap.containsKey(getString(R.string.all_settings_wizard))) {
                    if (prefsmap.containsKey(getString(R.string.wizard_key))
                            && prefsmap.containsKey(getString(R.string.wizard_uuid))) {
                        staticKey = CipherUtils.hexToBytes(prefsmap.get(getString(R.string.wizard_key)));

                        new WebAppHelper(new ServiceCallback()).executeOnExecutor(xdrip.executor,getString(R.string.wserviceurl) + "/joh-getsw/" + prefsmap.get(getString(R.string.wizard_uuid)));
                    } else {
                        Log.d(TAG, "Incorrectly formatted wizard pref");
                    }
                    return;
                }

                SharedPreferences.Editor editor = prefs.edit();
                int changes = 0;
                for (Map.Entry<String, String> entry : prefsmap.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    //            Log.d(TAG, "Saving preferences: " + key + " = " + value);
                    if (value.equals("true") || (value.equals("false"))) {
                        editor.putBoolean(key, Boolean.parseBoolean(value));
                        changes++;
                    } else if (!value.equals("null")) {
                        editor.putString(key, value);
                        changes++;
                    }
                }
                editor.apply();
                refreshFragments();
                Toast.makeText(getApplicationContext(), "Loaded " + Integer.toString(changes) + " preferences from QR code", Toast.LENGTH_LONG).show();
                PlusSyncService.clearandRestartSyncService(getApplicationContext());
                if (prefs.getString("dex_collection_method", "").equals("Follower")) {
                    PlusSyncService.clearandRestartSyncService(getApplicationContext());
                    GcmActivity.requestBGsync();
                }
            } else {
                android.util.Log.e(TAG, "Got null prefsmap during decode");
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception installing preferences");
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (scanResult == null || scanResult.getContents() == null) {
            return;
        }
        if (scanResult.getFormatName().equals("QR_CODE")) {

            String scanresults = scanResult.getContents();
            if (scanresults.startsWith(DisplayQRCode.qrmarker)) {
                installxDripPlusPreferencesFromQRCode(prefs, scanresults);
            }

            NSBarcodeConfig barcode = new NSBarcodeConfig(scanresults);
            if (barcode.hasMongoConfig()) {
                if (barcode.getMongoUri().isPresent()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("cloud_storage_mongodb_uri", barcode.getMongoUri().get());
                    editor.putString("cloud_storage_mongodb_collection", barcode.getMongoCollection().or("entries"));
                    editor.putString("cloud_storage_mongodb_device_status_collection", barcode.getMongoDeviceStatusCollection().or("devicestatus"));
                    editor.putBoolean("cloud_storage_mongodb_enable", true);
                    editor.apply();
                }
                if (barcode.hasApiConfig()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("cloud_storage_api_enable", true);
                    editor.putString("cloud_storage_api_base", Joiner.on(' ').join(barcode.getApiUris()));
                    editor.apply();
                } else {
                    prefs.edit().putBoolean("cloud_storage_api_enable", false).apply();
                }
            }
            if (barcode.hasApiConfig()) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("cloud_storage_api_enable", true);
                editor.putString("cloud_storage_api_base", Joiner.on(' ').join(barcode.getApiUris()));
                editor.apply();
            } else {
                prefs.edit().putBoolean("cloud_storage_api_enable", false).apply();
            }

            if (barcode.hasMqttConfig()) {
                if (barcode.getMqttUri().isPresent()) {
                    URI uri = URI.create(barcode.getMqttUri().or(""));
                    if (uri.getUserInfo() != null) {
                        String[] userInfo = uri.getUserInfo().split(":");
                        if (userInfo.length == 2) {
                            String endpoint = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
                            if (userInfo[0].length() > 0 && userInfo[1].length() > 0) {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("cloud_storage_mqtt_endpoint", endpoint);
                                editor.putString("cloud_storage_mqtt_user", userInfo[0]);
                                editor.putString("cloud_storage_mqtt_password", userInfo[1]);
                                editor.putBoolean("cloud_storage_mqtt_enable", true);
                                editor.apply();
                            }
                        }
                    }
                }
            } else {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("cloud_storage_mqtt_enable", false);
                editor.apply();
            }
        } else if (scanResult.getFormatName().equals("CODE_128")) {
            Log.d(TAG, "Setting serial number to: " + scanResult.getContents());
            prefs.edit().putString("share_key", scanResult.getContents()).apply();
        }
        refreshFragments();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            setTheme(R.style.OldAppTheme);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set theme");
        }
        super.onCreate(savedInstanceState);
        preferenceFragment = new AllPrefsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                preferenceFragment).commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        addPreferencesFromResource(R.xml.pref_general);

    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (AllPrefsFragment.class.getName().equals(fragmentName)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };
    private static Preference.OnPreferenceChangeListener sBindNumericPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (isNumeric(stringValue)) {
                preference.setSummary(stringValue);
                return true;
            }
            return false;
        }
    };
    private static Preference.OnPreferenceChangeListener sBindPreferenceTitleAppendToValueListenerUpdateChannel = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {

            boolean do_update = false;
            // detect not first run
            if (preference.getTitle().toString().contains("("))
            {
                do_update=true;
            }

            preference.setTitle(preference.getTitle().toString().replaceAll("  \\([a-z0-9A-Z]+\\)$", "") + "  (" + value.toString() + ")");
            if (do_update) {
                preference.getEditor().putString(preference.getKey(),value.toString()).apply(); // update prefs now
                UpdateActivity.last_check_time = 0;
                UpdateActivity.checkForAnUpdate(preference.getContext());
            }
            return true;
        }
    };

    private static String format_carb_ratio(String oldValue, String newValue) {
        return oldValue.replaceAll(" \\(.*\\)$", "") + "  (" + newValue + "g per Unit)";
    }

    private static String format_carb_absorption_rate(String oldValue, String newValue) {
        return oldValue.replaceAll(" \\(.*\\)$", "") + "  (" + newValue + "g per hour)";
    }

    private static String format_insulin_sensitivity(String oldValue, String newValue) {
        try {
            return oldValue.replaceAll("  \\(.*\\)$", "") + "  (" + newValue + " " + static_units + " per U)";
        } catch (Exception e) {
            return "ERROR - Invalid number";
        }
    }

    private static void do_format_insulin_sensitivity(Preference preference, SharedPreferences prefs, boolean from_change, String newValue) {
        if (newValue == null) {
            newValue = prefs.getString("profile_insulin_sensitivity_default", "54");
        }
        try {
            Profile.setSensitivityDefault(Double.parseDouble(newValue));
        } catch (Exception e) {
            Log.e(TAG, "Invalid insulin sensitivity: " + newValue);
        }

        EditTextPreference thispref = (EditTextPreference) preference;
        thispref.setText(newValue);
        if (from_change) {
            preference.getEditor().putString("profile_insulin_sensitivitiy", newValue);
        }

        preference.setTitle(format_insulin_sensitivity(preference.getTitle().toString(), newValue));
    }


    private static void bindPreferenceSummaryToValue(Preference preference) {
        try {
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        } catch (Exception e) {
            Log.e(TAG, "Got exception binding preference summary: " + e.toString());
        }
    }

    private static void bindPreferenceTitleAppendToValueUpdateChannel(Preference preference) {
        try {
            preference.setOnPreferenceChangeListener(sBindPreferenceTitleAppendToValueListenerUpdateChannel);
            sBindPreferenceTitleAppendToValueListenerUpdateChannel.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        } catch (Exception e) {
            Log.e(TAG, "Got exception binding preference title: " + e.toString());
        }
    }


    private static void bindPreferenceSummaryToValueAndEnsureNumeric(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindNumericPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }


    public static class AllPrefsFragment extends PreferenceFragment {

        SharedPreferences prefs;

        private void setSummary(String pref_name) {
            try {
                // is there a cleaner way to bind these values when setting programatically?
                final String pref_val = prefs.getString(pref_name, "");
                findPreference(pref_name).setSummary(pref_val);
                EditTextPreference thispref = (EditTextPreference) findPreference(pref_name);
                thispref.setText(pref_val);
            } catch (Exception e) {
                Log.e(TAG, "Exception during setSummary: " + e.toString());
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            static_units = prefs.getString("units", "mgdl");
            addPreferencesFromResource(R.xml.pref_license);
            addPreferencesFromResource(R.xml.pref_general);
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("highValue"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("lowValue"));
            units_pref = findPreference("units");
            bindPreferenceSummaryToValue(units_pref);

            addPreferencesFromResource(R.xml.pref_notifications);
            bindPreferenceSummaryToValue(findPreference("bg_alert_profile"));
            bindPreferenceSummaryToValue(findPreference("calibration_notification_sound"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("calibration_snooze"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("bg_unclear_readings_minutes"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("disable_alerts_stale_data_minutes"));
            bindPreferenceSummaryToValue(findPreference("falling_bg_val"));
            bindPreferenceSummaryToValue(findPreference("rising_bg_val"));
            bindPreferenceSummaryToValue(findPreference("other_alerts_sound"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("other_alerts_snooze"));

            addPreferencesFromResource(R.xml.pref_data_source);

            addPreferencesFromResource(R.xml.pref_data_sync);
            setupBarcodeConfigScanner();
            setupBarcodeShareScanner();
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_uri"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_collection"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_device_status_collection"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_api_base"));

            addPreferencesFromResource(R.xml.pref_advanced_settings);
            addPreferencesFromResource(R.xml.xdrip_plus_prefs);


            bindPreferenceTitleAppendToValueUpdateChannel(findPreference("update_channel"));
            final Preference profile_carb_ratio_default = findPreference("profile_carb_ratio_default");
            profile_carb_ratio_default.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (!isNumeric(newValue.toString())) {
                        return false;
                    }
                    preference.setTitle(format_carb_ratio(preference.getTitle().toString(), newValue.toString()));
                    Profile.reloadPreferences(prefs);
                    Home.staticRefreshBGCharts();
                    return true;
                }
            });

            profile_carb_ratio_default.setTitle(format_carb_ratio(profile_carb_ratio_default.getTitle().toString(), prefs.getString("profile_carb_ratio_default", "")));


            profile_insulin_sensitivity_default = findPreference("profile_insulin_sensitivity_default");
            profile_insulin_sensitivity_default.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (!isNumeric(newValue.toString())) {
                        return false;
                    }
                    do_format_insulin_sensitivity(preference, prefs, true, newValue.toString());
                    Profile.reloadPreferences(prefs);
                    Home.staticRefreshBGCharts();
                    return true;
                }
            });

            do_format_insulin_sensitivity(profile_insulin_sensitivity_default, prefs, false, null);

            final Preference profile_carb_absorption_default = findPreference("profile_carb_absorption_default");
            profile_carb_absorption_default.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (!isNumeric(newValue.toString())) {
                        return false;
                    }
                    preference.setTitle(format_carb_absorption_rate(preference.getTitle().toString(), newValue.toString()));
                    Profile.reloadPreferences(prefs);
                    Home.staticRefreshBGCharts();
                    return true;
                }
            });

            profile_carb_absorption_default.setTitle(format_carb_absorption_rate(profile_carb_absorption_default.getTitle().toString(), prefs.getString("profile_carb_absorption_default", "")));


            refresh_extra_items();
            findPreference("plus_extra_features").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Home.invalidateMenu = true; // force redraw
                    refresh_extra_items();

                    return true;
                }
            });

            final Preference crash_reports = findPreference("enable_crashlytics");
            crash_reports.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(preference.getContext(),
                            "Crash Setting takes effect on next restart", Toast.LENGTH_LONG).show();
                    return true;
                }
            });

            bindTTSListener();
            final Preference collectionMethod = findPreference("dex_collection_method");
            final Preference runInForeground = findPreference("run_service_in_foreground");
            final Preference wifiRecievers = findPreference("wifi_recievers_addresses");
            final Preference predictiveBG = findPreference("predictive_bg");
            final Preference interpretRaw = findPreference("interpret_raw");

            final Preference shareKey = findPreference("share_key");
            shareKey.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    prefs.edit().remove("dexcom_share_session_id").apply();
                    return true;
                }
            });

            Preference.OnPreferenceChangeListener shareTokenResettingListener = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    prefs.edit().remove("dexcom_share_session_id").apply();
                    return true;
                }
            };

            final Preference sharePassword = findPreference("dexcom_account_password");
            sharePassword.setOnPreferenceChangeListener(shareTokenResettingListener);
            final Preference shareAccountName = findPreference("dexcom_account_name");
            shareAccountName.setOnPreferenceChangeListener(shareTokenResettingListener);

            final Preference scanShare = findPreference("scan_share2_barcode");
            final EditTextPreference transmitterId = (EditTextPreference) findPreference("dex_txid");
            final Preference pebbleSync = findPreference("broadcast_to_pebble");
            final Preference useCustomSyncKey = findPreference("use_custom_sync_key");
            final Preference CustomSyncKey = findPreference("custom_sync_key");
            final PreferenceCategory collectionCategory = (PreferenceCategory) findPreference("collection_category");
            final PreferenceCategory otherCategory = (PreferenceCategory) findPreference("other_category");
            final PreferenceScreen calibrationAlertsScreen = (PreferenceScreen) findPreference("calibration_alerts_screen");
            final PreferenceCategory alertsCategory = (PreferenceCategory) findPreference("alerts_category");
            final Preference disableAlertsStaleDataMinutes = findPreference("disable_alerts_stale_data_minutes");
            disableAlertsStaleDataMinutes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (!isNumeric(newValue.toString())) {
                        return false;
                    }
                    if ((Integer.parseInt(newValue.toString())) < 10) {
                        Toast.makeText(preference.getContext(),
                                "Value must be at least 10 minutes", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    preference.setSummary(newValue.toString());
                    return true;
                }
            });


            units_pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    try {
                        Double highVal = Double.parseDouble(prefs.getString("highValue", "0"));
                        Double lowVal = Double.parseDouble(prefs.getString("lowValue", "0"));
                        Double default_insulin_sensitivity = Double.parseDouble(prefs.getString("profile_insulin_sensitivity_default", "54"));
                        static_units = newValue.toString();
                        if (newValue.toString().equals("mgdl")) {
                            if (highVal < 36) {
                                prefs.edit().putString("highValue", Long.toString(Math.round(highVal * Constants.MMOLL_TO_MGDL))).apply();
                                prefs.edit().putString("profile_insulin_sensitivity_default", Long.toString(Math.round(default_insulin_sensitivity * Constants.MMOLL_TO_MGDL))).apply();
                            }
                            if (lowVal < 36) {
                                prefs.edit().putString("lowValue", Long.toString(Math.round(lowVal * Constants.MMOLL_TO_MGDL))).apply();
                                prefs.edit().putString("profile_insulin_sensitivity_default", Long.toString(Math.round(default_insulin_sensitivity * Constants.MMOLL_TO_MGDL))).apply();
                            }

                        } else {
                            if (highVal > 35) {
                                prefs.edit().putString("highValue", JoH.qs(highVal * Constants.MGDL_TO_MMOLL, 1)).apply();
                                prefs.edit().putString("profile_insulin_sensitivity_default", JoH.qs(default_insulin_sensitivity * Constants.MGDL_TO_MMOLL, 2)).apply();

                            }
                            if (lowVal > 35) {
                                prefs.edit().putString("lowValue", JoH.qs(lowVal * Constants.MGDL_TO_MMOLL, 1)).apply();
                                prefs.edit().putString("profile_insulin_sensitivity_default", JoH.qs(default_insulin_sensitivity * Constants.MGDL_TO_MMOLL, 2)).apply();
                            }
                        }
                        preference.setSummary(newValue.toString());
                        setSummary("highValue");
                        setSummary("lowValue");
                        if (profile_insulin_sensitivity_default != null) {
                            Log.d(TAG, "refreshing profile insulin sensitivity default display");
                            do_format_insulin_sensitivity(profile_insulin_sensitivity_default, prefs, false, null);
                        }
                        Profile.reloadPreferences(prefs);

                    } catch (Exception e) {
                        Log.e(TAG, "Got excepting processing high/low value preferences: " + e.toString());
                    }
                    return true;
                }
            });

            // jamorham xDrip+ prefs
            if (prefs.getString("custom_sync_key", "").equals("")) {
                prefs.edit().putString("custom_sync_key", CipherUtils.getRandomHexKey()).apply();
            }
            bindPreferenceSummaryToValue(findPreference("custom_sync_key")); // still needed?

            bindPreferenceSummaryToValue(findPreference("xplus_insulin_dia"));
            bindPreferenceSummaryToValue(findPreference("xplus_liver_sensitivity"));
            bindPreferenceSummaryToValue(findPreference("xplus_liver_maximpact"));

            bindPreferenceSummaryToValue(findPreference("low_predict_alarm_level"));


            useCustomSyncKey.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    PlusSyncService.clearandRestartSyncService(context);
                    return true;
                }
            });
            CustomSyncKey.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    Context context = preference.getContext();
                    PlusSyncService.clearandRestartSyncService(context);
                    return true;
                }
            });

            Log.d(TAG, prefs.getString("dex_collection_method", "BluetoothWixel"));
            if (prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("DexcomShare") != 0) {
                collectionCategory.removePreference(shareKey);
                collectionCategory.removePreference(scanShare);
                otherCategory.removePreference(interpretRaw);
                alertsCategory.addPreference(calibrationAlertsScreen);
            } else {
                otherCategory.removePreference(predictiveBG);
                alertsCategory.removePreference(calibrationAlertsScreen);
                prefs.edit().putBoolean("calibration_notifications", false).apply();
            }


            if ((prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("WifiWixel") != 0)
                    && (prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("WifiBlueToothWixel") != 0)) {
                String receiversIpAddresses;
                receiversIpAddresses = prefs.getString("wifi_recievers_addresses", "");
                // only hide if non wifi wixel mode and value not previously set to cope with
                // dynamic mode changes. jamorham
                if (receiversIpAddresses == null || receiversIpAddresses.equals("")) {
                    collectionCategory.removePreference(wifiRecievers);
                }
            }

            if (prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("DexbridgeWixel") != 0) {
                collectionCategory.removePreference(transmitterId);
            }

            if(prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("DexcomG5") == 0) {
                collectionCategory.addPreference(transmitterId);
            }
            pebbleSync.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final Context context = preference.getContext();
                    if ((Boolean) newValue) {

                            AlertDialog.Builder builder = new AlertDialog.Builder(context);

                            builder.setTitle("Pebble Install");
                            builder.setMessage("Install Pebble Watchface?");

                            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    context.startActivity(new Intent(context, InstallPebbleWatchFace.class));
                                }
                            });

                            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                            AlertDialog alert = builder.create();
                            alert.show();

                        context.startService(new Intent(context, PebbleSync.class));
                    } else {
                        context.stopService(new Intent(context, PebbleSync.class));
                    }
                    return true;
                }
            });
            bindPreferenceSummaryToValue(collectionMethod);
            bindPreferenceSummaryToValue(shareKey);
//            bindPreferenceSummaryToValue(wifiRecievers);

            wifiRecievers.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    ParakeetHelper.notifyOnNextCheckin(true);
                    return true;
                }
            });

            bindPreferenceSummaryToValue(transmitterId);
            transmitterId.getEditText().setFilters(new InputFilter[]{new InputFilter.AllCaps()});
            collectionMethod.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (((String) newValue).compareTo("DexcomShare") != 0) { // NOT USING SHARE
                        collectionCategory.removePreference(shareKey);
                        collectionCategory.removePreference(scanShare);
                        otherCategory.removePreference(interpretRaw);
                        otherCategory.addPreference(predictiveBG);
                        alertsCategory.addPreference(calibrationAlertsScreen);
                    } else {
                        collectionCategory.addPreference(shareKey);
                        collectionCategory.addPreference(scanShare);
                        otherCategory.addPreference(interpretRaw);
                        otherCategory.removePreference(predictiveBG);
                        alertsCategory.removePreference(calibrationAlertsScreen);
                        prefs.edit().putBoolean("calibration_notifications", false).apply();
                    }

                    if (((String) newValue).compareTo("BluetoothWixel") != 0
                            && ((String) newValue).compareTo("DexcomShare") != 0
                            && ((String) newValue).compareTo("DexbridgeWixel") != 0
                            && ((String) newValue).compareTo("LimiTTer") != 0
                            && ((String) newValue).compareTo("WifiBlueToothWixel") != 0) {
                        collectionCategory.removePreference(runInForeground);
                    } else {
                        collectionCategory.addPreference(runInForeground);
                    }

                    // jamorham always show wifi receivers option if populated as we may switch modes dynamically
                    if ((((String) newValue).compareTo("WifiWixel") != 0)
                            && (((String) newValue).compareTo("WifiBlueToothWixel") != 0)) {
                        String receiversIpAddresses;
                        receiversIpAddresses = prefs.getString("wifi_recievers_addresses", "");
                        if (receiversIpAddresses == null || receiversIpAddresses.equals("")) {
                            collectionCategory.removePreference(wifiRecievers);
                        } else {
                            collectionCategory.addPreference(wifiRecievers);
                        }
                    } else {
                        collectionCategory.addPreference(wifiRecievers);
                    }

                    if (((String) newValue).compareTo("DexbridgeWixel") != 0) {
                        collectionCategory.removePreference(transmitterId);
                    } else {
                        collectionCategory.addPreference(transmitterId);
                    }

                    if(((String) newValue).compareTo("DexcomG5") == 0) {
                        collectionCategory.addPreference(transmitterId);
                    }

                    String stringValue = newValue.toString();
                    if (preference instanceof ListPreference) {
                        ListPreference listPreference = (ListPreference) preference;
                        int index = listPreference.findIndexOfValue(stringValue);
                        preference.setSummary(
                                index >= 0
                                        ? listPreference.getEntries()[index]
                                        : null);

                    } else if (preference instanceof RingtonePreference) {
                        if (TextUtils.isEmpty(stringValue)) {
                            preference.setSummary(R.string.pref_ringtone_silent);

                        } else {
                            Ringtone ringtone = RingtoneManager.getRingtone(
                                    preference.getContext(), Uri.parse(stringValue));
                            if (ringtone == null) {
                                preference.setSummary(null);
                            } else {
                                String name = ringtone.getTitle(preference.getContext());
                                preference.setSummary(name);
                            }
                        }
                    } else {
                        preference.setSummary(stringValue);
                    }
                    if (preference.getKey().equals("dex_collection_method")) {
                        CollectionServiceStarter.restartCollectionService(preference.getContext(), (String) newValue);
                        if (newValue.equals("Follower"))
                        {
                            prefs.edit().putBoolean("plus_follow_master", false).apply();
                            GcmActivity.requestBGsync();
                        }
                    } else {
                        CollectionServiceStarter.restartCollectionService(preference.getContext());
                    }
                    return true;
                }
            });
        }

        private void setupBarcodeConfigScanner() {
            findPreference("auto_configure").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AndroidBarcode(getActivity()).scan();
                    return true;
                }
            });
        }


        private void setupBarcodeShareScanner() {
            findPreference("scan_share2_barcode").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AndroidBarcode(getActivity()).scan();
                    return true;
                }
            });
        }

        private void refresh_extra_items() {
            try {
                if (prefs == null) return;
                if (!prefs.getBoolean("plus_extra_features", false)) {
                    // getPreferenceScreen().removePreference(findPreference("plus_follow_master"));

                } else {
                    // getPreferenceScreen().addPreference(findPreference("plus_follow_master"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Got exception in refresh extra: " + e.toString());
            }
        }

        private void bindTTSListener() {
            findPreference("bg_to_speech").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((Boolean) newValue) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                        alertDialog.setTitle("Install Text-To-Speech Data?");
                        alertDialog.setMessage("Install Text-To-Speech Data?\n(After installation of languages you might have to press \"Restart Collector\" in System Status.)");
                        alertDialog.setCancelable(true);
                        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BgToSpeech.installTTSData(getActivity());
                            }
                        });
                        alertDialog.setNegativeButton(R.string.no, null);
                        AlertDialog alert = alertDialog.create();
                        alert.show();
                    }
                    return true;
                }
            });
        }


        private static Preference.OnPreferenceChangeListener sBgMissedAlertsHandler = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Context context = preference.getContext();
                context.startService(new Intent(context, MissedReadingService.class));
                return true;
            }
        };

        
        private void bindBgMissedAlertsListener(){
          findPreference("other_alerts_snooze").setOnPreferenceChangeListener(sBgMissedAlertsHandler);
        }

        private static class WidgetListener implements Preference.OnPreferenceChangeListener {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Context context = preference.getContext();
                if(AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, xDripWidget.class)).length > 0){
                    context.startService(new Intent(context, WidgetUpdateService.class));
                }
                return true;
            }
        }
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}

