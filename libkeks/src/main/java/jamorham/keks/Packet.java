package jamorham.keks;

import static org.bouncycastle.util.BigIntegers.asUnsignedByteArray;
import static org.bouncycastle.util.BigIntegers.fromUnsignedByteArray;
import static jamorham.keks.Curve.FIELD_SIZE;
import static jamorham.keks.Curve.PACKET_SIZE;
import static jamorham.keks.JECPoint.pointFromBytes;

import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import jamorham.keks.util.ByteArrayHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;

/**
 * JamOrHam
 *
 * Data packet serialization
 */

@AllArgsConstructor
public class Packet {

    @Getter
    BigInteger hash;
    @Getter
    ECPoint publicKeyPoint1;
    @Getter
    ECPoint publicKeyPoint2;

    public Packet(final ByteArrayHashMap bhm) {
        this(fromUnsignedByteArray(bhm.mget(HBYTES1_ID)),
                pointFromBytes(bhm.mget(POINT1X_ID), bhm.mget(POINT1Y_ID)),
                pointFromBytes(bhm.mget(POINT2X_ID), bhm.mget(POINT2Y_ID)));
    }

    public static final int POINT1X_ID = 28082;
    public static final int POINT1Y_ID = 37603;
    public static final int POINT2X_ID = 54247;
    public static final int POINT2Y_ID = 40255;
    public static final int HBYTES1_ID = 65535;

    private static final int[] ID_LIST = {POINT1X_ID, POINT1Y_ID, POINT2X_ID, POINT2Y_ID, HBYTES1_ID};

    public static Packet parse(final byte[] packet) {
        if (packet.length < PACKET_SIZE) return null;
        val bhm = new ByteArrayHashMap();
        val buf = ByteBuffer.wrap(packet);
        for (val id : ID_LIST) {
            buf.get(bhm.mget(id));
        }
        return new Packet(bhm);
    }

    public byte[] output() {
        val packet = ByteBuffer.allocate(PACKET_SIZE);
        packet.put(new JECPoint(getPublicKeyPoint1()).toBytes());
        packet.put(new JECPoint(getPublicKeyPoint2()).toBytes());
        packet.put(asUnsignedByteArray(FIELD_SIZE, getHash()));
        val array = packet.array();
        if (array.length != PACKET_SIZE) {
            throw new RuntimeException("Invalid size");
        }
        return array;
    }

}
