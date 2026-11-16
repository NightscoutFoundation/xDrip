package xrwd.li3.pairing;

import static com.google.common.truth.Truth.assertThat;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import xrwd.li3.ble.Gatt;
import xrwd.li3.crypto.AesCcm;
import xrwd.li3.crypto.AppIdentity;
import xrwd.li3.crypto.SensorCert;
import xrwd.li3.util.Hex;

/**
 * Plays the sensor side of the handshake with its own synthetic keys, deriving kAuth from its end of the two ECDH
 * exchanges. Commands are answered on the command characteristic; certificate, ephemeral key, R1 and Phase 6 go out as
 * sequence-numbered notifies of up to 19 bytes. It knows one app identity, {@link #identity}; any other fails its
 * first pair.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public final class FakeSensor implements Li3Transport {

    /** The synthetic app identity from AppIdentityTest. */
    public static final String IDENTITY_SCALAR =
            "0000000000000000000000000000000000000000000000000000000000004444";
    public static final String IDENTITY_CERT = "03000102030405060708090a0b0c0d0e0f1000005f92a79201000000000000000004"
            + "a85b3aad6f3a346d85523141cb434e1caf4c642b2b3cc952cb07a635cb6a1df1bcbdadb66bae8eb25eca92f8b08b67be02cc73"
            + "6150dd7ffcd3f019c875f9ffde" + zeros(128);

    /** The session keys the sensor issues in Phase 6. */
    public static final byte[] K_ENC = Hex.toBytes("505152535455565758595a5b5c5d5e5f");
    public static final byte[] IV_ENC = Hex.toBytes("6061626364656667");

    private static final String OTHER_SCALAR = "0000000000000000000000000000000000000000000000000000000000005555";
    private static final int CERT_KEY_OFFSET = 33;

    private static final byte[] R1 = Hex.toBytes("101112131415161718191a1b1c1d1e1f");
    private static final byte[] R1_NONCE = Hex.toBytes("2104000080c4b0");
    private static final byte[] PHASE6_NONCE = Hex.toBytes("220400007f438e");

    final List<Integer> commands = new ArrayList<>();
    final List<UUID> writes = new ArrayList<>();
    final Map<UUID, ByteArrayOutputStream> received = new HashMap<>();
    final Map<UUID, Deque<byte[]>> notifies = new HashMap<>();

    byte[] phoneStatic65 = Arrays.copyOfRange(Hex.toBytes(IDENTITY_CERT), CERT_KEY_OFFSET, CERT_KEY_OFFSET + 65);
    /** The kAuth a cached reconnect is accepted with; null refuses every cached reconnect. */
    public byte[] acceptedKAuth;
    byte[] phase6KeyOverride;
    /** The R1 the Phase 6 reply echoes; null echoes the challenge that was sent. */
    byte[] phase6R1Override;
    /** The answer to SendChallengeLoadDone; 0x08 accepts the challenge. */
    int challengeAnswer = 0x08;
    int certificateAnswer = 0x04;
    int silentAfter = -1;

    byte[] phoneCert;
    byte[] kAuth;
    byte[] tailSeen;

    private final AsymmetricCipherKeyPair staticKey;
    private final AsymmetricCipherKeyPair ephemeralKey;
    private boolean fullHandshake;
    private boolean challengeSent;

    public FakeSensor() {
        try {
            staticKey = EphemeralExchange.fromScalar(Hex.toBytes(
                    "00000000000000000000000000000000000000000000000000000000000a1b2c"));
            ephemeralKey = EphemeralExchange.fromScalar(Hex.toBytes(
                    "00000000000000000000000000000000000000000000000000000000000d3e4f"));
        } catch (GeneralSecurityException e) {
            throw new AssertionError(e);
        }
    }

    /** The app identity this sensor pairs with. */
    public static AppIdentity identity(String label) {
        try {
            return AppIdentity.of(label, Hex.toBytes(IDENTITY_SCALAR), Hex.toBytes(IDENTITY_CERT));
        } catch (GeneralSecurityException e) {
            throw new AssertionError(e);
        }
    }

    /** A valid app identity this sensor does not know, so a first pair with it fails at Phase 6. */
    public static AppIdentity unknownIdentity(String label) {
        try {
            byte[] scalar = Hex.toBytes(OTHER_SCALAR);
            byte[] cert = Hex.toBytes(IDENTITY_CERT);
            byte[] key = EphemeralExchange.publicKey65(EphemeralExchange.fromScalar(scalar));
            System.arraycopy(key, 0, cert, CERT_KEY_OFFSET, key.length);
            return AppIdentity.of(label, scalar, cert);
        } catch (GeneralSecurityException e) {
            throw new AssertionError(e);
        }
    }

    byte[] staticKey65() {
        return EphemeralExchange.publicKey65(staticKey);
    }

    int writeCount(UUID characteristic) {
        int count = 0;
        for (UUID uuid : writes) {
            if (uuid.equals(characteristic)) {
                count++;
            }
        }
        return count;
    }

    /** Removes and returns the notifies queued on a characteristic since it was last asked, oldest first. */
    public List<byte[]> takeNotifies(UUID characteristic) {
        Deque<byte[]> queue = notifies.remove(characteristic);
        return queue == null ? new ArrayList<>() : new ArrayList<>(queue);
    }

    @Override
    public void write(UUID characteristic, byte[] value) throws IOException {
        writes.add(characteristic);
        if (!characteristic.equals(Gatt.SEC_COMMAND_RESPONSE)) {
            int offset = (value[0] & 0xff) | (value[1] & 0xff) << 8;
            ByteArrayOutputStream buffer = received.computeIfAbsent(characteristic,
                    k -> new ByteArrayOutputStream());
            assertThat(offset).isEqualTo(buffer.size());
            buffer.write(value, 2, value.length - 2);
            return;
        }
        assertThat(value).hasLength(1);
        int command = value[0] & 0xff;
        commands.add(command);
        if (command == silentAfter) {
            return;
        }
        try {
            answer(command);
        } catch (GeneralSecurityException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public byte[] awaitNotify(UUID characteristic, long timeoutMs) throws IOException {
        Deque<byte[]> queue = notifies.get(characteristic);
        if (queue == null || queue.isEmpty()) {
            throw new IOException("Timed out after " + timeoutMs + " ms");
        }
        return queue.removeFirst();
    }

    private void answer(int command) throws GeneralSecurityException {
        switch (command) {
            case 0x01:
                fullHandshake = true;
                challengeSent = false;
                received.clear();
                break;
            case 0x03:
                phoneCert = take(Gatt.SEC_CERT_DATA);
                respond(certificateAnswer);
                break;
            case 0x09:
                respond(0x0a);
                byte[] cert = new byte[SensorCert.SIZE];
                System.arraycopy(EphemeralExchange.publicKey65(staticKey), 0, cert, 11, 65);
                notify(Gatt.SEC_CERT_DATA, cert);
                break;
            case 0x0e:
                byte[] phoneEphemeral = Arrays.copyOf(take(Gatt.SEC_CERT_DATA), 65);
                kAuth = deriveKAuth(phoneEphemeral);
                respond(0x0f);
                notify(Gatt.SEC_CERT_DATA, EphemeralExchange.publicKey65(ephemeralKey));
                break;
            case 0x11:
                if (!fullHandshake && acceptedKAuth == null) {
                    respond(0x0b);
                    break;
                }
                if (!fullHandshake) {
                    kAuth = acceptedKAuth;
                }
                respond(0x08);
                notify(Gatt.SEC_CHALLENGE_DATA, concat(R1, R1_NONCE));
                challengeSent = true;
                break;
            case 0x08:
                if (!challengeSent) {
                    respond(0x0b);
                    break;
                }
                byte[] phase5 = take(Gatt.SEC_CHALLENGE_DATA);
                byte[] phoneR2 = new byte[16];
                try {
                    byte[] plain = AesCcm.decrypt(kAuth, R1_NONCE, Arrays.copyOf(phase5, 40), 4);
                    assertThat(Arrays.copyOf(plain, 16)).isEqualTo(R1);
                    phoneR2 = Arrays.copyOfRange(plain, 16, 32);
                    tailSeen = Arrays.copyOfRange(plain, 32, 36);
                } catch (GeneralSecurityException wrongKey) {
                    // the sensor answers anyway; the phone cannot open what follows
                }
                respond(challengeAnswer);
                if (challengeAnswer != 0x08) {
                    break;
                }
                byte[] key = phase6KeyOverride != null ? phase6KeyOverride : kAuth;
                byte[] r1 = phase6R1Override != null ? phase6R1Override : R1;
                byte[] sealed = AesCcm.encrypt(key, PHASE6_NONCE, concat(phoneR2, r1, K_ENC, IV_ENC), 4);
                notify(Gatt.SEC_CHALLENGE_DATA, concat(sealed, PHASE6_NONCE));
                break;
            default:
                break;
        }
    }

    private byte[] deriveKAuth(byte[] phoneEphemeral65) throws GeneralSecurityException {
        byte[] ze = EphemeralExchange.sharedSecret((ECPrivateKeyParameters) ephemeralKey.getPrivate(),
                EphemeralExchange.parsePeer(phoneEphemeral65));
        byte[] zs = EphemeralExchange.sharedSecret((ECPrivateKeyParameters) staticKey.getPrivate(),
                EphemeralExchange.parsePeer(phoneStatic65));
        SHA256Digest sha = new SHA256Digest();
        sha.update(new byte[]{0, 0, 0, 1}, 0, 4);
        sha.update(ze, 0, ze.length);
        sha.update(zs, 0, zs.length);
        byte[] digest = new byte[32];
        sha.doFinal(digest, 0);
        return Arrays.copyOf(digest, 16);
    }

    private byte[] take(UUID characteristic) {
        ByteArrayOutputStream buffer = received.remove(characteristic);
        return buffer == null ? new byte[0] : buffer.toByteArray();
    }

    private void respond(int opcode) {
        queue(Gatt.SEC_COMMAND_RESPONSE).add(new byte[]{(byte) opcode, 0x17});
    }

    private void notify(UUID characteristic, byte[] message) {
        int sequence = 0;
        for (int offset = 0; offset < message.length; offset += 19, sequence++) {
            int length = Math.min(19, message.length - offset);
            byte[] chunk = new byte[1 + length];
            chunk[0] = (byte) sequence;
            System.arraycopy(message, offset, chunk, 1, length);
            queue(characteristic).add(chunk);
        }
    }

    private Deque<byte[]> queue(UUID characteristic) {
        return notifies.computeIfAbsent(characteristic, k -> new ArrayDeque<>());
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    private static String zeros(int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, '0');
        return new String(chars);
    }
}
