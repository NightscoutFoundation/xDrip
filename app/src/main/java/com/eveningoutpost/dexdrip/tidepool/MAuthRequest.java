package com.eveningoutpost.dexdrip.tidepool;

// jamorham

import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import okhttp3.Credentials;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;

public class MAuthRequest extends BaseMessage {

    public static String getAuthRequestHeader() {

        final String username = Pref.getString("tidepool_username", null);
        final String password = Pref.getString("tidepool_password", null);

        if (emptyString(username) || emptyString(password)) return null;
        return Credentials.basic(username.trim(), password);
    }
}



