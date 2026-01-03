package com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth;

import java.util.Calendar;
import java.util.Date;

import okhttp3.Cookie;
import okhttp3.Headers;

public class CareLinkCredential {


    public String country = null;
    public String accessToken = null;
    public Cookie[] cookies = null;
    public Date accessValidTo = null;
    public Date refreshValidTo = null;
    public CareLinkAuthType authType = null;
    public String androidModel = null;
    public String clientId = null;
    public String clientSecret = null;
    public String refreshToken = null;


    public CareLinkAuthentication getAuthentication() {

        //Not authenticated
        if (this.authType == null || this.getAuthorizationFieldValue() == null)
            return null;

        //Build authentication
        Headers.Builder headers = new Headers.Builder();
        headers.add("Authorization", this.getAuthorizationFieldValue());
        return new CareLinkAuthentication(headers.build(), this.authType);

    }

    public String getToken() {
        return accessToken;
    }

    public String getAuthorizationFieldValue() {
        if (this.getToken() == null)
            return null;
        else
            return "Bearer " + this.getToken();
    }

    public long getAccessExpiresIn() {
        if (this.accessValidTo == null)
            return -1;
        else
            return this.accessValidTo.getTime() - Calendar.getInstance().getTime().getTime();
    }

}
