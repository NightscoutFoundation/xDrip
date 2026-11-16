package xrwd.li3.pairing;

import static com.google.common.truth.Truth.assertThat;
import static xrwd.li3.pairing.FakeSensor.IDENTITY_CERT;
import static xrwd.li3.pairing.FakeSensor.IV_ENC;
import static xrwd.li3.pairing.FakeSensor.K_ENC;
import static org.junit.Assert.assertThrows;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import xrwd.li3.ble.Gatt;
import xrwd.li3.util.Hex;

/**
 * Tests for the pairing handshake against a {@link FakeSensor} that plays the sensor side with its own synthetic keys
 * and derives kAuth from its end of the two ECDH exchanges. Ported from RoundWhiteDiscKit 7dd6396
 * {@code PublicSmokeTests.swift} and the command order in {@code PairingFlow.swift}.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class PairingFlowTest {

    private static final byte[] BLE_PIN = Hex.toBytes("3225ec72");

    /** The full first-pair command order RoundWhiteDiscKit's PairingFlow writes. */
    private static final List<Integer> FULL_ORDER = Arrays.asList(
            0x01, 0x02, 0x03, 0x09, 0x0d, 0x0e, 0x11, 0x08);

    // ================================================== First pair ===================================================

    /** A first pair writes the full command order and ends with the session keys the sensor issued. */
    @Test
    public void firstPair_runsFullSequence_andReturnsSensorKeys() throws Exception {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        PairingResult result = flow.firstPair(BLE_PIN);

        // :: Verify
        assertThat(sensor.commands).isEqualTo(FULL_ORDER);
        assertThat(result.kAuth()).isEqualTo(sensor.kAuth);
        assertThat(result.kEnc()).isEqualTo(K_ENC);
        assertThat(result.ivEnc()).isEqualTo(IV_ENC);
        assertThat(result.usedCache()).isFalse();
        assertThat(sensor.tailSeen).isEqualTo(BLE_PIN);
    }

    /** The phone certificate and padded ephemeral key go out as offset-prefixed 18-byte fragments. */
    @Test
    public void firstPair_writesCertificateAndEphemeralFragmented() throws Exception {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        flow.firstPair(BLE_PIN);

        // :: Verify
        assertThat(sensor.writeCount(Gatt.SEC_CERT_DATA)).isEqualTo(9 + 4);
        assertThat(sensor.writeCount(Gatt.SEC_CHALLENGE_DATA)).isEqualTo(3);
        assertThat(sensor.phoneCert).isEqualTo(Hex.toBytes(IDENTITY_CERT));
    }

    /** The result's text names the stage reached and none of the keys. */
    @Test
    public void firstPair_toString_hidesKeys() throws Exception {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        PairingResult result = flow.firstPair(BLE_PIN);

        // :: Verify
        String text = result.toString();
        assertThat(text).contains(result.lastStage());
        assertThat(text).doesNotContain(Hex.of(result.kAuth()));
        assertThat(text).doesNotContain(Hex.of(K_ENC));
        assertThat(text).doesNotContain(Hex.of(IV_ENC));
    }

    /** A first pair hands back the sensor's static key, so the collector can hold later pairings to it. */
    @Test
    public void firstPair_returnsSensorStaticKey() throws Exception {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        PairingResult result = flow.firstPair(BLE_PIN);

        // :: Verify
        assertThat(result.sensorKey65()).isEqualTo(sensor.staticKey65());
    }

    /** A sensor showing the key it showed before pairs as usual. */
    @Test
    public void firstPair_sameKeyAsBefore_pairs() throws Exception {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        PairingResult result = flow.firstPair(BLE_PIN, sensor.staticKey65());

        // :: Verify
        assertThat(result.kEnc()).isEqualTo(K_ENC);
        assertThat(sensor.tailSeen).isEqualTo(BLE_PIN);
    }

    /**
     * A device showing another key than the sensor paired before is refused before the challenge, so it never gets
     * the PIN.
     */
    @Test
    public void firstPair_otherKeyThanBefore_throwsBeforePinIsSent() {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");
        byte[] before = sensor.staticKey65();
        before[64] ^= 1;

        // :: Act
        ThrowingRunnable pair = () -> flow.firstPair(BLE_PIN, before);

        // :: Verify
        GeneralSecurityException e = assertThrows(GeneralSecurityException.class, pair);
        assertThat(e).hasMessageThat().contains("key");
        assertThat(sensor.commands).doesNotContain(0x11);
        assertThat(sensor.tailSeen).isNull();
    }

    /** A reconnect exchanges no certificate, so it has no sensor key to hand back. */
    @Test
    public void reconnect_returnsNoSensorKey() throws Exception {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        byte[] cached = Hex.toBytes("404142434445464748494a4b4c4d4e4f");
        sensor.acceptedKAuth = cached;
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        PairingResult result = flow.reconnect(cached, BLE_PIN);

        // :: Verify
        assertThat(result.sensorKey65()).isNull();
    }

    /** A PIN that is not four bytes is rejected before anything reaches the sensor. */
    @Test
    public void firstPair_pinNotFourBytes_throwsBeforeAnyWrite() {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        ThrowingRunnable pair = () -> flow.firstPair(new byte[]{0x01, 0x02, 0x03});

        // :: Verify
        assertThrows(IllegalArgumentException.class, pair);
        assertThat(sensor.commands).isEmpty();
        assertThat(sensor.writes).isEmpty();
    }

    /** A sensor that goes quiet surfaces as an IOException that names the stage, with no partial result. */
    @Test
    public void firstPair_notifyTimeout_throwsNamingStage() {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        sensor.silentAfter = 0x0e;
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        ThrowingRunnable pair = () -> flow.firstPair(BLE_PIN);

        // :: Verify
        IOException e = assertThrows(IOException.class, pair);
        assertThat(e).hasMessageThat().contains("EphemeralReady");
        assertThat(sensor.commands).doesNotContain(0x11);
    }

    /** A command answered with the wrong opcode stops the handshake at that stage. */
    @Test
    public void firstPair_certificateRejected_throwsNamingStage() {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        sensor.certificateAnswer = 0x05;
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        ThrowingRunnable pair = () -> flow.firstPair(BLE_PIN);

        // :: Verify
        IOException e = assertThrows(IOException.class, pair);
        assertThat(e).hasMessageThat().contains("CertificateAccepted");
        assertThat(sensor.commands).containsExactly(0x01, 0x02, 0x03).inOrder();
    }

    /** A Phase 6 reply the phone's kAuth cannot open is a security failure, not a result. */
    @Test
    public void firstPair_phase6UnderOtherKey_throwsSecurityException() {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        sensor.phase6KeyOverride = Hex.toBytes("000102030405060708090a0b0c0d0e0f");
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        ThrowingRunnable pair = () -> flow.firstPair(BLE_PIN);

        // :: Verify
        assertThrows(GeneralSecurityException.class, pair);
    }

    /**
     * A Phase 6 reply that opens under kAuth but does not echo the challenge sent is not a result: it may be a reply
     * replayed from another session.
     */
    @Test
    public void firstPair_phase6NotEchoingChallenge_throwsSecurityException() {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        sensor.phase6R1Override = Hex.toBytes("ffffffffffffffffffffffffffffffff");
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        ThrowingRunnable pair = () -> flow.firstPair(BLE_PIN);

        // :: Verify
        GeneralSecurityException e = assertThrows(GeneralSecurityException.class, pair);
        assertThat(e).hasMessageThat().contains("echo");
    }

    /** A sensor that refuses the challenge on a first pair fails the pairing, naming the stage and the answer. */
    @Test
    public void firstPair_challengeRefused_throwsNamingStage() {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        sensor.challengeAnswer = 0x0b;
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        ThrowingRunnable pair = () -> flow.firstPair(BLE_PIN);

        // :: Verify
        IOException e = assertThrows(IOException.class, pair);
        assertThat(e).hasMessageThat().contains("PatchChallengeLoadDone");
        assertThat(e).hasMessageThat().contains("0b");
    }

    // =============================================== Cached reconnect ================================================

    /** A cached reconnect starts at StartAuthorization and never sends a certificate or ephemeral key. */
    @Test
    public void reconnect_cachedAccepted_skipsCertificateExchange() throws Exception {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        byte[] kAuth = Hex.toBytes("404142434445464748494a4b4c4d4e4f");
        sensor.acceptedKAuth = kAuth;
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        PairingResult result = flow.reconnect(kAuth, BLE_PIN);

        // :: Verify
        assertThat(sensor.commands).containsExactly(0x11, 0x08).inOrder();
        assertThat(sensor.writeCount(Gatt.SEC_CERT_DATA)).isEqualTo(0);
        assertThat(sensor.writeCount(Gatt.SEC_CHALLENGE_DATA)).isEqualTo(3);
        assertThat(result.usedCache()).isTrue();
        assertThat(result.kAuth()).isEqualTo(kAuth);
        assertThat(result.kEnc()).isEqualTo(K_ENC);
        assertThat(result.ivEnc()).isEqualTo(IV_ENC);
        assertThat(sensor.tailSeen).isEqualTo(BLE_PIN);
    }

    /**
     * A sensor that refuses StartAuthorization rejects the cached key: no full handshake on the same connection, so
     * the caller can drop the key and pair afresh next time.
     */
    @Test
    public void reconnect_startAuthorizationRefused_throwsWithoutFullSequence() {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        ThrowingRunnable reconnect = () -> flow.reconnect(Hex.toBytes("404142434445464748494a4b4c4d4e4f"), BLE_PIN);

        // :: Verify
        GeneralSecurityException e = assertThrows(GeneralSecurityException.class, reconnect);
        assertThat(e).hasMessageThat().contains("ChallengeLoadDone");
        assertThat(sensor.commands).containsExactly(0x11);
    }

    /** A cached key the sensor no longer holds is rejected without a full handshake on the same connection. */
    @Test
    public void reconnect_staleKey_throwsWithoutFullSequence() {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        sensor.acceptedKAuth = Hex.toBytes("404142434445464748494a4b4c4d4e4f");
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        ThrowingRunnable reconnect = () -> flow.reconnect(Hex.toBytes("ffeeddccbbaa99887766554433221100"), BLE_PIN);

        // :: Verify
        assertThrows(GeneralSecurityException.class, reconnect);
        assertThat(sensor.commands).containsExactly(0x11, 0x08).inOrder();
    }

    /** A sensor that refuses the challenge on a cached reconnect has rejected the key, so the caller can drop it. */
    @Test
    public void reconnect_challengeRefused_throwsSecurityException() {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        sensor.acceptedKAuth = Hex.toBytes("404142434445464748494a4b4c4d4e4f");
        sensor.challengeAnswer = 0x0b;
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        ThrowingRunnable reconnect = () -> flow.reconnect(sensor.acceptedKAuth, BLE_PIN);

        // :: Verify
        GeneralSecurityException e = assertThrows(GeneralSecurityException.class, reconnect);
        assertThat(e).hasMessageThat().contains("PatchChallengeLoadDone");
        assertThat(sensor.commands).containsExactly(0x11, 0x08).inOrder();
    }

    /** A link failure on the cached path is not a rejection: it surfaces and the cache is not abandoned. */
    @Test
    public void reconnect_linkFailure_doesNotFallBack() {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        sensor.acceptedKAuth = Hex.toBytes("404142434445464748494a4b4c4d4e4f");
        sensor.silentAfter = 0x11;
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        ThrowingRunnable reconnect = () -> flow.reconnect(sensor.acceptedKAuth, BLE_PIN);

        // :: Verify
        IOException e = assertThrows(IOException.class, reconnect);
        assertThat(e).hasMessageThat().contains("ChallengeLoadDone");
        assertThat(sensor.commands).containsExactly(0x11);
    }

    /** A PIN that is not four bytes is rejected before anything reaches the sensor. */
    @Test
    public void reconnect_pinNotFourBytes_throwsBeforeAnyWrite() {
        // :: Setup
        FakeSensor sensor = new FakeSensor();
        PairingFlow flow = flow(sensor, "0000000000000000000000000000000000000000000000000000000000002222");

        // :: Act
        ThrowingRunnable reconnect = () -> flow.reconnect(Hex.toBytes("404142434445464748494a4b4c4d4e4f"),
                new byte[5]);

        // :: Verify
        assertThrows(IllegalArgumentException.class, reconnect);
        assertThat(sensor.writes).isEmpty();
    }

    private static PairingFlow flow(FakeSensor sensor, String phoneEphemeralScalar) {
        try {
            AsymmetricCipherKeyPair ephemeral = EphemeralExchange.fromScalar(Hex.toBytes(phoneEphemeralScalar));
            return new PairingFlow(sensor, FakeSensor.identity("test"), () -> ephemeral);
        } catch (GeneralSecurityException e) {
            throw new AssertionError(e);
        }
    }
}
