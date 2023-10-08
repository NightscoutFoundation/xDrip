package com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth;

import java.util.Calendar;
import java.util.Date;

import okhttp3.Cookie;

public class CareLinkCredential {

    public String country = null;
    public String accessToken = null;
    public Cookie[] cookies = null;
    public Date tokenValidTo = null;

    public String getToken() {
        return accessToken;
    }

    public String getAuthorizationFieldValue() {
        if (this.getToken() == null)
            return null;
        else
            return "Bearer " + this.getToken();
    }

    public long getExpiresIn() {
        if (this.tokenValidTo == null)
            return -1;
        else
            return this.tokenValidTo.getTime() - Calendar.getInstance().getTime().getTime();
    }

}
