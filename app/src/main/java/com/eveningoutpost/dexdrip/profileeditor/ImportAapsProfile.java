package com.eveningoutpost.dexdrip.profileeditor;

import static com.eveningoutpost.dexdrip.profileeditor.BasalProfile.consolidate;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.Unitized;
import com.eveningoutpost.dexdrip.utils.jobs.BackgroundQueue;
import com.google.gson.GsonBuilder;

import java.util.Collections;
import java.util.HashMap;

import lombok.val;

/**
 * JamOrHam
 * Import AAPS profiles via local treatment broadcast
 */

public class ImportAapsProfile {

    private static final String TAG = ImportAapsProfile.class.getSimpleName();

    public static AapsProfile importFromJson(final String json) {
        if (json == null) return null;
        val pjo = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, AapsProfile.class);
        if (pjo != null) {

            if (pjo.basal != null) {
                Collections.sort(pjo.basal);
            }
            if (pjo.carbratio != null) {
                Collections.sort(pjo.carbratio);
            }
            if (pjo.sens != null) {
                Collections.sort(pjo.sens);
            }
        }
        return pjo;
    }

    public static AapsProfile importFromMap(final HashMap<String, Object> tmap) {

        // TODO sanity check mills ?
        val profileJson = (String) tmap.get("profileJson");
        if (profileJson != null) {
            return importFromJson(profileJson);
        } else {
            UserError.Log.wtf(TAG, "Can't find profileJson in profile switch?");
        }
        return null;
    }

    public static void importAndSaveFromMap(final HashMap<String, Object> tmap) {
        val pjo = importFromMap(tmap);
        if (pjo != null && pjo.looksReasonable()) {
            BasalProfile.save(BasalProfile.getActiveRateName(), consolidate(pjo.getBasalByMinute(), 24));
            if (pjo.usingMgdl() == Unitized.usingMgDl()) {
                val profile = pjo.getXdripMergedProfileList();
                if (profile.size() > 0) {
                    ProfileEditor.saveProfileJson(JoH.defaultGsonInstance().toJson(profile));
                } else {
                    UserError.Log.wtf(TAG, "Couldn't get any carb/sens from imported profile");
                }
            } else {
                UserError.Log.wtf(TAG, "Mismatched units mmol vs mgdl between AAPS and xDrip");
            }
            if (Pref.getBooleanDefaultFalse("profile_import_sound")) {
                BackgroundQueue.post(() -> JoH.playSoundUri(JoH.getResourceURI(R.raw.labbed_musical_chime)));
            }
            UserError.Log.e(TAG, "xDrip imported AAPS profile");
        }
    }

}
