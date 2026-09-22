package com.eveningoutpost.dexdrip.nocturne;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.Test;
import org.nightscoutfoundation.nocturne.model.OAuthTokenResponse;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for the pure decision logic in {@link NocturneOAuthService}:
 * what counts as an OAuth error (drives credential clearing), which hosts
 * are exempt from the http-to-https upgrade, and refresh token rotation.
 */
public class NocturneOAuthServiceTest extends RobolectricTestWithConfig {

    private static final String CLOUDFLARE_HTML =
            "<!DOCTYPE html><html><body>Cross-site POST form submissions are forbidden</body></html>";

    // ---- OAuth error classification (decides whether credentials are wiped) ----

    @Test
    public void oauthErrorCode_extractsErrorField() {
        assertThat(NocturneOAuthService.oauthErrorCode("{\"error\":\"authorization_pending\"}"))
                .isEqualTo("authorization_pending");
        assertThat(NocturneOAuthService.oauthErrorCode("{\"error\":\"invalid_grant\",\"error_description\":\"expired\"}"))
                .isEqualTo("invalid_grant");
    }

    @Test
    public void oauthErrorCode_nonOAuthBodies_returnEmpty() {
        assertThat(NocturneOAuthService.oauthErrorCode(CLOUDFLARE_HTML)).isEmpty();
        assertThat(NocturneOAuthService.oauthErrorCode("{\"message\":\"no error field\"}")).isEmpty();
        assertThat(NocturneOAuthService.oauthErrorCode("")).isEmpty();
        assertThat(NocturneOAuthService.oauthErrorCode(null)).isEmpty();
    }

    @Test
    public void isOAuthErrorResponse_cloudflareHtml_mustNotCountAsOAuthError() {
        // A proxy/CDN 403 with an HTML body must not clear stored credentials
        assertThat(NocturneOAuthService.isOAuthErrorResponse(CLOUDFLARE_HTML)).isFalse();
    }

    @Test
    public void isOAuthErrorResponse_invalidGrantJson_countsAsOAuthError() {
        assertThat(NocturneOAuthService.isOAuthErrorResponse("{\"error\":\"invalid_grant\"}")).isTrue();
    }

    // ---- Local host detection (decides http-to-https upgrade exemption) ----

    @Test
    public void isLocalHost_recognisesPrivateAddresses() {
        assertThat(NocturneOAuthService.isLocalHost("localhost")).isTrue();
        assertThat(NocturneOAuthService.isLocalHost("127.0.0.1")).isTrue();
        assertThat(NocturneOAuthService.isLocalHost("192.168.1.5")).isTrue();
        assertThat(NocturneOAuthService.isLocalHost("10.0.0.2")).isTrue();
        assertThat(NocturneOAuthService.isLocalHost("172.16.0.1")).isTrue();
        assertThat(NocturneOAuthService.isLocalHost("172.31.255.255")).isTrue();
        assertThat(NocturneOAuthService.isLocalHost("nocturne.local")).isTrue();
        assertThat(NocturneOAuthService.isLocalHost("[::1]")).isTrue();
        assertThat(NocturneOAuthService.isLocalHost("[fd00::1]")).isTrue();
        assertThat(NocturneOAuthService.isLocalHost("[fe80::1]")).isTrue();
    }

    @Test
    public void isLocalHost_publicAddressesAreNotLocal() {
        assertThat(NocturneOAuthService.isLocalHost("example.com")).isFalse();
        assertThat(NocturneOAuthService.isLocalHost("8.8.8.8")).isFalse();
        // 172.16.0.0/12 boundaries: 172.15.x and 172.32.x are public
        assertThat(NocturneOAuthService.isLocalHost("172.15.0.1")).isFalse();
        assertThat(NocturneOAuthService.isLocalHost("172.32.0.1")).isFalse();
        assertThat(NocturneOAuthService.isLocalHost("[2001:db8::1]")).isFalse();
        // named host that merely starts with a private-looking prefix
        assertThat(NocturneOAuthService.isLocalHost("172.example.com")).isFalse();
    }

    @Test
    public void extractHost_handlesPortsPathsAndIpv6() {
        assertThat(NocturneOAuthService.extractHost("example.com/path")).isEqualTo("example.com");
        assertThat(NocturneOAuthService.extractHost("example.com:8080/path")).isEqualTo("example.com");
        assertThat(NocturneOAuthService.extractHost("example.com")).isEqualTo("example.com");
        assertThat(NocturneOAuthService.extractHost("[::1]:5000/path")).isEqualTo("[::1]");
        assertThat(NocturneOAuthService.extractHost("[fd00::1]")).isEqualTo("[fd00::1]");
    }

    // ---- getBaseUrl (decides whether OAuth traffic goes cleartext or https) ----

    @Test
    public void getBaseUrl_upgradesPublicHttpToHttps() {
        Pref.setString("nocturne_instance_url", "http://example.com");
        assertThat(new NocturneOAuthService().getBaseUrl()).isEqualTo("https://example.com/");
    }

    @Test
    public void getBaseUrl_keepsLanHttp() {
        Pref.setString("nocturne_instance_url", "http://192.168.1.5:1337");
        assertThat(new NocturneOAuthService().getBaseUrl()).isEqualTo("http://192.168.1.5:1337/");
    }

    @Test
    public void getBaseUrl_leavesHttpsAloneAndAddsTrailingSlash() {
        Pref.setString("nocturne_instance_url", "https://nocturne.example.com");
        assertThat(new NocturneOAuthService().getBaseUrl()).isEqualTo("https://nocturne.example.com/");
    }

    @Test
    public void getBaseUrl_emptyStaysEmpty() {
        Pref.setString("nocturne_instance_url", "");
        assertThat(new NocturneOAuthService().getBaseUrl()).isEmpty();
    }

    // ---- Refresh token rotation (RFC 6749 §6 allows omitting refresh_token) ----

    @Test
    public void storeTokens_withoutRotatedRefreshToken_keepsExistingOne() {
        PersistentStore.setString("nocturne_refresh_token", "old-refresh");
        new NocturneOAuthService().storeTokens(new OAuthTokenResponse()
                .accessToken("new-access")
                .expiresIn(3600));
        assertThat(PersistentStore.getString("nocturne_access_token")).isEqualTo("new-access");
        assertThat(PersistentStore.getString("nocturne_refresh_token")).isEqualTo("old-refresh");
    }

    @Test
    public void storeTokens_withRotatedRefreshToken_replacesExistingOne() {
        PersistentStore.setString("nocturne_refresh_token", "old-refresh");
        new NocturneOAuthService().storeTokens(new OAuthTokenResponse()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .expiresIn(3600));
        assertThat(PersistentStore.getString("nocturne_refresh_token")).isEqualTo("new-refresh");
    }
}
