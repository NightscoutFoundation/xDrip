package com.eveningoutpost.dexdrip.tidepool;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.junit.Test;

import java.io.IOException;

/**
 * Tests for {@link AuthFlowIn#isTransientTokenError} — the classifier that decides whether a
 * failed Tidepool token refresh should be retried silently (transient network/server error) or
 * fall back to an interactive browser re-login (rejected credential).
 *
 * @author Asbjørn Aarrestad
 */
public class AuthFlowInTest extends RobolectricTestWithConfig {

    @Test
    public void networkError_isTransient() {
        // :: Setup — mirror what AppAuth produces on a failed refresh over a bad network:
        // a fresh instance built from the NETWORK_ERROR template ({"type":0,"code":3}).
        final AuthorizationException networkError = AuthorizationException.fromTemplate(
                AuthorizationException.GeneralErrors.NETWORK_ERROR, new IOException("timeout"));

        // :: Act & Verify
        assertThat(AuthFlowIn.isTransientTokenError(networkError)).isTrue();
    }

    @Test
    public void serverError_isTransient() {
        // :: Setup
        final AuthorizationException serverError = AuthorizationException.fromTemplate(
                AuthorizationException.GeneralErrors.SERVER_ERROR, new IOException("503"));

        // :: Act & Verify
        assertThat(AuthFlowIn.isTransientTokenError(serverError)).isTrue();
    }

    @Test
    public void invalidGrant_isNotTransient() {
        // :: Verify — a rejected credential (invalid_grant) is an OAuth token error, not transient.
        assertThat(AuthFlowIn.isTransientTokenError(
                AuthorizationException.TokenRequestErrors.INVALID_GRANT)).isFalse();
    }

    @Test
    public void otherGeneralError_isNotTransient() {
        // :: Verify — a general error that is not a connectivity/server failure (here: JSON
        // deserialization, type 0 code 5) must NOT be transient. Without this the code-level
        // discrimination would be dead and every general error would suppress re-login.
        assertThat(AuthFlowIn.isTransientTokenError(
                AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR)).isFalse();
    }

    @Test
    public void nullException_isNotTransient() {
        // :: Verify
        assertThat(AuthFlowIn.isTransientTokenError(null)).isFalse();
    }

    // classifyFreshTokenResult — the full outcome decision the fresh-token callback acts on.

    @Test
    public void noAccessTokenWithTransientError_retriesSilently() {
        // :: Setup
        final AuthorizationException networkError = AuthorizationException.fromTemplate(
                AuthorizationException.GeneralErrors.NETWORK_ERROR, new IOException("timeout"));

        // :: Act & Verify — the fix: a transient failure keeps the session, no re-login.
        assertThat(AuthFlowIn.classifyFreshTokenResult(null, null, networkError))
                .isEqualTo(AuthFlowIn.TokenOutcome.RETRY_SILENTLY);
    }

    @Test
    public void noAccessTokenWithRejectedCredential_retriesLogin() {
        // :: Act & Verify — a genuine credential failure still forces interactive re-login.
        assertThat(AuthFlowIn.classifyFreshTokenResult(
                null, null, AuthorizationException.TokenRequestErrors.INVALID_GRANT))
                .isEqualTo(AuthFlowIn.TokenOutcome.RETRY_LOGIN_TOKEN_REJECTED);
    }

    @Test
    public void accessTokenButNoLastResponse_retriesLogin() {
        // :: Act & Verify — have a token but no stored response/token type: re-login as before.
        assertThat(AuthFlowIn.classifyFreshTokenResult("access-token", null, null))
                .isEqualTo(AuthFlowIn.TokenOutcome.RETRY_LOGIN_NO_TOKEN_TYPE);
    }

    @Test
    public void accessTokenWithLastResponse_startsSession() {
        // :: Setup — a minimal real TokenResponse (refresh-token grant needs only a refresh token).
        final AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(
                Uri.parse("https://auth.example/authorize"), Uri.parse("https://auth.example/token"));
        final TokenRequest request = new TokenRequest.Builder(config, "xdrip")
                .setGrantType("refresh_token")
                .setRefreshToken("refresh-token")
                .build();
        final TokenResponse lastResponse = new TokenResponse.Builder(request).build();

        // :: Act & Verify — happy path: valid token and a stored response starts the session.
        assertThat(AuthFlowIn.classifyFreshTokenResult("access-token", lastResponse, null))
                .isEqualTo(AuthFlowIn.TokenOutcome.START_SESSION);
    }
}
