package jamorham.keks;

import static org.bouncycastle.asn1.ASN1Sequence.getInstance;
import static org.bouncycastle.util.BigIntegers.asUnsignedByteArray;
import static jamorham.keks.util.Util.arrayAppend;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.jcajce.provider.asymmetric.ec.SignatureSpi;

import java.security.PrivateKey;
import java.security.SignatureException;

import jamorham.keks.util.Log;
import lombok.AllArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 */

@AllArgsConstructor
public class DSAChallenger extends SignatureSpi.ecDSA256.ecDSA256 {

    private static final int BYTES_PER_INTEGER = 32;
    private final PrivateKey key;

    public byte[] response(final byte[] challenge) {
        try {
            engineInitSign(key);
            engineUpdate(challenge);
            return sequenceToBytes(getInstance(new ASN1InputStream(engineSign())
                    .readObject()), 2);
        } catch (Exception e) {
           Log.l("Exception in challenger response: " + e);
        }
        return null;
    }

    public void engineUpdate(final byte[] bytes) throws SignatureException {
        digest.update(bytes, 0, bytes.length);
    }

    private byte[] sequenceToBytes(final ASN1Sequence sequence, final int count) {
        if (sequence.size() < count) return null;
        byte[] reply = new byte[0];
        for (int i = 0; i < count; i++) {
            val o = sequence.getObjectAt(i);
            if (o != null) {
                reply = arrayAppend(reply, asUnsignedByteArray(BYTES_PER_INTEGER,
                        ((ASN1Integer) o).getPositiveValue()));
            } else {
                return null;
            }
        }
        return reply;
    }

}
