package com.eveningoutpost.dexdrip.insulin;

import android.util.Log;

import com.eveningoutpost.dexdrip.UtilityModels.Pref;
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
    private static  Insulin primaryProfile;

    class insulinDataWrapper {
        public ArrayList<insulinData> profiles;

        insulinDataWrapper() {
            profiles = new ArrayList<insulinData>();
        }

        public ArrayList<Insulin> getInsulinProfiles()
        {
            if (!checkUniquenessPPN())
                return null;
            ArrayList<Insulin> ret = new ArrayList<Insulin>();
            for (insulinData d: profiles)
            {
                Insulin insulin;
                switch (d.Curve.type.toLowerCase())
                {
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

        private Boolean checkUniquenessPPN()
        {
            Log.d(TAG, "checking for uniqueness");
            ArrayList<String> PPNs = new ArrayList<String>();
            for (insulinData d: profiles)
                for (String ppn: d.PPN)
                    if (PPNs.contains(ppn)) {
                        Log.d(TAG, "pharmacy product number dupplicated " + ppn + ". that's not allowed!");
                        return false;
                    }
                    else PPNs.add(ppn);
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

    public static ArrayList<Insulin> getInstance(InputStream in_s) {
        initializeInsulinManager(in_s);
        return profiles;
    }

    public static Insulin getPrimaryProfile()
    {
        return primaryProfile;
    }
    public static void setPrimaryProfile(Insulin p)
    {
        primaryProfile = p;
    }

    public static ArrayList<Insulin> getAllProfiles() {
        return profiles;
    }

    public static Insulin getProfile(int i)
    {
        if (profiles == null) {
            Log.d(TAG, "InsulinManager seems not load Profiles beforehand");
            return null;
        }
        ArrayList<Insulin> t = new ArrayList<Insulin>();
        for (Insulin ins: profiles)
            if (isProfileEnabled(ins))
                t.add(ins);
        if (i >= t.size())
            return null;
        return t.get(i);
    }

    public static Insulin getProfile(String name)
    {
        if (profiles == null) {
            Log.d(TAG, "InsulinManager seems not load Profiles beforehand");
            return null;
        }
        for (Insulin i: profiles)
            if (i.getName().toLowerCase().equals(name.toLowerCase()))
                return i;
        return null;
    }

    public static long getMaxEffect(Boolean enabled)
    {
        long max = 0;
        for (Insulin i: profiles)
            if (!enabled || i.isEnabled())
                if (max < i.getMaxEffect())
                    max = i.getMaxEffect();
        return max;
    }

    public static Boolean isProfileEnabled(Insulin i)
    {
        return i.isEnabled();
    }

    public static void disableProfile(Insulin i)
    {
        if (isProfileEnabled(i) && (countEnabledProfiles() > 1))
            i.disable();
    }
    public static void enableProfile(Insulin i)
    {
        if (!isProfileEnabled(i) && (countEnabledProfiles() < 3))
            i.enable();
    }

    private static int countEnabledProfiles()
    {
        int ret = 0;
        for (Insulin ins: profiles)
            if (isProfileEnabled(ins))
                ret++;
        return ret;
    }

    public static void LoadDisabledProfilesFromPrefs()
    {
        for (Insulin i: profiles)
            i.enable();
        String json = Pref.getString("saved_disabled_insulinprofiles_json", "[]");
        Log.d(TAG, "Loaded disabled Insulin Profiles from Prefs: " + json);
        String[] disabled = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, String[].class);
        for (String d: disabled)
        {
            Insulin ins = getProfile(d);
            if (ins != null)
                disableProfile(ins);
        }
        String primProf = Pref.getString("saved_primary_insulinprofiles", "");
        Log.d(TAG, "Loaded primary Insulin Profiles from Prefs: " + primProf);
        primaryProfile = getProfile(primProf);
    }

    public static void saveDisabledProfilesToPrefs()
    {
        ArrayList<String> disabled = new ArrayList<String>();
        for (Insulin i: profiles)
            if (!isProfileEnabled(i))
                disabled.add(i.getName());
        String json = new GsonBuilder().create().toJson(disabled);
        Pref.setString("saved_disabled_insulinprofiles_json", json);
        Log.d(TAG, "saved disabled Insulin Profiles to Prefs: " + json);
        if (primaryProfile != null) {
            Pref.setString("saved_primary_insulinprofiles", primaryProfile.getName());
            Log.d(TAG, "saved primary Insulin Profiles to Prefs: " + primaryProfile.getName());
        }
    }
}
