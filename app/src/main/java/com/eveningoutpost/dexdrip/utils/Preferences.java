package com.eveningoutpost.dexdrip.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;

import com.eveningoutpost.dexdrip.R;

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
    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (EulaPrefFragment.class.getName().equals(fragmentName)){ return true; }
        if (GeneralPrefFragment.class.getName().equals(fragmentName)){ return true; }
        if (BgNotificationPrefFragment.class.getName().equals(fragmentName)){ return true; }
        if (CalibrationNotificationPrefFragment.class.getName().equals(fragmentName)){ return true; }
        if (CollectionMethodPrefFragment.class.getName().equals(fragmentName)){ return true; }
        if (DataUploadPrefFragment.class.getName().equals(fragmentName)){ return true; }
        if (wifiPrefFragment.class.getName().equals(fragmentName)){ return true; }
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
            if (preference.getKey().compareTo("dex_collection_method") == 0) {
                ListPreference listPreference = (ListPreference) preference;
//                if (listPreference.getValue().compareTo("DexcomShare") == 0) {
//                    PreferenceFragment preferenceScreen = new CollectionMethodPrefFragment();
//                    PreferenceScreen collectionMethodScreen = preferenceScreen.getPreferenceScreen();
//                    Preference sharePreference = collectionMethodScreen.getPreference(1);
//                    collectionMethodScreen.removePreference(sharePreference);
//                }

            }
            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
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

    public static class EulaPrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_license);
        }
    }

    public static class GeneralPrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            bindPreferenceSummaryToValue(findPreference("highValue"));
            bindPreferenceSummaryToValue(findPreference("lowValue"));
            bindPreferenceSummaryToValue(findPreference("units"));
        }
    }
    public static class BgNotificationPrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_bg_notification);
            bindPreferenceSummaryToValue(findPreference("bg_snooze"));
        }
    }
    public static class CalibrationNotificationPrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_calibration_notfication);
            bindPreferenceSummaryToValue(findPreference("calibration_snooze"));
        }
    }
    public static class CollectionMethodPrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_source);
            Preference collectionMethod = findPreference("dex_collection_method");
            final Preference shareKey = findPreference("share_key");
            bindPreferenceSummaryToValue(collectionMethod);
            bindPreferenceSummaryToValue(shareKey);

            collectionMethod.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if(((String) newValue).compareTo("DexcomShare") == 0) {
                        getPreferenceScreen().removePreference(shareKey);
                    } else {
                        getPreferenceScreen().addPreference(shareKey);
                    }
                    return true;
                }
            });
        }
    }
    public static class DataUploadPrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_uri"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_collection"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_device_status_collection"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_api_base"));
        }
    }
    public static class wifiPrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_wifi);
            bindPreferenceSummaryToValue(findPreference("wifi_recievers_addresses"));
        }
    }
    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);

            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }
    }
}
