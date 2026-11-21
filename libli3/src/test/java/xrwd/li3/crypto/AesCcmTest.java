package xrwd.li3.crypto;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import xrwd.li3.util.Hex;

/**
 * Tests for {@link AesCcm}: AES-CCM as the Li3 handshake (7-byte nonce) and data plane (13-byte nonce) use it,
 * both with a 4-byte tag. Ported from RoundWhiteDiscKit 7dd6396 {@code AESCCMTests.swift}.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class AesCcmTest {

    private static final byte[] KEY = Hex.toBytes("c0c1c2c3c4c5c6c7c8c9cacbcccdcecf");
    private static final byte[] NONCE13 = Hex.toBytes("00000003020100a0a1a2a3a4a5");
    private static final byte[] AAD = Hex.toBytes("0001020304050607");
    private static final byte[] PLAIN = Hex.toBytes("08090a0b0c0d0e0f101112131415161718191a1b1c1d1e");

    // ==================================================== encrypt ====================================================

    /** AESCCMTests.swift:15-30: RFC 3610 packet vector 1, ciphertext followed by its 8-byte tag. */
    @Test
    public void encrypt_rfc3610Vector1_matches() throws Exception {
        // :: Act
        byte[] sealed = AesCcm.encrypt(KEY, NONCE13, AAD, PLAIN, 8);

        // :: Verify
        assertThat(Hex.of(sealed)).isEqualTo("588c979a61c663d2f066d0c2c0f989806d5f6b61dac384" + "17e8d12cfdf926e0");
    }

    // ==================================================== decrypt ====================================================

    /** AESCCMTests.swift:32-33: RFC 3610 packet vector 1 opens back to the plaintext. */
    @Test
    public void decrypt_rfc3610Vector1_recoversPlaintext() throws Exception {
        // :: Setup
        byte[] sealed = Hex.toBytes("588c979a61c663d2f066d0c2c0f989806d5f6b61dac38417e8d12cfdf926e0");

        // :: Act
        byte[] plain = AesCcm.decrypt(KEY, NONCE13, AAD, sealed, 8);

        // :: Verify
        assertThat(plain).isEqualTo(PLAIN);
    }

    /** AESCCMTests.swift:36-46: a flipped ciphertext bit fails authentication as a checked security exception. */
    @Test
    public void decrypt_tampered_throws() throws Exception {
        // :: Setup
        byte[] key = new byte[16];
        byte[] nonce = new byte[13];
        byte[] sealed = AesCcm.encrypt(key, nonce, "hello world".getBytes(StandardCharsets.US_ASCII), 8);
        sealed[0] ^= 1;

        // :: Act
        ThrowingRunnable open = () -> AesCcm.decrypt(key, nonce, sealed, 8);

        // :: Verify
        assertThrows(GeneralSecurityException.class, open);
    }

    /** The 4-byte tag and 7-byte nonce the handshake uses round-trip and add four bytes. */
    @Test
    public void decrypt_tag4Nonce7_roundTrips() throws Exception {
        // :: Setup
        byte[] nonce = Hex.toBytes("a1a2a3a4a5a6a7");
        byte[] sealed = AesCcm.encrypt(KEY, nonce, PLAIN, 4);

        // :: Act
        byte[] plain = AesCcm.decrypt(KEY, nonce, sealed, 4);

        // :: Verify
        assertThat(sealed).hasLength(PLAIN.length + 4);
        assertThat(plain).isEqualTo(PLAIN);
    }
}
