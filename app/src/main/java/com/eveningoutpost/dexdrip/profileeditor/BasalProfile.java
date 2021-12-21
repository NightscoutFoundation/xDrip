package com.eveningoutpost.dexdrip.profileeditor;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import lombok.val;

import static com.eveningoutpost.dexdrip.Models.JoH.JsonStringToFloatList;

// jamorham

public class BasalProfile {

    private static final String[] PROFILE_NAMES = {"1", "2", "3", "4", "5"};
    private static final String BASAL_PREFIX = "BASAL-PROFILE-";

    private static String getPrefix(final String ref) {
        return BASAL_PREFIX + ref;
    }

    public static void save(final String ref, final List<Float> segments) {
        Pref.setString(getPrefix(ref), JoH.defaultGsonInstance().toJson(segments));
        //android.util.Log.d("PROFILEXX", "Saved value:" + Pref.getString(getPrefix(ref), "null"));
    }

    public static List<Float> load(final String ref) {
        return JsonStringToFloatList(Pref.getString(getPrefix(ref), ""));
    }


    public static String getAllProfilesAsJson() {
        val profiles = new JSONArray();
        for (val profile : PROFILE_NAMES) {
            val item = new JSONObject();
            try {
                val result = Pref.getString(getPrefix(profile), null);
                if (result != null && result.length() > 5) {
                    item.put(profile, result);
                    profiles.put(item);
                }
            } catch (final JSONException e) {
                e.printStackTrace();
            }
        }
        return profiles.length() > 0 ? profiles.toString() : null;
    }

}
