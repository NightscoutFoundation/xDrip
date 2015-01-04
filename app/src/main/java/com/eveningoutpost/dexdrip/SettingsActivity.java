package com.eveningoutpost.dexdrip;

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
import android.preference.RingtonePreference;
import android.text.TextUtils;


import java.util.List;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupSimplePreferencesScreen();
    }

    private void setupSimplePreferencesScreen() {
        addPreferencesFromResource(R.xml.pref_license);

        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_general);


        fakeHeader = new PreferenceCategory(this);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_bg_notification);

        fakeHeader = new PreferenceCategory(this);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_calibration_notfication);

        fakeHeader = new PreferenceCategory(this);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_data_source);

        fakeHeader = new PreferenceCategory(this);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_data_sync);

        fakeHeader = new PreferenceCategory(this);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_wifi);

        bindPreferenceSummaryToValue(findPreference("highValue"));
        bindPreferenceSummaryToValue(findPreference("lowValue"));
        bindPreferenceSummaryToValue(findPreference("bg_snooze"));
        bindPreferenceSummaryToValue(findPreference("calibration_snooze"));
        bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_uri"));
        bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_collection"));
        bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_device_status_collection"));
        bindPreferenceSummaryToValue(findPreference("cloud_storage_api_base"));
        bindPreferenceSummaryToValue(findPreference("dex_collection_method"));
        bindPreferenceSummaryToValue(findPreference("wifi_recievers_addresses"));
        bindPreferenceSummaryToValue(findPreference("units"));
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
}
