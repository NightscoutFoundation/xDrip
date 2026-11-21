package xrwd.li3.pairing;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;

import xrwd.li3.ble.Gatt;
import xrwd.li3.ble.NotifyReassembler;
import xrwd.li3.ble.WriteFragmenter;
import xrwd.li3.crypto.AppIdentity;
import xrwd.li3.crypto.SensorCert;
import xrwd.li3.crypto.SessionKeys;

/**
 * The security-channel handshake that turns a connected sensor into a pair of data-plane keys. A first pair exchanges
 * certificates and ephemeral keys to derive kAuth; a reconnect skips straight to the challenge with a cached kAuth
 * and falls back to a first pair in the same session when the sensor turns that key down. A link failure is not a
 * refusal: it surfaces as an {@link IOException} naming the stage, and the cached key stays good. The sensor
 * certificate is not verified against the manufacturer's signing keys; instead the caller can pin the static key a
 * sensor showed at its first pairing, so a later first pair with another device is refused before the PIN is sent.
 * Ported from RoundWhiteDiscKit {@code Pairing/PairingFlow.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class PairingFlow {

    static final long COMMAND_TIMEOUT_MS = 2000;
    static final long NOTIFY_TIMEOUT_MS = 10000;

    private static final int START_AUTHENTICATION = 0x01;
    private static final int LOAD_CERTIFICATE = 0x02;
    private static final int CERTIFICATE_LOAD_DONE = 0x03;
    private static final int CERTIFICATE_ACCEPTED = 0x04;
    private static final int CHALLENGE_LOAD_DONE = 0x08;
    private static final int GET_CERTIFICATE = 0x09;
    private static final int CERTIFICATE_READY = 0x0a;
    private static final int VALIDATE_CERTIFICATE = 0x0d;
    private static final int EPHEMERAL_LOAD_DONE = 0x0e;
    private static final int EPHEMERAL_READY = 0x0f;
    private static final int START_AUTHORIZATION = 0x11;

    private static final int EPHEMERAL_WIRE_SIZE = 72;
    private static final int CHALLENGE_SIZE = 23;
    private static final int KEY_SIZE = 16;
    private static final int PIN_SIZE = 4;

    private final Li3Transport transport;
    private final AppIdentity identity;
    private final Supplier<AsymmetricCipherKeyPair> ephemeral;
    private final SecureRandom random = new SecureRandom();

    public PairingFlow(Li3Transport transport, AppIdentity identity, Supplier<AsymmetricCipherKeyPair> ephemeral) {
        this.transport = transport;
        this.identity = identity;
        this.ephemeral = ephemeral;
    }

    /**
     * Runs the full handshake: certificates, ephemeral keys, then the challenge.
     *
     * @param blePin the 4-byte BLE PIN from activation
     * @throws IOException              when the link fails or the sensor answers out of turn, naming the stage
     * @throws GeneralSecurityException when the sensor's keys or Phase 6 reply do not check out
     */
    public PairingResult firstPair(byte[] blePin) throws IOException, GeneralSecurityException {
        return firstPair(blePin, null);
    }

    /**
     * Runs the full handshake, refusing a sensor whose static key is not the pinned one.
     *
     * @param blePin          the 4-byte BLE PIN from activation
     * @param pinnedSensorKey the static key this sensor showed at its first pairing, or null to accept any
     * @throws IOException              when the link fails or the sensor answers out of turn, naming the stage
     * @throws GeneralSecurityException when the sensor shows another key than the pinned one, before the PIN is sent,
     *                                  or its keys or Phase 6 reply do not check out
     */
    public PairingResult firstPair(byte[] blePin, byte[] pinnedSensorKey)
            throws IOException, GeneralSecurityException {
        requireLength("BLE PIN", blePin, PIN_SIZE);
        command("StartAuthentication", START_AUTHENTICATION);
        command("LoadCertificate", LOAD_CERTIFICATE);
        writeFragmented("CertificateWrite", Gatt.SEC_CERT_DATA, identity.certificate());
        command("SendCertificateLoadDone", CERTIFICATE_LOAD_DONE);
        expectResponse("CertificateAccepted", CERTIFICATE_ACCEPTED);
        command("GetCertificate", GET_CERTIFICATE);
        expectResponse("CertificateReady", CERTIFICATE_READY);
        SensorCert sensorCert = SensorCert.parse(readNotify("SensorCertificate", Gatt.SEC_CERT_DATA, SensorCert.SIZE));
        if (pinnedSensorKey != null && !MessageDigest.isEqual(pinnedSensorKey, sensorCert.publicKey65())) {
            throw new GeneralSecurityException("Li3 pairing refused: the device shows another key than the sensor"
                    + " paired before");
        }
        command("ValidateCertificate", VALIDATE_CERTIFICATE);

        AsymmetricCipherKeyPair phoneEphemeral = ephemeral.get();
        writeFragmented("EphemeralWrite", Gatt.SEC_CERT_DATA,
                Arrays.copyOf(EphemeralExchange.publicKey65(phoneEphemeral), EPHEMERAL_WIRE_SIZE));
        command("SendEphemeralDone", EPHEMERAL_LOAD_DONE);
        expectResponse("EphemeralReady", EPHEMERAL_READY);
        byte[] sensorEphemeral = readNotify("SensorEphemeral", Gatt.SEC_CERT_DATA, 65);
        byte[] kAuth = SessionKeys.kAuth(identity, phoneEphemeral, sensorEphemeral, sensorCert.publicKey65());

        command("StartAuthorization", START_AUTHORIZATION);
        expectResponse("ChallengeLoadDone", CHALLENGE_LOAD_DONE);
        return challenge(kAuth, blePin, sensorCert.publicKey65(), false);
    }

    /**
     * Reconnects with a cached kAuth. A sensor that refuses StartAuthorization, refuses the challenge, or answers
     * with a Phase 6 reply the cached key cannot open has rejected the key; there is no full handshake on the same
     * connection, so the caller drops the key and pairs afresh on the next one.
     *
     * @throws IOException              when the link fails, naming the stage; the cached key is not to blame
     * @throws GeneralSecurityException when the sensor rejects the cached key
     */
    public PairingResult reconnect(byte[] cachedKAuth, byte[] blePin) throws IOException, GeneralSecurityException {
        requireLength("cached kAuth", cachedKAuth, KEY_SIZE);
        requireLength("BLE PIN", blePin, PIN_SIZE);
        command("StartAuthorization", START_AUTHORIZATION);
        if (response("ChallengeLoadDone") != CHALLENGE_LOAD_DONE) {
            throw new GeneralSecurityException("Cached key refused at ChallengeLoadDone");
        }
        PairingResult result = challenge(cachedKAuth, blePin, null, true);
        if (result == null) {
            throw new GeneralSecurityException("Cached key refused at PatchChallengeLoadDone");
        }
        return result;
    }

    /**
     * @param sensorKey65 the sensor's static key from a first pair, or null on a reconnect
     * @return null when a cached-key challenge is refused before Phase 6
     */
    private PairingResult challenge(byte[] kAuth, byte[] blePin, byte[] sensorKey65, boolean cached)
            throws IOException, GeneralSecurityException {
        byte[] challenge = readNotify("Challenge", Gatt.SEC_CHALLENGE_DATA, CHALLENGE_SIZE);
        byte[] sensorR1 = Arrays.copyOfRange(challenge, 0, 16);
        byte[] nonce = Arrays.copyOfRange(challenge, 16, CHALLENGE_SIZE);
        byte[] phoneR2 = new byte[16];
        random.nextBytes(phoneR2);
        writeFragmented("Phase5", Gatt.SEC_CHALLENGE_DATA,
                Phase5Challenge.build(kAuth, sensorR1, phoneR2, blePin, nonce));
        command("SendChallengeLoadDone", CHALLENGE_LOAD_DONE);
        int answer = response("PatchChallengeLoadDone");
        if (answer != CHALLENGE_LOAD_DONE) {
            if (cached) {
                return null;
            }
            throw unexpected("PatchChallengeLoadDone", CHALLENGE_LOAD_DONE, answer);
        }
        Phase6Response reply = Phase6Response.open(kAuth,
                readNotify("Phase6", Gatt.SEC_CHALLENGE_DATA, Phase6Response.WIRE_SIZE));
        if (!MessageDigest.isEqual(reply.phoneR2(), phoneR2) || !MessageDigest.isEqual(reply.sensorR1(), sensorR1)) {
            throw new GeneralSecurityException("Phase 6 reply does not echo the challenge");
        }
        return new PairingResult(kAuth, reply.kEnc(), reply.ivEnc(), sensorKey65, cached, "Phase6");
    }

    private void command(String stage, int opcode) throws IOException {
        try {
            transport.write(Gatt.SEC_COMMAND_RESPONSE, new byte[]{(byte) opcode});
        } catch (IOException e) {
            throw failed(stage, e);
        }
    }

    /** @return the response opcode, or -1 for an empty response */
    private int response(String stage) throws IOException {
        byte[] value;
        try {
            value = transport.awaitNotify(Gatt.SEC_COMMAND_RESPONSE, COMMAND_TIMEOUT_MS);
        } catch (IOException e) {
            throw failed(stage, e);
        }
        return value.length == 0 ? -1 : value[0] & 0xff;
    }

    private void expectResponse(String stage, int opcode) throws IOException {
        int answer = response(stage);
        if (answer != opcode) {
            throw unexpected(stage, opcode, answer);
        }
    }

    private void writeFragmented(String stage, UUID characteristic, byte[] message) throws IOException {
        try {
            for (byte[] fragment : WriteFragmenter.split(message)) {
                transport.write(characteristic, fragment);
            }
        } catch (IOException e) {
            throw failed(stage, e);
        }
    }

    private byte[] readNotify(String stage, UUID characteristic, int length) throws IOException {
        NotifyReassembler reassembler = new NotifyReassembler(length);
        try {
            while (!reassembler.add(transport.awaitNotify(characteristic, NOTIFY_TIMEOUT_MS))) {
                // keep reading fragments until the message is whole
            }
        } catch (IOException e) {
            throw failed(stage, e);
        }
        return reassembler.result();
    }

    private static IOException failed(String stage, IOException cause) {
        return new IOException("Li3 pairing failed at " + stage + ": " + cause.getMessage(), cause);
    }

    private static IOException unexpected(String stage, int expected, int answer) {
        return new IOException(String.format("Li3 pairing failed at %s: expected response %02x, got %s",
                stage, expected, answer < 0 ? "nothing" : String.format("%02x", answer)));
    }

    private static void requireLength(String name, byte[] value, int length) {
        if (value == null || value.length != length) {
            throw new IllegalArgumentException(name + " must be " + length + " bytes");
        }
    }
}
