package jamorham.keks;


import static java.math.BigInteger.ONE;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * JamOrHam
 *
 * KEKS Elliptic Curve
 */

public class Curve {

    private static final SecureRandom random = new SecureRandom();
    public static final String name = "secp256r1";
    public static final ECNamedCurveParameterSpec curveSpec = ECNamedCurveTable.getParameterSpec(name);
    public static final ECPoint G = curveSpec.getG();
    public static final ECCurve curve = curveSpec.getCurve();
    public static final BigInteger Q = curve.getOrder();
    public static final BigInteger QM1 = Q.subtract(ONE);
    public static final int CURVE_BITS = curve.getFieldSize();
    public static final int FIELD_SIZE = (CURVE_BITS + 7) / 8;
    public static final int PACKET_SIZE = FIELD_SIZE * 5;

    public static BigInteger getExponent() {
        return BigIntegers.createRandomInRange(ONE, QM1, random);
    }

}
