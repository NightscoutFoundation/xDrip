package com.eveningoutpost.dexdrip.tidepool;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.models.JoH.pratelimit;
import static com.eveningoutpost.dexdrip.tidepool.AuthFlowOut.eraseAuthState;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import lombok.val;
import okio.Okio;

/**
 * JamOrHam
 * <p>
 * Handle inbound authentication events for Tidepool new authentication mechanism
 */

public class AuthFlowIn extends AppCompatActivity {

    private static final String TAG = "TidePoolAuth";
    private static final String PREF_TIDEPOOL_USER_NAME = "tidepool_username";
    private static final String PREF_TIDEPOOL_SUB_NAME = "tidepool_subname";

    final AtomicReference<JSONObject> userInfo = new AtomicReference<>();

    public void onCreate(final Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        Log.d(TAG, "Got response");
        Inevitable.task("tidepool-process-auth", 10, () -> processIntent(getIntent()));
        this.finish();
    }

    private void processIntent(final Intent intent) {
        val authorizationResponse = AuthorizationResponse.fromIntent(intent);
        val authorizationException = AuthorizationException.fromIntent(intent);
        val state = AuthFlowOut.getAuthState();
        if (state == null) {
            Log.wtf(TAG, "Could not get auth state");
            return;
        }
        state.update(authorizationResponse, authorizationException);
        if (authorizationException != null) {
            Log.d(TAG, "Got authorization error - resetting state: " + authorizationException);
            eraseAuthState();
        }
        if (authorizationResponse != null) {
            // authorization completed
            AuthFlowOut.saveAuthState();

            val service = AuthFlowOut.getAuthService();
            service.performTokenRequest(
                    authorizationResponse.createTokenExchangeRequest(),
                    (tokenResponse, exception) -> {
                        state.update(tokenResponse, exception);
                        if (exception != null) {
                            Log.d(TAG, "Token request exception: " + exception);
                            eraseAuthState();
                        }
                        if (tokenResponse != null) {
                            Log.d(TAG, "Got first token");
                            AuthFlowOut.saveAuthState();

                            val configuration = state.getAuthorizationServiceConfiguration();
                            if (configuration == null) {
                                Log.wtf(TAG, "Got null for authorization service configuration");
                                return;
                            }
                            val discoveryDoc = configuration.discoveryDoc;
                            if (discoveryDoc == null) {
                                Log.wtf(TAG, "Got null for discoveryDoc");
                                return;
                            }
                            val userInfoEndpoint = discoveryDoc.getUserinfoEndpoint();
                            if (userInfoEndpoint == null) {
                                Log.wtf(TAG, "Got null for userInfoEndpoint");
                                return;
                            }

                            Inevitable.task("tidepool-get-userinfo", 100, () -> {
                                try {
                                    val conn = AppAuthConfiguration.DEFAULT.getConnectionBuilder()
                                            .openConnection(userInfoEndpoint);
                                    // TODO should hardcoded Bearer be replaced by token type?
                                    conn.setRequestProperty("Authorization", "Bearer " + tokenResponse.accessToken);
                                    conn.setInstanceFollowRedirects(false);
                                    val response = Okio.buffer(Okio.source(conn.getInputStream()))
                                            .readString(StandardCharsets.UTF_8);
                                    Log.d(TAG, "UserInfo: " + response);
                                    userInfo.set(new JSONObject(response));

                                    state.performActionWithFreshTokens(service, (accessToken, idToken, authorizationException1) -> {
                                        if (authorizationException1 != null) {
                                            Log.e(TAG, "Got fresh token exception: " + authorizationException1);
                                            return;
                                        }
                                        val session = new Session(tokenResponse.tokenType, TidepoolUploader.getSESSION_TOKEN_HEADER());
                                        session.authReply = new MAuthReply(idToken);
                                        session.token = accessToken;
                                        try {
                                            val email = userInfo.get().getString("email");
                                            if (!emptyString(email)) {
                                                Log.d(TAG, "Setting username to: " + email);
                                                Pref.setString(PREF_TIDEPOOL_USER_NAME, email);
                                            } else {
                                                Log.wtf(TAG, "Could not get userinfo email");
                                            }
                                            session.authReply.userid = userInfo.get().getString("sub");
                                            if (!emptyString(session.authReply.userid)) {
                                                Pref.setString(PREF_TIDEPOOL_SUB_NAME, session.authReply.userid);
                                                TidepoolUploader.startSession(session, true);
                                            } else {
                                                Log.wtf(TAG, "Could not get 'sub' field - cannot proceed");
                                            }
                                        } catch (JSONException e) {
                                            Log.wtf(TAG, "Getting Access Token 1 Exception: " + e);
                                        }
                                    });

                                } catch (IOException ioException) {
                                    Log.e(TAG, "Network error when querying userinfo endpoint", ioException);
                                } catch (JSONException jsonException) {
                                    Log.e(TAG, "Failed to parse userinfo response");
                                }
                            });

                        } else {
                            Log.e(TAG, "First token err: " + exception);
                        }
                    });

        } else {
            Log.e(TAG, "Got response failure " + authorizationException.toString());
        }
    }

    public static void handleTokenLoginAndStartSession() {
        val state = AuthFlowOut.getAuthState();
        if (state != null) {
            val service = AuthFlowOut.getAuthService();
            state.performActionWithFreshTokens(service, (accessToken, idToken, tokenException) -> {
                if (tokenException != null) {
                    Log.e(TAG, "Got exception token: " + tokenException);
                }
                if (accessToken != null) {
                    val lastReponse = state.getLastTokenResponse();
                    if (lastReponse != null) {
                        val session = new Session(lastReponse.tokenType, TidepoolUploader.getSESSION_TOKEN_HEADER());
                        session.authReply = new MAuthReply(idToken);
                        session.token = accessToken;
                        session.authReply.userid = Pref.getStringDefaultBlank(PREF_TIDEPOOL_SUB_NAME);
                        TidepoolUploader.startSession(session, false);
                        AuthFlowOut.saveAuthState();
                    } else {
                        Log.e(TAG, "Failing to get response / token type - trying initial login again");
                        retryInitialLogin();
                    }
                } else {
                    Log.e(TAG, "Failing to use access token - trying initial login again");
                    retryInitialLogin();
                }
            });
        } else {
            Log.e(TAG, "Failing to get state - trying initial login");
            retryInitialLogin();
        }
    }

    private static void retryInitialLogin() {
        if (pratelimit("tidepool-retry-login", 600)) {
            AuthFlowOut.doTidePoolInitialLogin();
        }
    }
}