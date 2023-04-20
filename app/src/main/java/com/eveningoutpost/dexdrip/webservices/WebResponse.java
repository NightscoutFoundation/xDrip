package com.eveningoutpost.dexdrip.webservices;

import com.eveningoutpost.dexdrip.models.UserError;

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

    public String getResultDesc() {
        switch (resultCode) {
            case 200:
                return "OK";
            case 400:
                return "Bad Request";
            case 404:
                return "Not Found";
            case 500:
                return "Server Error";
            default:
                return "Unknown";
        }
    }
}
