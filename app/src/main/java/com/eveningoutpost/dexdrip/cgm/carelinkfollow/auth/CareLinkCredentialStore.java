package com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth;


import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.google.gson.GsonBuilder;

import java.util.Calendar;
import java.util.Date;

import okhttp3.Cookie;

public class CareLinkCredentialStore {

    private static final String TAG = "CareLinkCredentialStore";

    public final static int NOT_AUTHENTICATED = 0;
    public final static int ACCESS_EXPIRED = 1;
    public final static int AUTHENTICATED = 2;
    public final static int REFRESH_EXPIRED = 3;

    private CareLinkCredential credential = null;
    private static CareLinkCredentialStore instance = null;
    private final static String PREF_CARELINK_CREDENTIAL = "carelink_credential";

    private int authStatus = NOT_AUTHENTICATED;


    private CareLinkCredentialStore() {

    }

    public static CareLinkCredentialStore getInstance() {
        if (instance == null) {
            instance = new CareLinkCredentialStore();
            UserError.Log.d(TAG, "Trying to restore saved Credential");
            String credJson = PersistentStore.getString(PREF_CARELINK_CREDENTIAL, "");
            if (!credJson.equals("")) {
                try {
                    CareLinkCredential savedCred = new GsonBuilder().create().fromJson(credJson, CareLinkCredential.class);
                    instance.setCredential(savedCred.country, savedCred.authType, savedCred.accessToken, savedCred.accessValidTo, savedCred.refreshValidTo, savedCred.cookies,
                            savedCred.androidModel, savedCred.deviceId, savedCred.clientId, savedCred.clientSecret, savedCred.magIdentifier, savedCred.refreshToken, false);
                } catch (Exception e) {
                    UserError.Log.d(TAG, "Error when restoring saved Credential: " + e.getMessage());
                }
            } else {
                UserError.Log.d(TAG, "No saved Credential found!");
            }

        }
        return instance;
    }

    synchronized void setMobileAppCredential(String country, String deviceId, String androidModel, String clientId, String clientSecret, String magIdentifier, String accessToken, String refreshToken, Date accessValidTo, Date refreshValidTo) {
        this.setCredential(country, CareLinkAuthType.MobileApp, accessToken, accessValidTo, refreshValidTo, null, androidModel, deviceId, clientId, clientSecret, magIdentifier, refreshToken, true);
    }

    synchronized void updateMobileAppCredential(String accessToken, Date accessValidTo, Date refreshValidTo, String refreshToken) {
        this.setCredential(credential.country, CareLinkAuthType.MobileApp, accessToken, accessValidTo, refreshValidTo, null, credential.androidModel, credential.deviceId, credential.clientId, credential.clientSecret, credential.magIdentifier, refreshToken, true);
    }

    synchronized void updateBrowserCredential(String accessToken, Date accessValidTo, Date refreshValidTo, Cookie[] cookies) {
        this.setCredential(credential.country, CareLinkAuthType.Browser, accessToken, accessValidTo, refreshValidTo, cookies, null, null, null, null, null, null, true);
    }

    synchronized void setBrowserCredential(String country, String accessToken, Date accessValidTo, Date refreshValidTo, Cookie[] cookies) {
        this.setCredential(country, CareLinkAuthType.Browser, accessToken, accessValidTo, refreshValidTo, cookies, null, null, null, null, null, null, true);
    }

    protected synchronized void setCredential(String country, CareLinkAuthType authType, String accessToken, Date accessValidTo, Date refreshValidTo, Cookie[] cookies, String androidModel, String deviceId, String clientId, String clientSecret, String magIdentifier, String refreshToken, boolean save) {

        credential = new CareLinkCredential();
        credential.authType = authType;
        credential.country = country;
        credential.accessToken = accessToken;
        credential.accessValidTo = accessValidTo;
        credential.cookies = cookies;
        credential.androidModel = androidModel;
        credential.deviceId = deviceId;
        credential.clientId = clientId;
        credential.clientSecret = clientSecret;
        credential.magIdentifier = magIdentifier;
        credential.refreshToken = refreshToken;
        credential.refreshValidTo = refreshValidTo;
        if (credential.accessToken == null || credential.accessValidTo == null)
            authStatus = NOT_AUTHENTICATED;
        else
            evaluateExpiration();
        UserError.Log.d(TAG, "Credential updated!");
        if (save) {
            try {
                PersistentStore.setString(PREF_CARELINK_CREDENTIAL, new GsonBuilder().create().toJson(credential));
                UserError.Log.d(TAG, "Credential saved!");
            } catch (Exception e) {
                UserError.Log.d(TAG, "Error saving Credential:  " + e.getMessage());
            }
        }
    }

    public CareLinkCredential getCredential() {
        return credential;
    }

    public int getAuthStatus() {
        return authStatus;
    }

    public long getAccessExpiresIn() {
        if (credential == null || credential.accessValidTo == null)
            return -1;
        else
            return credential.accessValidTo.getTime() - Calendar.getInstance().getTime().getTime();
    }

    public long getAccessExpiresOn() {
        if (credential == null || credential.accessValidTo == null)
            return -1;
        else
            return credential.accessValidTo.getTime();
    }

    public long getRefreshExpiresIn() {
        if (credential == null || credential.refreshValidTo == null)
            return -1;
        else
            return credential.refreshValidTo.getTime() - Calendar.getInstance().getTime().getTime();
    }

    public long getRefreshExpiresOn() {
        if (credential == null || credential.refreshValidTo == null)
            return -1;
        else
            return credential.refreshValidTo.getTime();
    }

    synchronized void clear() {
        this.credential = null;
        PersistentStore.setString(PREF_CARELINK_CREDENTIAL, "");
        UserError.Log.d(TAG, "Credential cleared");
        authStatus = NOT_AUTHENTICATED;
    }

    protected void evaluateExpiration() {
        if (this.getRefreshExpiresIn() < 0)
            authStatus = REFRESH_EXPIRED;
        else if (this.getAccessExpiresIn() < 0)
            authStatus = ACCESS_EXPIRED;
        else
            authStatus = AUTHENTICATED;
    }

}
