package com.eveningoutpost.dexdrip.insulin;

import android.util.Log;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class InsulinManager {
    private static final String TAG = "InsulinManager";
    private static ArrayList<Insulin> profiles;
    private static Insulin basalProfile, bolusProfile;

    class insulinDataWrapper {
        public ArrayList<insulinData> profiles;

        insulinDataWrapper() {
            profiles = new ArrayList<insulinData>();
        }

        public ArrayList<Insulin> getInsulinProfiles() {
            if (!checkUniquenessPPN())
                return null;
            ArrayList<Insulin> ret = new ArrayList<Insulin>();
            for (insulinData d : profiles) {
                Insulin insulin;
                switch (d.Curve.type.toLowerCase()) {
                    case "linear trapezoid":
                        insulin = new LinearTrapezoidInsulin(d.name, d.displayName, d.PPN, d.concentration, d.Curve.data);
                        Log.d(TAG, "initialized linear trapezoid insulin " + d.displayName);
                        break;
                    default:
                        Log.d(TAG, "UNKNOWN Curve-Type " + d.Curve.type);
                        return null;
                }
                ret.add(insulin);
            }
            return ret;
        }

        private Boolean checkUniquenessPPN() {
            Log.d(TAG, "checking for uniqueness");
            ArrayList<String> PPNs = new ArrayList<String>();
            for (insulinData d : profiles)
                for (String ppn : d.PPN)
                    if (PPNs.contains(ppn)) {
                        Log.d(TAG, "pharmacy product number dupplicated " + ppn + ". that's not allowed!");
                        return false;
                    } else PPNs.add(ppn);
            Log.d(TAG, "pharmacy product numbers uniquee");
            return true;
        }
    }

    class insulinCurve {
        public String type;
        public JsonObject data;
    }

    class insulinData {
        public String displayName;
        public String name;
        public ArrayList<String> PPN;
        public String concentration;
        public insulinCurve Curve;
    }

    private static String readTextFile(InputStream inputStream) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (inputStream != null) {
            byte buf[] = new byte[1024];
            int len;
            try {
                while ((len = inputStream.read(buf)) != -1) {
                    outputStream.write(buf, 0, len);
                }
                outputStream.close();
                inputStream.close();
            } catch (IOException e) {

            }
        }
        return outputStream.toString();
    }

    private static void initializeInsulinManager(InputStream in_s) {
        Log.d(TAG, "Initialize insulin profiles");
        insulinDataWrapper iDW;
        try {
            String input = readTextFile(in_s);
            Gson gson = new Gson();
            iDW = gson.fromJson(input, insulinDataWrapper.class);
            profiles = iDW.getInsulinProfiles();
            Log.d(TAG, "Loaded Insulin Profiles: " + Integer.toString(profiles.size()));
            LoadDisabledProfilesFromPrefs();
            Log.d(TAG, "InsulinManager initialized from config file and Prefs");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Got exception during insulin load: " + e.toString());
        }
    }

    private static void checkInitialized() {
        if (profiles == null) {
            getDefaultInstance();
        }
    }

    // populate the data set with predefined resource as otherwise the static reference could be lost
    // as we are not really safely handling it
    public static ArrayList<Insulin> getDefaultInstance() {
        return getInstance(xdrip.getAppContext().getResources().openRawResource(R.raw.insulin_profiles));
    }

    // before this can be public, the issue of what to do if profiles is null needs to be resolved.
    private static ArrayList<Insulin> getInstance(InputStream in_s) {
        initializeInsulinManager(in_s);
        return profiles;
    }

    public static Insulin getBasalProfile() {
        return basalProfile;
    }

    public static void setBasalProfile(Insulin p) {
        basalProfile = p;
    }

    public static Insulin getBolusProfile() {
        checkInitialized();
        return bolusProfile;
    }

    public static void setBolusProfile(Insulin p) {
        bolusProfile = p;
    }

    public static ArrayList<Insulin> getAllProfiles() {
        return profiles;
    }

    public static Insulin getProfile(int i) {
        checkInitialized();
        if (profiles == null) {
            Log.d(TAG, "InsulinManager seems not load Profiles beforehand");
            return null;
        }
        ArrayList<Insulin> t = new ArrayList<Insulin>();
        for (Insulin ins : profiles)
            if (isProfileEnabled(ins))
                t.add(ins);
        if (i >= t.size())
            return null;
        return t.get(i);
    }

    public static Insulin getProfile(String name) {
        checkInitialized();
        if (profiles == null) {
            Log.d(TAG, "InsulinManager seems not load Profiles beforehand");
            return null;
        }
        name = name.toLowerCase();
        // TODO consider hashmap maybe? how many could be iterated here?
        for (Insulin i : profiles) {
            if (i.getName().toLowerCase().equals(name))
                return i;
        }
        return null;
    }

    public static long getMaxEffect(Boolean enabled) {
        checkInitialized();
        long max = 0;
        for (Insulin i : profiles)
            if (!enabled || i.isEnabled())
                if (max < i.getMaxEffect())
                    max = i.getMaxEffect();
        return max;
    }

    public static Boolean isProfileEnabled(Insulin i) {
        return i.isEnabled();
    }

    public static void disableProfile(Insulin i) {
        if (isProfileEnabled(i) && (countEnabledProfiles() > 1))
            i.disable();
    }

    public static void enableProfile(Insulin i) {
        if (!isProfileEnabled(i) && (countEnabledProfiles() < 3))
            i.enable();
    }

    private static int countEnabledProfiles() {
        checkInitialized();
        int ret = 0;
        for (Insulin ins : profiles)
            if (isProfileEnabled(ins))
                ret++;
        return ret;
    }

    public static void LoadDisabledProfilesFromPrefs() {
        checkInitialized();
        for (Insulin i : profiles)
            i.enable();
        String json = Pref.getString("saved_disabled_insulinprofiles_json", "[]");
        Log.d(TAG, "Loaded disabled Insulin Profiles from Prefs: " + json);
        String[] disabled = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, String[].class);
        for (String d : disabled) {
            Insulin ins = getProfile(d);
            if (ins != null)
                disableProfile(ins);
        }
        String prof = Pref.getString("saved_basal_insulinprofiles", "");
        Log.d(TAG, "Loaded basal Insulin Profiles from Prefs: " + prof);
        basalProfile = getProfile(prof);
        if (basalProfile == null)
            basalProfile = profiles.get(0);
        prof = Pref.getString("saved_bolus_insulinprofiles", "");
        Log.d(TAG, "Loaded bolus Insulin Profiles from Prefs: " + prof);
        bolusProfile = getProfile(prof);
        if (bolusProfile == null)
            bolusProfile = profiles.get(0);
    }

    public static void saveDisabledProfilesToPrefs() {
        checkInitialized();
        ArrayList<String> disabled = new ArrayList<String>();
        for (Insulin i : profiles)
            if (!isProfileEnabled(i))
                disabled.add(i.getName());
        String json = new GsonBuilder().create().toJson(disabled);
        Pref.setString("saved_disabled_insulinprofiles_json", json);
        Log.d(TAG, "saved disabled Insulin Profiles to Prefs: " + json);
        if (basalProfile != null) {
            Pref.setString("saved_basal_insulinprofiles", basalProfile.getName());
            Log.d(TAG, "saved basal Insulin Profiles to Prefs: " + basalProfile.getName());
        }
        if (bolusProfile != null) {
            Pref.setString("saved_bolus_insulinprofiles", bolusProfile.getName());
            Log.d(TAG, "saved bolus Insulin Profiles to Prefs: " + bolusProfile.getName());
        }
    }
}
