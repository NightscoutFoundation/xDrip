package com.eveningoutpost.dexdrip.insulin;

import android.util.Log;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class InsulinManager {
    private static final String TAG = "InsulinManager";
    private static ArrayList<Insulin> profiles;

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
                switch (d.ATCCode)
                {
                    case "A10AB01":
                        insulin = new A10AB01(d.displayName, d.PPN);
                        Log.d(TAG, "initialized A10AB01 insulin " + d.displayName);
                        break;
                    case "A10AB05":
                        insulin = new A10AB05(d.displayName, d.PPN);
                        Log.d(TAG, "initialized A10AB05 insulin " + d.displayName);
                        break;
                    case "A10AC01":
                        insulin = new A10AC01(d.displayName, d.PPN);
                        Log.d(TAG, "initialized A10AC01 insulin " + d.displayName);
                        break;
                    default:
                        Log.d(TAG, "UNKNOWN ATCCode " + d.ATCCode);
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

    class insulinData {
        public String displayName;
        public ArrayList<String> PPN;
        public String ATCCode;
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
        if (profiles == null) {
                Log.d(TAG, "Initialize insulin profiles");
                insulinDataWrapper iDW;
                try {
                    String input = readTextFile(in_s);
                    Gson gson = new Gson();
                    iDW = gson.fromJson(input, insulinDataWrapper.class);
                    profiles = iDW.getInsulinProfiles();
                    Log.d(TAG, "Loaded Insulin Profiles: " + Integer.toString(profiles.size()));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "Got exception during insulin load: " + e.toString());
                }
            }
    }

    public static ArrayList<Insulin> getInstance(InputStream in_s) {
        initializeInsulinManager(in_s);
        return profiles;
    }

    public static Insulin getProfile(int i)
    {
        if (profiles == null) {
            Log.d(TAG, "InsulinManager seems not load Profiles beforehand");
            return null;
        }
        if (i > profiles.size())
            return null;
        return profiles.get(i);
    }

    public static Insulin getProfile(String name)
    {
        if (profiles == null) {
            Log.d(TAG, "InsulinManager seems not load Profiles beforehand");
            return null;
        }
        for (Insulin i: profiles)
            if (i.getDisplayName().toLowerCase().equals(name.toLowerCase()))
                return i;
        return null;
    }
}
