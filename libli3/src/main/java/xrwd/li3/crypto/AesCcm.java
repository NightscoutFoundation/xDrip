package xrwd.li3.crypto;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.security.GeneralSecurityException;

/**
 * AES-CCM (RFC 3610) on BouncyCastle's {@link CCMBlockCipher}. The Li3 handshake uses a 7-byte nonce and the
 * data plane a 13-byte one, both with a 4-byte tag and no associated data.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class AesCcm {

    private static final byte[] NO_AAD = new byte[0];

    private AesCcm() {
    }

    /** Returns {@code ciphertext || tag}. */
    public static byte[] encrypt(byte[] key, byte[] nonce, byte[] plain, int tagLen) throws GeneralSecurityException {
        return encrypt(key, nonce, NO_AAD, plain, tagLen);
    }

    public static byte[] encrypt(byte[] key, byte[] nonce, byte[] aad, byte[] plain, int tagLen)
            throws GeneralSecurityException {
        return run(true, key, nonce, aad, plain, tagLen);
    }

    /**
     * Opens {@code ciphertext || tag}.
     *
     * @throws GeneralSecurityException when the tag does not match
     */
    public static byte[] decrypt(byte[] key, byte[] nonce, byte[] sealed, int tagLen) throws GeneralSecurityException {
        return decrypt(key, nonce, NO_AAD, sealed, tagLen);
    }

    public static byte[] decrypt(byte[] key, byte[] nonce, byte[] aad, byte[] sealed, int tagLen)
            throws GeneralSecurityException {
        return run(false, key, nonce, aad, sealed, tagLen);
    }

    private static byte[] run(boolean encrypt, byte[] key, byte[] nonce, byte[] aad, byte[] in, int tagLen)
            throws GeneralSecurityException {
        CCMBlockCipher ccm = new CCMBlockCipher(new AESEngine());
        try {
            ccm.init(encrypt, new AEADParameters(new KeyParameter(key), tagLen * 8, nonce, aad));
            byte[] out = new byte[ccm.getOutputSize(in.length)];
            int n = ccm.processBytes(in, 0, in.length, out, 0);
            ccm.doFinal(out, n);
            return out;
        } catch (InvalidCipherTextException e) {
            throw new GeneralSecurityException("AES-CCM authentication failed", e);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new GeneralSecurityException("AES-CCM parameters rejected: " + e.getMessage(), e);
        }
    }
}
