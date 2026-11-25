package jamorham.keks;

import org.bouncycastle.math.ec.ECPoint;
import org.junit.Test;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import static com.google.common.truth.Truth.assertThat;

/**
 * The elliptic-curve work libkeks hands to Bouncy Castle during sensor authentication: key import,
 * point encoding, the packet wire format, the J-PAKE exchange and the ECDSA challenge response. Known-answer tests pin
 * the bytes to values recorded on Bouncy Castle 1.68, so a library upgrade cannot change them silently.
 * <p>
 * Wherever the JDK can act as an independent oracle it does — standard secp256r1 constants, JDK-generated
 * keys, JDK signature verification — so these pass or fail on what the protocol needs, not on what one
 * Bouncy Castle version happens to return.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class KeksCryptoTest {

    private static final BigInteger SECP256R1_ORDER = new BigInteger(
            "FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16);
    private static final BigInteger SECP256R1_GX = new BigInteger(
            "6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296", 16);
    private static final BigInteger SECP256R1_GY = new BigInteger(
            "4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5", 16);

    private static final byte[] CLIENT_PARTY = "client-party".getBytes();
    private static final byte[] SENSOR_PARTY = "sensor-party".getBytes();

    // Recorded once on Bouncy Castle 1.68 (the version before 1.86) with the fixed scalars below. The wire bytes are
    // what a sensor running the 1.68 maths would send; they must keep validating and yield the same key and hash.
    private static final String CLIENT_KEY_A = "1111111111111111111111111111111111111111111111111111111111111111";
    private static final String CLIENT_KEY_B = "2222222222222222222222222222222222222222222222222222222222222222";
    private static final String RECORDED_SENSOR_ROUND1 =
            "51a7580833898ea1b183cbd7350a4099078c6ef1c1e18e970cd7683035f25e7d0110522712b0b5a7cff081685486984a" +
            "94e6831edac46e7360fa9d834a7a81a1edeab6fc45c97110bc9f7cef0becace85bfc768d33d409ff64c4c7e0c922e1ff" +
            "f22661f15220c6294475d1060c6c573297143bbe23e4466fbc7ccb764d6d867e0bded280eb4c38df57795f236029eaa4" +
            "4c65306dca427ada187c4c631e31a3ef";
    private static final String RECORDED_SENSOR_ROUND2 =
            "5b36890dacbd7c9a96bb74a1ee28b3d2d75b72e09a20ef25cf8e6fd8a9f0350d0e14bed8d4682a34d83538bdff5b96e8" +
            "9a6666ec0db5745d02fa1210072df75a4381f3f4115793ae6704c02a35a13f47b36269c9609c90d7efe6aceb6c89fcc0" +
            "0fa195ae7040ad6a6c3d58d07fd10a4d706e3231ce5b5d9876d1855a8d62297f73122abbb136ee834f784ef64745b2f9" +
            "c53bbf18362450f668bf0940e492e6c8";
    private static final String RECORDED_SENSOR_ROUND3 =
            "e2d02560c3a02f7bbcf5fafd0d7c702d9038bbb6b59ae7d3df3c2ed7c3ce9f1ae2ba70cf25ce4236ca2fccf771354b0f" +
            "49ea2b5f8717145319f9c5588fc10482964d8b4dc86e7d4f066e87eb7ed898c400c0a3dc9ce50024764fc1364a4767fe" +
            "966e0a75e8a8b8c59e59b032469ed58c82bf164915bf2928ef575054703c9a5c7248adfce990688c7b989191413733f6" +
            "8fc35957ba44994c07b441a70aa18d86";
    private static final String RECORDED_CLIENT_ROUND3_POINTS =
            "869c88768b238d9a0032b4b7fa3f27720ead967bbb7c63c4d9106d25e3c791e2592843004e57e21abee19a77d43c88cb" +
            "1769bd5ab7c51ab6a5eb6f35f6222eca";
    private static final String RECORDED_SHORT_KEY = "33e4b5478bf3727d3a676fd6adb75f44";
    private static final String RECORDED_CHALLENGE_HASH = "76fdaee906c3110d";
    private static final String RECORDED_ZK_HASH = "2086b293f8edf0eedd87ac1e22d1413c343619eb1614aaedce04848f052ef2c7";
    private static final String RECORDED_PUBLIC_X509 =
            "3059301306072a8648ce3d020106082a8648ce3d030107034200040c78104f8400fe5024e7364768e9ead86ca6dc4e48" +
            "e163a5b47046a3cbc299686f8335c0acbfd7e2be918aca5662b0a7625ad486d5492481dc5c885367e01303";
    private static final String RECORDED_PRIVATE_PKCS8 =
            "3041020100301306072a8648ce3d020106082a8648ce3d030107042730250201010420fec3bce23848264564fe745a27" +
            "6b3cba6033b3b483e41557a3ad75e845f6e1db";
    private static final String RECORDED_PRIVATE_D = "fec3bce23848264564fe745a276b3cba6033b3b483e41557a3ad75e845f6e1db";
    private static final String RECORDED_PUBLIC_X = "c78104f8400fe5024e7364768e9ead86ca6dc4e48e163a5b47046a3cbc29968";
    private static final String RECORDED_PUBLIC_Y = "6f8335c0acbfd7e2be918aca5662b0a7625ad486d5492481dc5c885367e01303";

    private static java.security.KeyPair jdkKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static Context party(byte[] self, byte[] peer, String password) {
        Context context = new Context();
        context.keyA = new KeyPair();
        context.KeyB = new KeyPair();
        context.alice = self;
        context.bob = peer;
        context.password = password;
        return context;
    }

    private static byte[] unhex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    private static KeyPair fixedKey(String scalarHex) {
        BigInteger scalar = new BigInteger(scalarHex, 16);
        return new KeyPair(scalar, Curve.G.multiply(scalar).normalize());
    }

    /** The recorded packet with the last byte of its proof changed. */
    private static Packet tampered(String recordedHex) {
        byte[] wire = unhex(recordedHex);
        wire[wire.length - 1] ^= 0x01;
        return Packet.parse(wire);
    }

    /** The client side with fixed keys, holding the sensor's recorded round 1 and 2 packets. */
    private static Context recordedClient() {
        Context client = new Context();
        client.keyA = fixedKey(CLIENT_KEY_A);
        client.KeyB = fixedKey(CLIENT_KEY_B);
        client.alice = CLIENT_PARTY;
        client.bob = SENSOR_PARTY;
        client.password = "123456";
        client.packet[1] = Packet.parse(unhex(RECORDED_SENSOR_ROUND1));
        client.packet[2] = Packet.parse(unhex(RECORDED_SENSOR_ROUND2));
        return client;
    }

    /** Runs rounds 1 and 2 over the wire format, so each side holds the other's parsed packets. */
    private static void exchangeRounds12(Context client, Context sensor) {
        client.packet[1] = Packet.parse(Calc.getRound1Packet(sensor).output());
        sensor.packet[1] = Packet.parse(Calc.getRound1Packet(client).output());
        client.packet[2] = Packet.parse(Calc.getRound2Packet(sensor).output());
        sensor.packet[2] = Packet.parse(Calc.getRound2Packet(client).output());
    }

    private static void exchangeRound3(Context client, Context sensor) {
        Packet fromClient = Calc.getRound3Packet(client);
        Packet fromSensor = Calc.getRound3Packet(sensor);
        client.packet[3] = Packet.parse(fromSensor.output());
        sensor.packet[3] = Packet.parse(fromClient.output());
    }

    /** The unsigned value in 32-byte field {@code index} of a packet's wire form. */
    private static BigInteger slot(byte[] wire, int index) {
        return new BigInteger(1, Arrays.copyOfRange(wire, index * 32, (index + 1) * 32));
    }

    private static byte[] toDer(byte[] rawSignature) throws Exception {
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(rawSignature, 0, 32));
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(rawSignature, 32, 64));
        return new org.bouncycastle.asn1.DERSequence(new org.bouncycastle.asn1.ASN1Encodable[]{
                new org.bouncycastle.asn1.ASN1Integer(r), new org.bouncycastle.asn1.ASN1Integer(s)}).getEncoded();
    }

    // ===== Curve and encoding ========================================================================================

    /** The curve parameters every packet is built on are the standard secp256r1 ones. */
    @Test
    public void curve_isStandardSecp256r1() {
        // :: Act
        ECPoint generator = Curve.G.normalize();

        // :: Verify
        assertThat(Curve.Q).isEqualTo(SECP256R1_ORDER);
        assertThat(generator.getAffineXCoord().toBigInteger()).isEqualTo(SECP256R1_GX);
        assertThat(generator.getAffineYCoord().toBigInteger()).isEqualTo(SECP256R1_GY);
        assertThat(Curve.FIELD_SIZE).isEqualTo(32);
        assertThat(Curve.PACKET_SIZE).isEqualTo(160);
    }

    /** The zero-knowledge hash feeds uncompressed point encodings into the digest: 0x04 || X || Y. */
    @Test
    public void pointEncoding_isUncompressedXy() {
        // :: Act
        byte[] encoded = Curve.G.getEncoded(false);

        // :: Verify
        assertThat(encoded.length).isEqualTo(65);
        assertThat(encoded[0]).isEqualTo((byte) 0x04);
        assertThat(new BigInteger(1, Arrays.copyOfRange(encoded, 1, 33))).isEqualTo(SECP256R1_GX);
        assertThat(new BigInteger(1, Arrays.copyOfRange(encoded, 33, 65))).isEqualTo(SECP256R1_GY);
    }

    // ===== Key import ================================================================================================

    /** Stored keys are X.509 / PKCS#8 encodings; the imported values must be the same key the JDK sees. */
    @Test
    public void keyPair_importsEncodedJdkKeys() throws Exception {
        // :: Setup
        java.security.KeyPair jdk = jdkKeyPair();
        ECPublicKey jdkPublic = (ECPublicKey) jdk.getPublic();
        ECPrivateKey jdkPrivate = (ECPrivateKey) jdk.getPrivate();

        // :: Act
        KeyPair imported = KeyPair.fromBytes(new byte[][]{jdkPublic.getEncoded(), jdkPrivate.getEncoded()});

        // :: Verify
        assertThat(imported).isNotNull();
        assertThat(imported.getPrivateKey()).isEqualTo(jdkPrivate.getS());
        ECPoint publicPoint = imported.getPublicKey().normalize();
        assertThat(publicPoint.getAffineXCoord().toBigInteger()).isEqualTo(jdkPublic.getW().getAffineX());
        assertThat(publicPoint.getAffineYCoord().toBigInteger()).isEqualTo(jdkPublic.getW().getAffineY());
    }

    // ===== Packet wire format ========================================================================================

    /** A packet is X1 | Y1 | X2 | Y2 | hash, 32 bytes each, and parses back to the same values. */
    @Test
    public void packet_outputLayoutAndParseRoundTrip() {
        // :: Setup
        ECPoint point1 = new KeyPair().getPublicKey().normalize();
        ECPoint point2 = new KeyPair().getPublicKey().normalize();
        BigInteger hash = new BigInteger("0123456789abcdef", 16);
        Packet packet = new Packet(hash, point1, point2);

        // :: Act
        byte[] wire = packet.output();
        Packet parsed = Packet.parse(wire);

        // :: Verify
        assertThat(wire.length).isEqualTo(160);
        assertThat(slot(wire, 0)).isEqualTo(point1.getAffineXCoord().toBigInteger());
        assertThat(slot(wire, 1)).isEqualTo(point1.getAffineYCoord().toBigInteger());
        assertThat(slot(wire, 2)).isEqualTo(point2.getAffineXCoord().toBigInteger());
        assertThat(slot(wire, 3)).isEqualTo(point2.getAffineYCoord().toBigInteger());
        assertThat(slot(wire, 4)).isEqualTo(hash);
        assertThat(parsed.getHash()).isEqualTo(hash);
        assertThat(parsed.getPublicKeyPoint1()).isEqualTo(point1);
        assertThat(parsed.getPublicKeyPoint2()).isEqualTo(point2);
    }

    /** A packet shorter than the fixed size is rejected rather than read past its end. */
    @Test
    public void packet_tooShort_parsesToNull() {
        // :: Act
        Packet parsed = Packet.parse(new byte[159]);

        // :: Verify
        assertThat(parsed).isNull();
    }

    // ===== J-PAKE exchange ===========================================================================================

    /** Both sides with the same pairing code validate every round and derive the same session key. */
    @Test
    public void jpake_samePassword_bothSidesValidateAndAgreeOnKey() {
        // :: Setup
        Context client = party(CLIENT_PARTY, SENSOR_PARTY, "123456");
        Context sensor = party(SENSOR_PARTY, CLIENT_PARTY, "123456");

        // :: Act
        exchangeRounds12(client, sensor);
        exchangeRound3(client, sensor);
        byte[] clientKey = Calc.getShortSharedKey(client);
        byte[] sensorKey = Calc.getShortSharedKey(sensor);

        // :: Verify
        assertThat(Calc.validateRound1Packet(client)).isTrue();
        assertThat(Calc.validateRound2Packet(client)).isTrue();
        assertThat(Calc.validateRound1Packet(sensor)).isTrue();
        assertThat(Calc.validateRound2Packet(sensor)).isTrue();
        assertThat(Calc.validateRound3Packet(client)).isTrue();
        assertThat(Calc.validateRound3Packet(sensor)).isTrue();
        assertThat(clientKey).hasLength(16);
        assertThat(clientKey).isEqualTo(sensorKey);
    }

    /** A wrong pairing code must not produce a shared key. */
    @Test
    public void jpake_differentPassword_keysDiffer() {
        // :: Setup
        Context client = party(CLIENT_PARTY, SENSOR_PARTY, "123456");
        Context sensor = party(SENSOR_PARTY, CLIENT_PARTY, "654321");

        // :: Act
        exchangeRounds12(client, sensor);
        exchangeRound3(client, sensor);
        byte[] clientKey = Calc.getShortSharedKey(client);
        byte[] sensorKey = Calc.getShortSharedKey(sensor);

        // :: Verify
        assertThat(clientKey).isNotEqualTo(sensorKey);
    }

    /** A round-1 packet whose proof was made for another party identity fails validation. */
    @Test
    public void jpake_proofForWrongParty_isRejected() {
        // :: Setup
        Context client = party(CLIENT_PARTY, SENSOR_PARTY, "123456");
        Context impostor = party("someone-else".getBytes(), CLIENT_PARTY, "123456");
        client.packet[1] = Packet.parse(Calc.getRound1Packet(impostor).output());

        // :: Act
        boolean valid = Calc.validateRound1Packet(client);

        // :: Verify
        assertThat(valid).isFalse();
    }

    // ===== ECDSA challenge ===========================================================================================

    /** The challenge response is a raw r || s P-256 signature over challenge bytes 2..17, verifiable by the JDK. */
    @Test
    public void challenger_signsChallengeWithStoredKey() throws Exception {
        // :: Setup
        java.security.KeyPair jdk = jdkKeyPair();
        byte[] storedPrivateKey = jdk.getPrivate().getEncoded();
        byte[] challenge = new byte[20];
        for (int i = 0; i < challenge.length; i++) {
            challenge[i] = (byte) (i * 7 + 3);
        }

        // :: Act
        byte[] response = Calc.challenger(storedPrivateKey, challenge);

        // :: Verify
        assertThat(response).hasLength(64);
        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(jdk.getPublic());
        verifier.update(Arrays.copyOfRange(challenge, 2, 18));
        assertThat(verifier.verify(toDer(response))).isTrue();
    }

    // ===== Known answers recorded on Bouncy Castle 1.68 ==============================================================

    /** Proofs a peer computed with the earlier Bouncy Castle maths still validate in all three rounds. */
    @Test
    public void knownAnswer_recordedSensorPackets_validate() {
        // :: Setup
        Context client = recordedClient();
        client.packet[3] = Packet.parse(unhex(RECORDED_SENSOR_ROUND3));

        // :: Act
        boolean round1 = Calc.validateRound1Packet(client);
        boolean round2 = Calc.validateRound2Packet(client);
        boolean round3 = Calc.validateRound3Packet(client);

        // :: Verify
        assertThat(round1).isTrue();
        assertThat(round2).isTrue();
        assertThat(round3).isTrue();
    }

    /** A round-2 packet whose proof was altered fails validation while the untouched round 1 still passes. */
    @Test
    public void knownAnswer_tamperedRound2Packet_isRejected() {
        // :: Setup
        Context client = recordedClient();
        client.packet[2] = tampered(RECORDED_SENSOR_ROUND2);

        // :: Act
        boolean round1 = Calc.validateRound1Packet(client);
        boolean round2 = Calc.validateRound2Packet(client);

        // :: Verify
        assertThat(round1).isTrue();
        assertThat(round2).isFalse();
    }

    /** A round-3 packet whose proof was altered fails validation. */
    @Test
    public void knownAnswer_tamperedRound3Packet_isRejected() {
        // :: Setup
        Context client = recordedClient();
        client.packet[3] = tampered(RECORDED_SENSOR_ROUND3);

        // :: Act
        boolean round3 = Calc.validateRound3Packet(client);

        // :: Verify
        assertThat(round3).isFalse();
    }

    /** The point the client sends in round 3 is deterministic from the keys, so it must match the recorded bytes. */
    @Test
    public void knownAnswer_clientRound3Points_matchRecorded() {
        // :: Setup
        Context client = recordedClient();

        // :: Act
        byte[] wire = Calc.getRound3Packet(client).output();

        // :: Verify
        assertThat(Arrays.copyOf(wire, 64)).isEqualTo(unhex(RECORDED_CLIENT_ROUND3_POINTS));
    }

    /** The session key and the challenge hash sent back to the sensor are byte-for-byte the recorded ones. */
    @Test
    public void knownAnswer_sharedKeyAndChallengeHash_matchRecorded() {
        // :: Setup
        Context client = recordedClient();
        client.packet[3] = Packet.parse(unhex(RECORDED_SENSOR_ROUND3));
        client.challenge = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};

        // :: Act
        byte[] shortKey = Calc.getShortSharedKey(client);
        byte[] challengeHash = Calc.calculateHash(client);

        // :: Verify
        assertThat(shortKey).isEqualTo(unhex(RECORDED_SHORT_KEY));
        assertThat(challengeHash).isEqualTo(unhex(RECORDED_CHALLENGE_HASH));
    }

    /** The zero-knowledge hash over fixed points equals the recorded value. */
    @Test
    public void knownAnswer_zeroKnowledgeHash_matchesRecorded() {
        // :: Act
        BigInteger hash = Calc.getZeroKnowledgeHash(Curve.G, fixedKey("05").getPublicKey(),
                fixedKey("07").getPublicKey(), CLIENT_PARTY);

        // :: Verify
        assertThat(hash).isEqualTo(new BigInteger(RECORDED_ZK_HASH, 16));
    }

    /** A key pair stored by the earlier version imports to the same private scalar and public point. */
    @Test
    public void knownAnswer_recordedKeyEncoding_imports() {
        // :: Act
        KeyPair imported = KeyPair.fromBytes(new byte[][]{unhex(RECORDED_PUBLIC_X509), unhex(RECORDED_PRIVATE_PKCS8)});

        // :: Verify
        assertThat(imported).isNotNull();
        assertThat(imported.getPrivateKey()).isEqualTo(new BigInteger(RECORDED_PRIVATE_D, 16));
        ECPoint publicPoint = imported.getPublicKey().normalize();
        assertThat(publicPoint.getAffineXCoord().toBigInteger()).isEqualTo(new BigInteger(RECORDED_PUBLIC_X, 16));
        assertThat(publicPoint.getAffineYCoord().toBigInteger()).isEqualTo(new BigInteger(RECORDED_PUBLIC_Y, 16));
    }
}
