package com.eveningoutpost.dexdrip.cgm.nsfollow.utils;

import static com.eveningoutpost.dexdrip.HexTestTools.sha1Hex;
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

/**
 * Characterization tests for {@link NightscoutUrl#getHashedSecret()}.
 *
 * <p>Pins the output against an independently computed UTF-8 SHA-1 (JDK
 * {@link java.security.MessageDigest}, not Guava) so the hashing behaviour is
 * guaranteed to survive refactoring away from Guava's deprecated
 * {@code Charsets} helper.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutUrlTest {

    @Test
    public void getHashedSecret_returnsUtf8Sha1Hex_forSecretInUserInfo() {
        // :: Setup
        final NightscoutUrl url = new NightscoutUrl("http://my-api-secret@example.com/");

        // :: Verify
        assertWithMessage("UTF-8 SHA-1 of the user-info secret")
                .that(url.getHashedSecret())
                .isEqualTo(sha1Hex("my-api-secret"));
    }

    @Test
    public void getHashedSecret_returnsNull_whenNoSecretPresent() {
        // :: Setup
        final NightscoutUrl url = new NightscoutUrl("http://example.com/");

        // :: Verify
        assertWithMessage("no user-info means no hashed secret")
                .that(url.getHashedSecret())
                .isNull();
    }
}
