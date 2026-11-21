package com.eveningoutpost.dexdrip.cgm.li3;

import androidx.annotation.VisibleForTesting;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.CookieJar;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import xrwd.li3.util.Reversed;

/**
 * Finds the account ID with one login to the follower service, so a user who does not know it can take over a sensor
 * started in the official app. Ported from xDrip4iOS {@code *AccountLookup.swift} (Paul Plant, GPL-3.0); unlike
 * it, this sends {@code product: llu.android}.
 * <p>
 * The email, password and the login's token are used for this one request and never kept or logged; only the
 * caller keeps the ID. The password goes only to the account service's API host and the region host the login
 * names: HTTP redirects are never followed, and at most three hosts are asked, none twice.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public final class Li3AccountLookup {

    /** Why a lookup gave no ID. */
    public enum Failure {
        /** The account service did not accept the email and password. */
        LOGIN,
        /** The account has new terms to accept in the official app first. */
        TERMS,
        /** Anything else: no connection, an unexpected answer, or an app version the service no longer accepts. */
        UNAVAILABLE
    }

    /** A lookup that gave no ID; the message names the status, HTTP code or region, never what was sent. */
    public static final class LookupException extends Exception {
        public final Failure failure;

        LookupException(Failure failure, String message) {
            super(message);
            this.failure = failure;
        }
    }

    private static final String TAG = "Li3";
    private static final String VERSION = "4.17.0"; // the version xDrip4iOS sends
    private static final MediaType JSON = MediaType.get("application/json");
    private static final Set<String> REGIONS = new HashSet<>(Arrays.asList(
            "ae", "ap", "au", "ca", "de", "eu", "eu2", "fr", "jp", "la", "us"));
    private static final int MAX_REQUESTS = 3;
    private static final String API_HOST = Reversed.of("oi.weiverbil.ipa");
    private static final String REGION_DOMAIN = Reversed.of("oi.weiverbil.");
    private static final String LOGIN_PATH = Reversed.of("nigol/htua/ull/");

    private final OkHttpClient client;

    public Li3AccountLookup(OkHttpClient client) {
        this.client = client.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .cookieJar(CookieJar.NO_COOKIES)
                .cache(null)
                .callTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @VisibleForTesting
    OkHttpClient client() {
        return client;
    }

    /**
     * Looks the account ID up and saves it as the Li3 account ID; a failure leaves the setting as it was. Only the
     * outcome is logged.
     *
     * @return the message to show the user
     */
    public static int fillAccountId(Li3AccountLookup lookup, String email, String password) {
        try {
            Pref.setString(Li3Store.PREF_ACCOUNT_ID, lookup.accountId(email, password));
            UserError.Log.uel(TAG, "Li3 account ID lookup: done");
            return R.string.li3_lookup_done;
        } catch (LookupException e) {
            UserError.Log.uel(TAG, "Li3 account ID lookup: " + e.failure + " (" + e.getMessage() + ")");
            switch (e.failure) {
                case LOGIN:
                    return R.string.li3_lookup_bad_login;
                case TERMS:
                    return R.string.li3_lookup_terms;
                default:
                    return R.string.li3_lookup_failed;
            }
        }
    }

    /**
     * Logs in once and returns the account ID.
     *
     * @return the account ID, a lower-case UUID
     * @throws LookupException when no ID was found
     */
    public String accountId(String email, String password) throws LookupException {
        try {
            return lookUp(email, password);
        } catch (IOException | JSONException | RuntimeException e) {
            throw new LookupException(Failure.UNAVAILABLE, e.getClass().getSimpleName());
        }
    }

    private String lookUp(String email, String password) throws IOException, JSONException, LookupException {
        String body = new JSONObject().put("email", email).put("password", password).toString();
        Set<String> asked = new HashSet<>();
        String host = API_HOST;
        for (int i = 0; i < MAX_REQUESTS; i++) {
            if (!asked.add(host)) {
                throw new LookupException(Failure.UNAVAILABLE, "asked " + host + " twice");
            }
            JSONObject json = login(host, body);
            Object status = json.opt("status");
            if (!(status instanceof Integer)) {
                throw new LookupException(Failure.UNAVAILABLE, "no status");
            }
            int code = (Integer) status;
            if (code == 2) {
                throw new LookupException(Failure.LOGIN, "status 2");
            }
            if (code == 4) {
                throw new LookupException(Failure.TERMS, "status 4");
            }
            JSONObject data = json.optJSONObject("data");
            if (code != 0 || data == null) {
                throw new LookupException(Failure.UNAVAILABLE, "status " + code);
            }
            if (data.optBoolean("redirect")) {
                String region = data.optString("region");
                if (!REGIONS.contains(region)) {
                    throw new LookupException(Failure.UNAVAILABLE, "region " + region);
                }
                host = "api-" + region + REGION_DOMAIN;
                continue;
            }
            JSONObject user = data.optJSONObject("user");
            String id = user == null ? "" : user.optString("id");
            if (!isUuid(id)) {
                throw new LookupException(Failure.UNAVAILABLE, "no account ID");
            }
            return id.toLowerCase(Locale.ROOT);
        }
        throw new LookupException(Failure.UNAVAILABLE, "too many redirects");
    }

    private JSONObject login(String host, String body) throws IOException, JSONException, LookupException {
        Request request = new Request.Builder()
                .url("https://" + host + LOGIN_PATH)
                .header("product", "llu.android")
                .header("version", VERSION)
                .post(RequestBody.create(body, JSON))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new LookupException(Failure.UNAVAILABLE, "http " + response.code());
            }
            ResponseBody responseBody = response.body();
            return new JSONObject(responseBody.string());
        }
    }

    private static boolean isUuid(String id) {
        if (id.length() != 36) {
            return false;
        }
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
