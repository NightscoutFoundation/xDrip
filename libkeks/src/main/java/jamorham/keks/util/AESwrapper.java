package jamorham.keks.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 */

@RequiredArgsConstructor
public class AESwrapper {

    private final byte[] key;

    public byte[] aes(final byte[] plaintext) {
        try {
            val aesCipher = Cipher.getInstance("AES/ECB/NoPadding");
            val secretKeySpec = new SecretKeySpec(key, "AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return aesCipher.doFinal(plaintext, 0, plaintext.length);
        } catch (Exception e) {
            return null;
        }
    }

}
