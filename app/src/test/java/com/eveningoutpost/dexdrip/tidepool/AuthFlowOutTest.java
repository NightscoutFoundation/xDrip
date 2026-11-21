package com.eveningoutpost.dexdrip.tidepool;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import net.openid.appauth.AuthState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowLog;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for loading the stored Tidepool login with {@link AuthFlowOut#getAuthState()}.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class AuthFlowOutTest extends RobolectricTestWithConfig {

    private static final String ACCESS_TOKEN = "fake-access-token-for-unit-test";
    private static final String CONFIGURATION = "{\"authorizationEndpoint\":\"https://auth.example.invalid/auth\","
            + "\"tokenEndpoint\":\"https://auth.example.invalid/token\"}";
    private static final String STORED_STATE = "{\"refreshToken\":\"fake-refresh-token\",\"scope\":\"openid\","
            + "\"mLastTokenResponse\":{\"request\":{\"configuration\":" + CONFIGURATION + ","
            + "\"clientId\":\"xdrip\",\"grantType\":\"refresh_token\",\"refreshToken\":\"fake-refresh-token\","
            + "\"additionalParameters\":{}},\"token_type\":\"Bearer\",\"access_token\":\"" + ACCESS_TOKEN + "\","
            + "\"expires_at\":1,\"refresh_token\":\"fake-refresh-token\",\"scope\":\"openid\","
            + "\"additionalParameters\":{}}}";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        AuthFlowOut.clearAllSavedData();
        Pref.setString("tidepool-service-configuration", CONFIGURATION);
        Pref.setString("tidepool-last-response", STORED_STATE);
        ShadowLog.clear();
    }

    @After
    public void tearDown() {
        AuthFlowOut.clearAllSavedData(); // no stored login is left for later tests to refresh over the network
    }

    // ===== Loading ===================================================================================================

    /** The stored login is loaded with its access token. */
    @Test
    public void storedLoginIsLoadedWithItsAccessToken() {
        // :: Act
        final AuthState state = AuthFlowOut.getAuthState();

        // :: Verify
        assertThat(state).isNotNull();
        assertThat(state.getAccessToken()).isEqualTo(ACCESS_TOKEN);
    }

    /** Loading the stored login does not write the access token to the log. */
    @Test
    public void storedLoginKeepsTheAccessTokenOutOfTheLog() {
        // :: Act
        final AuthState state = AuthFlowOut.getAuthState();

        // :: Verify
        assertThat(state.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        for (final ShadowLog.LogItem item : ShadowLog.getLogs()) {
            assertThat(item.msg).doesNotContain(ACCESS_TOKEN);
        }
    }
}
