package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

/**
 * Verifies that NightscoutFollowV3 maps HTTP error codes to user-friendly messages.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowV3ErrorTest extends RobolectricTestWithConfig {

    @Test
    public void httpErrorMsg_returns401Message() {
        String status = "NS entries download was not successful: 401 Unauthorized";
        assertThat(NightscoutFollowV3.httpErrorMsg(status))
                .isEqualTo("Authentication failed — v3 needs a role-based access token (not API secret)");
    }

    @Test
    public void httpErrorMsg_returns403Message() {
        String status = "NS entries download was not successful: 403 Forbidden";
        assertThat(NightscoutFollowV3.httpErrorMsg(status))
                .isEqualTo("Access denied — check token permissions");
    }

    @Test
    public void httpErrorMsg_returns404Message() {
        String status = "NS entries download was not successful: 404 Not Found";
        assertThat(NightscoutFollowV3.httpErrorMsg(status))
                .isEqualTo("API v3 not found — server may not support v3");
    }

    @Test
    public void httpErrorMsg_returnsConnectionFailedMessage() {
        String status = "NS entries download Failed: java.net.ConnectException: Connection refused";
        assertThat(NightscoutFollowV3.httpErrorMsg(status))
                .isEqualTo("Connection failed — check your Nightscout URL");
    }

    @Test
    public void httpErrorMsg_returnsRawStatusForUnknownErrors() {
        String status = "NS entries download was not successful: 500 Internal Server Error";
        assertThat(NightscoutFollowV3.httpErrorMsg(status))
                .isEqualTo(status);
    }

    @Test
    public void httpErrorMsg_handlesNull() {
        assertThat(NightscoutFollowV3.httpErrorMsg(null)).isEqualTo("Unknown error");
    }
}
