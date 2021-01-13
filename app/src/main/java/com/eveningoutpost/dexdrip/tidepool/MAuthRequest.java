package com.eveningoutpost.dexdrip.tidepool;


import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import okhttp3.Credentials;

import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;

// jamorham

public class MAuthRequest extends BaseMessage {

    public static String getAuthRequestHeader() {

        final String username = Pref.getString("tidepool_username", null);
        final String password = Pref.getString("tidepool_password", null);

        if (emptyString(username) || emptyString(password)) return null;
        return Credentials.basic(username.trim(), password);
    }
}



