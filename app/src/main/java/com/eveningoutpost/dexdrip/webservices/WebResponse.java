package com.eveningoutpost.dexdrip.webservices;

import com.eveningoutpost.dexdrip.Models.UserError;

import java.io.UnsupportedEncodingException;

/**
 * Created by jamorham on 06/01/2018.
 *
 * Data class for webservice responses
 */

public class WebResponse {

    private static String TAG = "WebResponse";

    byte[] bytes;
    String mimeType;
    int resultCode;

    WebResponse(String str) {
        this(str, 200, "application/json");
    }

    WebResponse(String str, int resultCode, String mimeType) {
        try {
            bytes = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            UserError.Log.wtf(TAG, "UTF8 is unsupported!");
        }
        this.mimeType = mimeType;
        this.resultCode = resultCode;
    }
}
