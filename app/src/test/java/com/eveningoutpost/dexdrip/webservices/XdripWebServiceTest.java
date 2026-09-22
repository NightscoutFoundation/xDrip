package com.eveningoutpost.dexdrip.webservices;

import static com.eveningoutpost.dexdrip.HexTestTools.sha1Hex;
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

/**
 * Characterization tests for {@link XdripWebService#hashPassword(String)}.
 *
 * <p>Pins the output against an independently computed UTF-8 SHA-1 (JDK
 * {@link java.security.MessageDigest}, not Guava) so the hashing behaviour is
 * guaranteed to survive refactoring away from Guava's deprecated
 * {@code Charsets} helper.
 *
 * @author Asbjørn Aarrestad
 */
public class XdripWebServiceTest {

    @Test
    public void hashPassword_returnsUtf8Sha1Hex_forKnownSecret() {
        // :: Act
        final String hashed = XdripWebService.hashPassword("my-api-secret");

        // :: Verify
        assertWithMessage("UTF-8 SHA-1 of the secret")
                .that(hashed)
                .isEqualTo(sha1Hex("my-api-secret"));
    }

    @Test
    public void hashPassword_returnsNull_forEmptySecret() {
        // :: Act & Verify
        assertWithMessage("empty secret must not be hashed")
                .that(XdripWebService.hashPassword(""))
                .isNull();
    }
}
