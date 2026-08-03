package com.eveningoutpost.dexdrip.tidepool;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;

import org.junit.Test;
import org.robolectric.shadows.ShadowToast;

import java.io.IOException;

/**
 * Tests for {@link AuthFlowIn#isTransientTokenError} — the predicate that classifies a failed
 * Tidepool token refresh as transient (network/server error → retry silently on next sync) vs. a
 * rejected credential (→ interactive browser re-login).
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

    /**
     * The predicate tests above pin the classification. These two pin what the classification is
     * actually used for — whether a failed refresh escalates to the interactive browser login.
     * Entering that flow announces itself with a "Connecting to Tidepool" toast
     * ({@link AuthFlowOut#doTidePoolInitialLogin}), which is the observable difference the reporter
     * in #4605 sees as a login page appearing on a flaky network.
     */
    @Test
    public void transientNetworkError_keepsSessionInsteadOfLaunchingLogin() {
        // :: Setup — AppAuth hands back no access token plus {"type":0,"code":3} when the silent
        // refresh cannot reach the server. This is the exact failure captured in #4608's log.
        final AuthorizationException networkError = AuthorizationException.fromTemplate(
                AuthorizationException.GeneralErrors.NETWORK_ERROR, new IOException("no route to host"));

        // :: Act
        AuthFlowIn.onFreshTokenResult(new AuthState(), null, null, networkError);
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify — no interactive login was started, so no login page is pushed at the user.
        assertThat(ShadowToast.getTextOfLatestToast()).isNull();
    }

    @Test
    public void rejectedCredential_stillLaunchesInteractiveLogin() {
        // :: Setup — a refresh token the server refuses is not transient; the user genuinely has to
        // log in again. This guards against the fix suppressing re-login across the board.
        final AuthorizationException invalidGrant = AuthorizationException.TokenRequestErrors.INVALID_GRANT;

        // :: Act
        AuthFlowIn.onFreshTokenResult(new AuthState(), null, null, invalidGrant);
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Connecting to Tidepool");
    }
}
