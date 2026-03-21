package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.Before;
import org.junit.Test;

/**
 * Verifies token resolution and JWT status text in NightscoutFollowV3.
 * <p>
 * The v3 access token is embedded in {@code nsfollow_url} as RFC 3986 userinfo:
 * {@code https://mytoken@my.nightscout.com}. This matches the v1 convention where
 * API_SECRET is embedded as userinfo in the same URL field.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowV3TokenTest extends RobolectricTestWithConfig {

    @Before
    public void resetState() {
        Pref.setString("nsfollow_url", "");
        NightscoutFollowV3.resetInstance();
    }

    // ===== getToken ==============================================================================

    @Test
    public void getToken_returnsUserinfoFromUrl() {
        // :: Setup — token embedded as userinfo in the URL
        Pref.setString("nsfollow_url", "https://mytoken@my.nightscout.com");

        // :: Act + Verify
        assertThat(NightscoutFollowV3.getToken()).isEqualTo("mytoken");
    }

    @Test
    public void getToken_returnsNullWhenUrlHasNoUserinfo() {
        // :: Setup — URL without userinfo (open instance)
        Pref.setString("nsfollow_url", "https://my.nightscout.com");

        // :: Act + Verify
        assertThat(NightscoutFollowV3.getToken()).isNull();
    }

    @Test
    public void getToken_returnsNullWhenUrlIsEmpty() {
        // :: Setup — URL pref not set (cleared in @Before)

        // :: Act + Verify
        assertThat(NightscoutFollowV3.getToken()).isNull();
    }

    // ===== jwtStatusText =========================================================================

    @Test
    public void jwtStatusText_returnsNullWhenNoTokenConfigured() {
        // :: Setup — URL has no userinfo
        Pref.setString("nsfollow_url", "https://my.nightscout.com");

        assertThat(NightscoutFollowV3.jwtStatusText()).isNull();
    }

    @Test
    public void jwtStatusText_returnsNotFetchedWhenTokenSetButNoJwt() {
        // :: Setup — token in URL, JWT not yet exchanged
        Pref.setString("nsfollow_url", "https://test-74c387663745c193@my.nightscout.com");

        // :: Act + Verify
        assertThat(NightscoutFollowV3.jwtStatusText()).isEqualTo("Not fetched");
    }

    @Test
    public void jwtStatusText_returnsExpiredWhenJwtPastExpiry() {
        // :: Setup — JWT set but expired 1 second ago
        Pref.setString("nsfollow_url", "https://test-74c387663745c193@my.nightscout.com");
        NightscoutFollowV3.setJwtForTest("old-jwt", System.currentTimeMillis() - 1_000);

        // :: Act + Verify
        assertThat(NightscoutFollowV3.jwtStatusText()).isEqualTo("Expired");
    }

    @Test
    public void jwtStatusText_returnsActiveWithExpiryWhenJwtValid() {
        // :: Setup — JWT valid for 1 more hour
        Pref.setString("nsfollow_url", "https://test-74c387663745c193@my.nightscout.com");
        NightscoutFollowV3.setJwtForTest("valid-jwt", System.currentTimeMillis() + 3_600_000);

        // :: Act + Verify
        assertThat(NightscoutFollowV3.jwtStatusText()).startsWith("Active, expires in");
    }
}
