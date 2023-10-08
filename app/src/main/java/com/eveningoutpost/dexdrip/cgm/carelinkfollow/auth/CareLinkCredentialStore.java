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
    public final static int TOKEN_EXPIRED = 1;
    public final static int AUTHENTICATED = 2;

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
                    instance.setCredential(savedCred.country, savedCred.accessToken, savedCred.tokenValidTo, savedCred.cookies, false);
                } catch (Exception e) {
                    UserError.Log.d(TAG, "Error when restoring saved Credential: " + e.getMessage());
                }
            } else {
                UserError.Log.d(TAG, "No saved Credential found!");
            }

        }
        return instance;
    }

    synchronized void setCredential(String country, String accessToken, Date tokenValidTo, Cookie[] cookies) {
        this.setCredential(country, accessToken, tokenValidTo, cookies, true);
    }

    protected synchronized void setCredential(String country, String accessToken, Date tokenValidTo, Cookie[] cookies, boolean save) {
        credential = new CareLinkCredential();
        credential.country = country;
        credential.accessToken = accessToken;
        credential.cookies = cookies;
        credential.tokenValidTo = tokenValidTo;
        if (credential.accessToken == null || credential.tokenValidTo == null)
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

    public long getExpiresIn() {
        if (credential == null || credential.tokenValidTo == null)
            return -1;
        else
            return credential.tokenValidTo.getTime() - Calendar.getInstance().getTime().getTime();
    }

    public long getExpiresOn() {
        if (credential == null || credential.tokenValidTo == null)
            return -1;
        else
            return credential.tokenValidTo.getTime();
    }

    synchronized void clear() {
        this.credential = null;
        PersistentStore.setString(PREF_CARELINK_CREDENTIAL, "");
        UserError.Log.d(TAG, "Credential cleared");
        authStatus = NOT_AUTHENTICATED;
    }

    protected void evaluateExpiration() {
        if (this.getExpiresIn() < 0)
            authStatus = TOKEN_EXPIRED;
        else
            authStatus = AUTHENTICATED;
    }

}
