package com.eveningoutpost.dexdrip.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.PebbleSync;

import java.util.List;

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
    public  static SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new AllPrefsFragment()).commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        addPreferencesFromResource(R.xml.pref_general);

    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (AllPrefsFragment.class.getName().equals(fragmentName)){ return true; }
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

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    public static class AllPrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_license);
            addPreferencesFromResource(R.xml.pref_general);
            bindPreferenceSummaryToValue(findPreference("highValue"));
            bindPreferenceSummaryToValue(findPreference("lowValue"));
            bindPreferenceSummaryToValue(findPreference("units"));

            addPreferencesFromResource(R.xml.pref_notifications);
//            bindPreferenceSummaryToValue(findPreference("bg_alerts_screen"));
//            bindPreferenceSummaryToValue(findPreference("bg_alerts_from_main_menu"));
            bindPreferenceSummaryToValue(findPreference("calibration_notification_sound"));
            bindPreferenceSummaryToValue(findPreference("calibration_snooze"));
            bindPreferenceSummaryToValue(findPreference("bg_unclear_readings_minutes"));
            bindPreferenceSummaryToValue(findPreference("bg_missed_minutes"));
            bindPreferenceSummaryToValue(findPreference("other_alerts_sound"));
            bindPreferenceSummaryToValue(findPreference("other_alerts_snooze"));

            addPreferencesFromResource(R.xml.pref_data_source);


            addPreferencesFromResource(R.xml.pref_data_sync);
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_uri"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_collection"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_device_status_collection"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_api_base"));

            addPreferencesFromResource(R.xml.pref_advanced_settings);

            final Preference collectionMethod = findPreference("dex_collection_method");
            final Preference runInForeground = findPreference("run_service_in_foreground");
            final Preference wifiRecievers = findPreference("wifi_recievers_addresses");
            final Preference predictiveBG = findPreference("predictive_bg");
            final Preference interpretRaw = findPreference("interpret_raw");
            final Preference shareKey = findPreference("share_key");
            final Preference transmitterId = findPreference("dex_txid");
            final Preference pebbleSync = findPreference("broadcast_to_pebble");
            final PreferenceCategory collectionCategory = (PreferenceCategory) findPreference("collection_category");
            final PreferenceCategory otherCategory = (PreferenceCategory) findPreference("other_category");
            final PreferenceScreen calibrationAlertsScreen = (PreferenceScreen) findPreference("calibration_alerts_screen");
            final PreferenceCategory alertsCategory = (PreferenceCategory) findPreference("alerts_category");
            prefs =  getPreferenceManager().getDefaultSharedPreferences(getActivity());
            Log.d("PREF", prefs.getString("dex_collection_method", "BluetoothWixel"));
            if(prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("DexcomShare") != 0) {
                collectionCategory.removePreference(shareKey);
                otherCategory.removePreference(interpretRaw);
                alertsCategory.addPreference(calibrationAlertsScreen);
            } else {
                otherCategory.removePreference(predictiveBG);
                alertsCategory.removePreference(calibrationAlertsScreen);
                prefs.edit().putBoolean("calibration_notifications", false).apply();
            }

            if(prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("BluetoothWixel") != 0 && prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("DexcomShare") != 0 && prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("DexbridgeWixel") != 0) {
                collectionCategory.removePreference(runInForeground);
            }

            if(prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("WifiWixel") != 0) {
                collectionCategory.removePreference(wifiRecievers);
            }

            if(prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("DexbridgeWixel") != 0) {
                collectionCategory.removePreference(transmitterId);
            }
            pebbleSync.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    if ((Boolean) newValue) {
                        context.startService(new Intent(context, PebbleSync.class));
                    } else {
                        context.stopService(new Intent(context, PebbleSync.class));
                    }
                    return true;
                }
            });
            bindPreferenceSummaryToValue(collectionMethod);
            bindPreferenceSummaryToValue(shareKey);
            bindPreferenceSummaryToValue(wifiRecievers);
            collectionMethod.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if(((String) newValue).compareTo("DexcomShare") != 0) { // NOT USING SHARE
                        collectionCategory.removePreference(shareKey);
                        otherCategory.removePreference(interpretRaw);
                        otherCategory.addPreference(predictiveBG);
                        alertsCategory.addPreference(calibrationAlertsScreen);
                    } else {
                        collectionCategory.addPreference(shareKey);
                        otherCategory.addPreference(interpretRaw);
                        otherCategory.removePreference(predictiveBG);
                        alertsCategory.removePreference(calibrationAlertsScreen);
                        prefs.edit().putBoolean("calibration_notifications", false).apply();
                    }

                    if(((String) newValue).compareTo("BluetoothWixel") != 0 && ((String) newValue).compareTo("DexcomShare") != 0 && ((String) newValue).compareTo("DexbridgeWixel") != 0) {
                        collectionCategory.removePreference(runInForeground);
                    } else {
                        collectionCategory.addPreference(runInForeground);
                    }

                    if(((String) newValue).compareTo("WifiWixel") != 0) {
                        collectionCategory.removePreference(wifiRecievers);
                    } else {
                        collectionCategory.addPreference(wifiRecievers);
                    }

                    if(((String) newValue).compareTo("DexbridgeWixel") != 0) {
                        collectionCategory.removePreference(transmitterId);
                    } else {
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
                    CollectionServiceStarter.restartCollectionService(preference.getContext());
                    return true;
                }
            });
        }
    }
}
