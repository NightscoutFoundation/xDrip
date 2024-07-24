package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.annotations.Expose;

/**
 * jamorham
 *
 * Representation for error response json
 */

public class ShareErrorResponse {

    private static final String TAG = "ShareErrorResponse";

    @Expose
    public String Code;
    @Expose
    public String Message;
    @Expose
    public String SubCode;
    @Expose
    public String TypeName;

    // clean up a bit for nano status
    public String getNicerMessage() {
        if (Message != null) {
            return Message.replace("Simulate SSO: ", "").replaceAll("Authentication failed. AccountId=[^ ]*", xdrip.gs(R.string.authentication_failed_wrong_password));
        } else {
            return "";
        }
    }

    public static ShareErrorResponse fromJson(String json) {
        try {
            return JoH.defaultGsonInstance().fromJson(json, ShareErrorResponse.class);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception processing fromJson() " + e);
            UserError.Log.e(TAG, "json = " + json);
            return null;
        }
    }

}
