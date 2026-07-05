package com.eveningoutpost.dexdrip.tidepool;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import net.openid.appauth.AuthorizationException;

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
    public void invalidGrant_isNotTransient_soReloginStillHappens() {
        // :: Verify — a revoked/expired refresh token must still force interactive re-login.
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
}
