package jamorham.keks;


import static jamorham.keks.Curve.curveSpec;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi;
import org.bouncycastle.math.ec.ECPoint;

import java.io.IOException;
import java.math.BigInteger;
import java.security.PrivateKey;

import jamorham.keks.util.Log;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 * <p>
 * KeyPair data holder
 */

@RequiredArgsConstructor
public class KeyPair {

    @Getter
    public final BigInteger privateKey;
    @Getter
    public final ECPoint publicKey;

    public KeyPair(final byte[] publicBytes, final byte[] privateBytes) throws IOException {
        val keyFactorySpi = new KeyFactorySpi.EC();
        this.publicKey = ((BCECPublicKey) keyFactorySpi.generatePublic(SubjectPublicKeyInfo.getInstance(publicBytes))).getQ();
        this.privateKey = new BigInteger(((BCECPrivateKey) keyFactorySpi.generatePrivate(PrivateKeyInfo.getInstance(privateBytes))).getD().toString());
    }

    public KeyPair() {
        try {
            val keyPairGenerator = new KeyPairGeneratorSpi.EC();
            keyPairGenerator.initialize(curveSpec);
            val keyPair = keyPairGenerator.genKeyPair();
            publicKey = ((BCECPublicKey)keyPair.getPublic()).getQ();
            privateKey = ((BCECPrivateKey)keyPair.getPrivate()).getD();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public KeyPair(byte[][] bytes) throws IOException {
        this(bytes[0], bytes[1]);
    }

    public static KeyPair fromBytes(final byte[][] bytes) {
        try {
            return new KeyPair(bytes[0], bytes[1]);
        } catch (Exception e) {
            Log.l("Failure to generate KeyPair from bytes array");
        }
        return null;
    }

    public static PrivateKey fromBytes(byte[] privateBytes) {
        try {
            val ecKeyFac = new KeyFactorySpi.EC();
            return ecKeyFac.generatePrivate(PrivateKeyInfo.getInstance(privateBytes));
        } catch (Exception e) {
            Log.l("Failure to generate Key from bytes array");
        }
        return null;
    }

}
