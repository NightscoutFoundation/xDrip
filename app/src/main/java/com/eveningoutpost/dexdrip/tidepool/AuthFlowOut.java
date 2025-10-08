package com.eveningoutpost.dexdrip.tidepool;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.xdrip;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.browser.BrowserAllowList;
import net.openid.appauth.browser.VersionedBrowserMatcher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * Handler for new style Tidepool openid auth
 */

@SuppressLint("StaticFieldLeak")
public class AuthFlowOut {

    private static final String TAG = "TidePoolAuth";

    private static final String MY_CLIENT_ID = "xdrip";
    private static final Uri MY_REDIRECT_URI = Uri.parse("xdrip://callback/tidepool");

    private static volatile AuthState authState;
    private static volatile AuthorizationService authService;

    private static final String INTEGRATION_BASE_URL = "https://auth.integration.tidepool.org/realms/integration";
    private static final String PRODUCTION_BASE_URL = "https://auth.tidepool.org/realms/tidepool";

    private static final String PREF_TIDEPOOL_SERVICE_CONFIGURATON = "tidepool-service-configuration";
    private static final String PREF_TIDEPOOL_STATE_STORE = "tidepool-last-response";

    public static AuthState getAuthState() {
        if (authState == null) {
            try {
                authState = new AuthState(AuthorizationServiceConfiguration.fromJson(Pref.getStringDefaultBlank(PREF_TIDEPOOL_SERVICE_CONFIGURATON)));
                authState = AuthState.jsonDeserialize(Pref.getStringDefaultBlank(PREF_TIDEPOOL_STATE_STORE));
                Log.d(TAG, "Auth state loaded with: " + authState.getAccessToken());
            } catch (Exception e) {
                Log.d(TAG, "Error during getAuthState - could just be empty cache data: " + e);
            }
        }
        return authState;
    }

    public static void saveAuthState() {
        val state = authState;
        if (state != null) {
            Pref.setString(PREF_TIDEPOOL_STATE_STORE, state.jsonSerializeString());
        }
    }

    public static void eraseAuthState() {
        Pref.setString(PREF_TIDEPOOL_STATE_STORE, "");
    }

    public static AuthorizationService getAuthService() {
        if (authService == null) {
            val appAuthConfig = new AppAuthConfiguration.Builder()
                    .setBrowserMatcher(new BrowserAllowList(
                            VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
                            VersionedBrowserMatcher.FIREFOX_CUSTOM_TAB,
                            VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB))
                    .build();

            authService = new AuthorizationService(xdrip.getAppContext(), appAuthConfig);
        }
        return authService;
    }

    private static synchronized void resetAll() {
        authState = null;
        authService = null;
        getAuthService();
        getAuthState();
    }

    public static synchronized void clearAllSavedData() {
        Pref.setString(PREF_TIDEPOOL_SERVICE_CONFIGURATON, "");
        eraseAuthState();
        resetAll();
    }

    public static void doTidePoolInitialLogin() {
        doTidePoolInitialLogin(false);
    }

    public static void doTidePoolInitialLogin(boolean full) {

        val context = xdrip.getAppContext();
        JoH.static_toast_long("Connecting to Tidepool");    // TODO I18n
        AuthorizationServiceConfiguration.fetchFromIssuer(
                Uri.parse((Pref.getBooleanDefaultFalse("tidepool_dev_servers")
                        ? INTEGRATION_BASE_URL : PRODUCTION_BASE_URL)),
                (serviceConfiguration, error) -> {
                    if (error != null || serviceConfiguration == null) {
                        Log.e(TAG, "failed to fetch configuration");
                        return;
                    }

                    Pref.setString(PREF_TIDEPOOL_SERVICE_CONFIGURATON, serviceConfiguration.toJsonString());
                    resetAll();

                    val codeVerifierChallengeMethod = "S256";
                    val messageDigestAlgorithm = "SHA-256";
                    val encoding = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;
                    val challenge = CipherUtils.getRandomKey(64);
                    val codeVerifier = Base64.encodeToString(challenge, encoding);
                    MessageDigest digest;
                    try {
                        digest = MessageDigest.getInstance(messageDigestAlgorithm);
                    } catch (NoSuchAlgorithmException e) {
                        Log.wtf(TAG, "Failed to get message digest: " + e);
                        return;
                    }
                    val hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
                    val codeChallenge = Base64.encodeToString(hash, encoding);

                    val authRequestBuilder = new AuthorizationRequest.Builder(
                            serviceConfiguration, // the authorization service configuration
                            MY_CLIENT_ID, // the client ID, typically pre-registered and static
                            ResponseTypeValues.CODE, // the response_type value: we want a code
                            MY_REDIRECT_URI); // the redirect URI to which the auth response is sent

                    authRequestBuilder
                            .setScopes("openid", "offline_access")
                            .setLoginHint(Pref.getString("tidepool_username", ""))
                            .setCodeVerifier(codeVerifier, codeChallenge, codeVerifierChallengeMethod);

                    if (full) {
                        // full relogin wanted
                        authRequestBuilder.setPrompt("login");
                    }

                    val authRequest = authRequestBuilder.build();

                    Log.d(TAG, "Firing off request");
                    getAuthService().performAuthorizationRequest(
                            authRequest,
                            // TODO will need mutability flag in later target sdk versions
                            PendingIntent.getActivity(context, 0, new Intent(context, AuthFlowIn.class), 0),
                            PendingIntent.getActivity(context, 0, new Intent(context, AuthFlowIn.class), 0));
                });
    }
}
