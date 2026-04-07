package com.eveningoutpost.dexdrip.cloud.nightlite;

import static com.nightscout.core.barcode.NSBarcodeConfigKeys.API_URI;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.val;

// JamOrHam

public class NightLiteQR {

    private static final String TAG = NightLiteQR.class.getSimpleName();

    public static final String NSLITE_CONFIG = "nslite";
    private JSONObject config = new JSONObject();

    public NightLiteQR(String decodeResults) {
        configureBarcode(decodeResults);
    }

    public void configureBarcode(String jsonConfig) {
        if (jsonConfig == null) {
            throw new IllegalArgumentException("Null barcode");
        }
        try {
            this.config = new JSONObject(jsonConfig);
        } catch (JSONException e) {
            return;
        }
    }

    public boolean hasNsLiteConfig() {
        try {
            return config.has(NSLITE_CONFIG) &&
                    config.getJSONObject(NSLITE_CONFIG)
                            .getJSONArray(API_URI).length() > 0;
        } catch (JSONException e) {
            return false;
        }
    }

    public List<String> getApiUris() {
        List<String> apiUris = new ArrayList<>();
        if (hasNsLiteConfig()) {
            JSONArray jsonArray = null;
            try {
                jsonArray = config.getJSONObject(NSLITE_CONFIG)
                        .getJSONArray(API_URI);
            } catch (JSONException e) {
                UserError.Log.d(TAG, "Invalid json array: " + config.toString());
                return apiUris;
            }
            for (int index = 0; index < jsonArray.length(); index++) {
                try {
                    apiUris.add(jsonArray.getString(index));
                } catch (JSONException e) {
                    UserError.Log.d(TAG, "Invalid child json object: " + config.toString());
                }
            }
        }
        return apiUris;
    }

    public static String getJsonFromSetting(String setting) {
            val split = setting.trim().split(" ");
            val hm = new HashMap<String, Object>();
            val ep = new HashMap<String, String[]>();
            ep.put(API_URI, split);
            hm.put(NSLITE_CONFIG, ep);
            return JoH.defaultGsonInstance().toJson(hm);
    }

}
