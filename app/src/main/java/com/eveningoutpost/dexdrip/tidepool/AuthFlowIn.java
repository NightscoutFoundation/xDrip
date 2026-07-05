package com.eveningoutpost.dexdrip.tidepool;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.models.JoH.pratelimit;
import static com.eveningoutpost.dexdrip.tidepool.AuthFlowOut.eraseAuthState;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.TokenResponse;

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

    private static final boolean DEBUG = false;

    final AtomicReference<JSONObject> userInfo = new AtomicReference<>();

    public void onCreate(final Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        val intent = getIntent();
        Log.d(TAG, "Got response");
        if (DEBUG) {
            val extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Object value = extras.get(key);
                    Log.d(TAG, key + " = " + value + " (" + (value != null ? value.getClass().getName() : "null") + ")");
                }
            } else {
                Log.d(TAG, "No extras found in intent.");
            }
        }
        Inevitable.task("tidepool-process-auth", 10, () -> processIntent(intent));
        this.finish();
    }

    private void processIntent(final Intent intent) {
        if (intent == null) {
            Log.wtf(TAG, "Intent is null when trying to process intent");
            return;
        }
        val authorizationResponse = AuthorizationResponse.fromIntent(intent);
        val authorizationException = AuthorizationException.fromIntent(intent);
        val state = AuthFlowOut.getAuthState();
        if (state == null) {
            Log.wtf(TAG, "Could not get auth state");
            return;
        }
        if (authorizationResponse == null && authorizationException == null) {
            Log.wtf(TAG, "Both response and exception are null when processing intent?");
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

    /**
     * True for a transient token-refresh failure (no connectivity or a server-side error), where the
     * stored refresh token is still valid and the next sync should retry silently. A rejected
     * credential instead surfaces as an OAuth token error ({@code invalid_grant}) and still forces re-login.
     */
    @VisibleForTesting
    static boolean isTransientTokenError(final AuthorizationException ex) {
        if (ex == null) {
            return false;
        }
        return ex.type == AuthorizationException.TYPE_GENERAL_ERROR
                && (ex.code == AuthorizationException.GeneralErrors.NETWORK_ERROR.code
                || ex.code == AuthorizationException.GeneralErrors.SERVER_ERROR.code);
    }

    /** The four outcomes of a fresh-token attempt; isolated from side effects to stay unit-testable. */
    @VisibleForTesting
    enum TokenOutcome { START_SESSION, RETRY_LOGIN_NO_TOKEN_TYPE, RETRY_LOGIN_TOKEN_REJECTED, RETRY_SILENTLY }

    /**
     * Decide what to do with a fresh-token result: a usable access token with a stored response starts
     * a session; a missing token forces interactive re-login unless the failure was transient
     * ({@link #isTransientTokenError}), in which case the next sync retries silently.
     */
    @VisibleForTesting
    static TokenOutcome classifyFreshTokenResult(final String accessToken,
                                                 final TokenResponse lastResponse,
                                                 final AuthorizationException ex) {
        if (accessToken == null) {
            return isTransientTokenError(ex) ? TokenOutcome.RETRY_SILENTLY : TokenOutcome.RETRY_LOGIN_TOKEN_REJECTED;
        }
        return lastResponse != null ? TokenOutcome.START_SESSION : TokenOutcome.RETRY_LOGIN_NO_TOKEN_TYPE;
    }

    public static void handleTokenLoginAndStartSession() {
        val state = AuthFlowOut.getAuthState();
        if (state != null) {
            val service = AuthFlowOut.getAuthService();
            state.performActionWithFreshTokens(service, (accessToken, idToken, tokenException) -> {
                if (tokenException != null) {
                    Log.e(TAG, "Got exception token: " + tokenException);
                }
                val lastResponse = accessToken != null ? state.getLastTokenResponse() : null;
                switch (classifyFreshTokenResult(accessToken, lastResponse, tokenException)) {
                    case START_SESSION: {
                        val session = new Session(lastResponse.tokenType, TidepoolUploader.getSESSION_TOKEN_HEADER());
                        session.authReply = new MAuthReply(idToken);
                        session.token = accessToken;
                        session.authReply.userid = Pref.getStringDefaultBlank(PREF_TIDEPOOL_SUB_NAME);
                        TidepoolUploader.startSession(session, false);
                        AuthFlowOut.saveAuthState();
                        break;
                    }
                    case RETRY_LOGIN_NO_TOKEN_TYPE:
                        Log.e(TAG, "Failing to get response / token type - trying initial login again");
                        retryInitialLogin();
                        break;
                    case RETRY_LOGIN_TOKEN_REJECTED:
                        Log.e(TAG, "Failing to use access token - trying initial login again");
                        retryInitialLogin();
                        break;
                    case RETRY_SILENTLY:
                        Log.d(TAG, "Token refresh failed transiently - keeping session, will retry on next sync: " + tokenException);
                        break;
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