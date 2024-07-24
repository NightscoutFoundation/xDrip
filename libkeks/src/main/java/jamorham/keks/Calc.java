package jamorham.keks;


import static org.bouncycastle.util.BigIntegers.fromUnsignedByteArray;
import static java.lang.System.arraycopy;
import static jamorham.keks.Config.Get.REFERENCE;
import static jamorham.keks.util.Util.arrayReduce;
import static jamorham.keks.util.Util.intToByteArray;

import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import jamorham.keks.util.AESwrapper;
import jamorham.libkeks.Digest;
import jamorham.libkeks.SHA256;
import lombok.RequiredArgsConstructor;
import lombok.val;


/**
 * JamOrHam
 * <p>
 * For reference see J-PAKE as described in the following papers:
 * <p>
 * https://ia.cr/2010/190
 * http://grouper.ieee.org/groups/1363/Research/contributions/hao-ryan-2008.pdf
 * https://datatracker.ietf.org/doc/html/draft-cragie-tls-ecjpake-01
 */

public class Calc {

    private static final BigInteger exponent = fromUnsignedByteArray(REFERENCE.bytes);

    public static Packet getRound12Packet(final Context context, boolean part2) {
        val key = part2 ? context.KeyB : context.keyA;
        val zkp = new ZKP(Curve.G, key, context.alice);
        return new Packet(zkp.getProof(), key.getPublicKey(), zkp.getGv());
    }

    public static Packet getRound1Packet(final Context context) {
        return getRound12Packet(context, false);
    }

    public static Packet getRound2Packet(final Context context) {
        return getRound12Packet(context, true);
    }

    public static boolean validateRound1Packet(final Packet p, final byte[] party) {
        if (p == null) return false;
        return validateZeroKnowledgeProof(Curve.G, p.publicKeyPoint1, p.publicKeyPoint2, p.hash, party);
    }

    public static boolean validateRound1Packet(final Context context) {
        return validateRound1Packet(context.getRound1Packet(), context.bob);
    }

    public static boolean validateRound2Packet(final Context context) {
        return validateRound1Packet(context.getRound2Packet(), context.bob);
    }

    public static Packet getRound3Packet(final Context context) {
        val packet1 = context.getRound1Packet();
        val packet2 = context.getRound2Packet();
        val x1 = context.keyA.getPublicKey();
        val x2 = context.KeyB.getPrivateKey();
        val x3 = packet1.getPublicKeyPoint1();
        val x4 = packet2.getPublicKeyPoint1();
        val s = context.getPasswordBigInteger();
        val x2s = x2.multiply(s).mod(Curve.Q);
        val x134 = x1.add(x3).add(x4).normalize();
        val A = x134.multiply(x2s).normalize();
        val zkp = new ZKP(x134, new KeyPair(x2s, A), context.alice);
        return new Packet(zkp.getProof(), A, zkp.getGv());
    }

    public static boolean validateRound3Packet(final Context context) {
        val packet = context.getRound3Packet();
        if (packet == null) return false;
        val x1 = context.keyA.getPublicKey();
        val x2 = context.KeyB.getPublicKey();
        val x3 = context.getRound1Packet().getPublicKeyPoint1();
        val g = x1.add(x2).add(x3).normalize();
        val public1 = packet.getPublicKeyPoint1();
        return validateZeroKnowledgeProof(g, public1, packet.getPublicKeyPoint2(), packet.getHash(), context.bob);
    }

    public static byte[] getSharedKey(final Context context) {
        if (context.getRound3Packet() == null) return null;
        val point1 = context.getRound3Packet().getPublicKeyPoint1();
        val x2 = context.KeyB.getPrivateKey();
        val x4 = context.getRound2Packet().getPublicKeyPoint1();
        val s = context.getPasswordBigInteger();
        val key = point1.subtract(x4.multiply(
                x2.multiply(s).mod(Curve.Q))).multiply(x2).normalize();
        return SHA256.hash(key.getXCoord().getEncoded());
    }

    public static byte[] getShortSharedKey(final Context context) {
        return arrayReduce(getSharedKey(context), 16);
    }

    public static byte[] calculateHash(final Context context) {
        val data = context.challenge;
        val key = context.savedKey != null ? context.savedKey : getShortSharedKey(context);
        if (key == null) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.put(data);
        bb.put(data);
        val doubleData = bb.array();
        val aesBytes = new AESwrapper(key).aes(doubleData);
        bb = ByteBuffer.allocate(8);
        bb.put(aesBytes, 0, 8);
        return bb.array();
    }

    @RequiredArgsConstructor
    public static class ZKP {
        private final BigInteger exponent = Curve.getExponent();
        private final ECPoint g;
        private final KeyPair keyPair;
        private final byte[] party;
        private ECPoint gv = null;

        private ECPoint getGv() {
            if (gv == null) {
                gv = g.multiply(exponent).normalize();
            }
            return gv;
        }

        public BigInteger getProof() {
            return exponent.subtract(getZeroKnowledgeHash(g, getGv(), keyPair.getPublicKey(), party)
                    .multiply(keyPair.getPrivateKey()))
                    .mod(Curve.Q);
        }
    }

    public static boolean validateZeroKnowledgeProof(final ECPoint g, final ECPoint publicKey, final ECPoint gv, BigInteger b, byte[] party) {
        val hash = getZeroKnowledgeHash(g, gv, publicKey, party);
        return g.multiply(b)
                .add(publicKey.multiply(hash))
                .normalize()
                .equals(gv);
    }

    public static BigInteger getZeroKnowledgeHash(final ECPoint g, final ECPoint gv, final ECPoint gx, byte[] party) {
        val digestBytes = new byte[32];
        val digest = new Digest(digestBytes);
        updateDigestIncludingSize(digest, g);
        updateDigestIncludingSize(digest, gv);
        updateDigestIncludingSize(digest, gx);
        updateDigestIncludingSize(digest, party);
        digest.doFinal();
        return fromUnsignedByteArray(digestBytes)
                .mod(Curve.Q);
    }

    private static void updateDigestIncludingSize(final Digest digest, final ECPoint point) {
        updateDigestIncludingSize(digest, point.getEncoded(false));
    }

    private static void updateDigestIncludingSize(final Digest digest, final byte[] byteArray) {
        digest.update(intToByteArray(byteArray.length));
        digest.update(byteArray);
    }

    public static byte[] challenger(final byte[] bytes, final byte[] challenge) {
        val pchallenge = new byte[16];
        arraycopy(challenge, 2, pchallenge, 0, pchallenge.length);
        return new DSAChallenger(KeyPair.fromBytes(bytes))
                .response(pchallenge);
    }

}
