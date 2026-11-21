package xrwd.li3.crypto;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import xrwd.li3.pairing.EphemeralExchange;

/**
 * kAuth, the Phase 5/6 key: NIST SP 800-56A concat KDF {@code SHA-256(00000001 || Ze || Zs)[0..16]}, where Ze is
 * phone ephemeral × sensor ephemeral and Zs is phone static × sensor static.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class SessionKeys {

    private SessionKeys() {
    }

    public static byte[] kAuth(AppIdentity identity, AsymmetricCipherKeyPair phoneEphemeral, byte[] sensorEphemeral65,
                               byte[] sensorStatic65) throws GeneralSecurityException {
        byte[] ze = EphemeralExchange.sharedSecret((ECPrivateKeyParameters) phoneEphemeral.getPrivate(),
                EphemeralExchange.parsePeer(sensorEphemeral65));
        byte[] zs = EphemeralExchange.sharedSecret(identity.privateKey(), EphemeralExchange.parsePeer(sensorStatic65));
        SHA256Digest sha = new SHA256Digest();
        sha.update(new byte[]{0, 0, 0, 1}, 0, 4);
        sha.update(ze, 0, ze.length);
        sha.update(zs, 0, zs.length);
        byte[] digest = new byte[32];
        sha.doFinal(digest, 0);
        Arrays.fill(ze, (byte) 0);
        Arrays.fill(zs, (byte) 0);
        return Arrays.copyOf(digest, 16);
    }
}
