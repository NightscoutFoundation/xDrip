package com.eveningoutpost.dexdrip.webservices;

import com.eveningoutpost.dexdrip.models.DateUtil;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.profileeditor.BasalProfile;
import com.eveningoutpost.dexdrip.profileeditor.ProfileEditor;
import com.eveningoutpost.dexdrip.profileeditor.ProfileItem;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.eveningoutpost.dexdrip.models.JoH.tolerantParseDouble;

/**
 * emulates the Nightscout /api/v1/profile.json endpoint at profile.json
 * <p>
 * returns the active carb ratio, insulin sensitivity and basal rate per time block
 * from ProfileEditor / BasalProfile, plus the global carb absorption rate
 */

public class WebServiceProfile extends BaseWebService {

    private static final String TAG = "WebServiceProfile";
    private static final String DEFAULT_PROFILE_NAME = "Default";
    private static final int MINS_PER_DAY = 24 * 60;

    public WebResponse request(String query) {
        final JSONArray reply = new JSONArray();

        try {
            final List<ProfileItem> profileItemList = ProfileEditor.loadData(false);
            Collections.sort(profileItemList, Comparator.comparingInt(item -> item.start_min));

            final boolean using_mgdl = Pref.getString("units", "mgdl").equals("mgdl");
            final String units = using_mgdl ? "mg/dl" : "mmol";
            final String timezone = TimeZone.getDefault().getID();

            final JSONObject defaultProfile = new JSONObject();
            defaultProfile.put("carbratio", buildBlockArray(profileItemList, BlockField.CARB_RATIO));
            defaultProfile.put("sens", buildBlockArray(profileItemList, BlockField.SENSITIVITY));
            defaultProfile.put("basal", buildBasalArray(BasalProfile.load(BasalProfile.getActiveRateName())));
            // xDrip stores carb absorption rate as a single global value, not per time block
            defaultProfile.put("carbs_hr", tolerantParseDouble(Pref.getString("profile_carb_absorption_default", "35")));
            defaultProfile.put("units", units);
            defaultProfile.put("timezone", timezone);

            final JSONObject store = new JSONObject();
            store.put(DEFAULT_PROFILE_NAME, defaultProfile);

            final long now = JoH.tsl();
            final JSONObject profile = new JSONObject();
            profile.put("defaultProfile", DEFAULT_PROFILE_NAME);
            profile.put("store", store);
            profile.put("startDate", DateUtil.toISOString(now));
            profile.put("mills", now);
            profile.put("units", units);

            reply.put(profile);
        } catch (JSONException e) {
            UserError.Log.wtf(TAG, "Got json exception: " + e);
        }

        return new WebResponse(reply.toString());
    }

    private enum BlockField {CARB_RATIO, SENSITIVITY}

    private static double fieldValue(final ProfileItem item, final BlockField field) {
        return field == BlockField.CARB_RATIO ? item.carb_ratio : item.sensitivity;
    }

    private static JSONArray buildBlockArray(final List<ProfileItem> profileItemList, final BlockField field) throws JSONException {
        final JSONArray blocks = new JSONArray();
        for (final ProfileItem item : profileItemList) {
            final JSONObject block = new JSONObject();
            block.put("time", String.format(Locale.ENGLISH, "%02d:%02d", item.start_min / 60, item.start_min % 60));
            block.put("timeAsSeconds", item.start_min * 60);
            block.put("value", fieldValue(item, field));
            blocks.put(block);
        }
        return blocks;
    }

    // basal segments are stored as an evenly spaced List<Float> covering the whole day
    private static JSONArray buildBasalArray(final List<Float> segments) throws JSONException {
        final JSONArray blocks = new JSONArray();
        if (segments == null || segments.isEmpty()) {
            return blocks;
        }
        final int minutesPerSegment = MINS_PER_DAY / segments.size();
        for (int i = 0; i < segments.size(); i++) {
            final int start_min = i * minutesPerSegment;
            final JSONObject block = new JSONObject();
            block.put("time", String.format(Locale.ENGLISH, "%02d:%02d", start_min / 60, start_min % 60));
            block.put("timeAsSeconds", start_min * 60);
            block.put("value", JoH.roundDouble(segments.get(i), 2));
            blocks.put(block);
        }
        return blocks;
    }

}
