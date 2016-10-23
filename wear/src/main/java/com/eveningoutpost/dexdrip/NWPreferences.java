package com.eveningoutpost.dexdrip;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.eveningoutpost.dexdrip.R;

public class NWPreferences extends PreferenceActivity {

    private SharedPreferences mPrefs;
    public PreferenceScreen screen;
    public PreferenceCategory category;
    public Preference collectionMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        screen = (PreferenceScreen) findPreference("preferenceScreen");
        category = (PreferenceCategory) findPreference("collection_category");
        collectionMethod = findPreference("g5_collection_method");
        listenForChangeInSettings();
        setCollectionPrefs();
    }

    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

        if(key.compareTo("g5_collection_method") == 0) {
            setCollectionPrefs();
        }

        }
    };

    public void listenForChangeInSettings() {
        mPrefs.registerOnSharedPreferenceChangeListener(prefListener);
        // TODO do we need an unregister!?
    }

    public void setCollectionPrefs() {
        if (mPrefs.getBoolean("g5_collection_method", false)) {//DexCollectionType.DexcomG5
            screen.addPreference(category);
            Log.d("NWPreferences", "setCollectionPrefs addPreference category");
        }
        else {
            screen.removePreference(category);
            Log.d("NWPreferences", "setCollectionPrefs removePreference category");
        }

        if (collectionMethod != null && category != null) {
            category.removePreference(collectionMethod);
        }

    }
}