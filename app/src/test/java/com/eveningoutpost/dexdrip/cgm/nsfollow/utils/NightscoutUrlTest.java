package com.eveningoutpost.dexdrip.cgm.nsfollow.utils;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/**
 * Unit tests for {@link NightscoutUrl} URL parsing and API version detection.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutUrlTest {

    // ===== getApiVersion =========================================================================

    @Test
    public void getApiVersion_returnsZero_forV3Url() {
        // :: Setup & Act — NightscoutUrl is v1 infrastructure; v3 is not a recognized version
        NightscoutUrl url = new NightscoutUrl("https://host/api/v3/");

        // :: Verify
        assertThat(url.getApiVersion()).isEqualTo(0);
    }

    @Test
    public void getApiVersion_returnsOne_forV1Url() {
        // :: Setup & Act
        NightscoutUrl url = new NightscoutUrl("https://host/api/v1/");

        // :: Verify
        assertThat(url.getApiVersion()).isEqualTo(1);
    }

    @Test
    public void getApiVersion_returnsZero_forUnknownPath() {
        // :: Setup & Act
        NightscoutUrl url = new NightscoutUrl("https://host/api/v2/");

        // :: Verify
        assertThat(url.getApiVersion()).isEqualTo(0);
    }

    // ===== getURI auto-append ====================================================================

    @Test
    public void getURI_autoAppendsV1_forBareUrl() {
        // :: Setup & Act
        NightscoutUrl url = new NightscoutUrl("https://host");

        // :: Verify
        assertThat(url.getURI().toString()).isEqualTo("https://host/api/v1/");
    }

    @Test
    public void getURI_doesNotModify_whenApiPathAlreadyPresent() {
        // :: Setup & Act
        NightscoutUrl url = new NightscoutUrl("https://host/api/v3/");

        // :: Verify
        assertThat(url.getURI().toString()).isEqualTo("https://host/api/v3/");
    }

    @Test
    public void getURI_doesNotModify_whenV1PathExplicitlySet() {
        // :: Setup & Act
        NightscoutUrl url = new NightscoutUrl("https://host/api/v1/");

        // :: Verify — v1 explicit URLs are preserved as-is
        assertThat(url.getURI().toString()).isEqualTo("https://host/api/v1/");
    }

    // ===== getSecret =============================================================================

    @Test
    public void getSecret_returnsToken_fromCredentialsUrl() {
        // :: Setup & Act
        NightscoutUrl url = new NightscoutUrl("https://mytoken@host/api/v3/");

        // :: Verify
        assertThat(url.getSecret()).isEqualTo("mytoken");
    }

    @Test
    public void getSecret_returnsNull_whenNoCredentials() {
        // :: Setup & Act
        NightscoutUrl url = new NightscoutUrl("https://host/api/v3/");

        // :: Verify
        assertThat(url.getSecret()).isNull();
    }
}
