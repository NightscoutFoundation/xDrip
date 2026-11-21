package xrwd.li3.crypto;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import xrwd.li3.pairing.EphemeralExchange;

/**
 * An app credential: the 162-byte phone certificate sent in Phase 1 and the P-256 scalar behind its public key.
 * Both are secrets: nothing here puts them in a message or {@code toString}.
 * Ported from RoundWhiteDiscKit {@code Pairing/AppIdentity.swift} and {@code PhoneCert.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class AppIdentity {

    public static final int CERT_SIZE = 162;
    private static final int PUBKEY_OFFSET = 33;

    private final String label;
    private final AsymmetricCipherKeyPair keyPair;
    private final byte[] certificate;

    private AppIdentity(String label, AsymmetricCipherKeyPair keyPair, byte[] certificate) {
        this.label = label;
        this.keyPair = keyPair;
        this.certificate = certificate;
    }

    /**
     * @throws GeneralSecurityException when the certificate is not 162 bytes, holds no uncompressed point, or the
     *                                  scalar is not the private key behind that point
     */
    public static AppIdentity of(String label, byte[] privateKey32, byte[] cert162) throws GeneralSecurityException {
        if (cert162.length != CERT_SIZE) {
            throw new GeneralSecurityException("Identity '" + label + "': certificate is " + cert162.length
                    + " bytes, not " + CERT_SIZE);
        }
        if (cert162[PUBKEY_OFFSET] != 0x04) {
            throw new GeneralSecurityException("Identity '" + label + "': certificate key is not an uncompressed point");
        }
        AsymmetricCipherKeyPair keyPair = EphemeralExchange.fromScalar(privateKey32);
        byte[] certKey = Arrays.copyOfRange(cert162, PUBKEY_OFFSET, PUBKEY_OFFSET + 65);
        if (!Arrays.equals(EphemeralExchange.publicKey65(keyPair), certKey)) {
            throw new GeneralSecurityException("Identity '" + label + "': private key does not match certificate");
        }
        return new AppIdentity(label, keyPair, cert162.clone());
    }

    public String label() {
        return label;
    }

    /** The 162 bytes written in Phase 1. */
    public byte[] certificate() {
        return certificate.clone();
    }

    ECPrivateKeyParameters privateKey() {
        return (ECPrivateKeyParameters) keyPair.getPrivate();
    }

    @Override
    public String toString() {
        return "AppIdentity(" + label + ")";
    }
}
