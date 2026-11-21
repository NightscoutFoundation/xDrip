package xrwd.li3.crypto;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

import xrwd.li3.util.Hex;
import xrwd.li3.util.Reversed;

/**
 * Tests for unpacking the downloaded app identity bundle: XZ, then a SHA-256 pin, then the JSON shape and every
 * identity's key against its certificate. The fixture is built here from the synthetic identity in AppIdentityTest.
 * Mirrors the checks in xDrip4iOS {@code *IdentityStore.swift}.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
@RunWith(RobolectricTestRunner.class)
public class IdentityBundleTest {

    private static final String SCALAR = "0000000000000000000000000000000000000000000000000000000000004444";
    private static final String OTHER_SCALAR = "0000000000000000000000000000000000000000000000000000000000004445";
    private static final String CERT = "03000102030405060708090a0b0c0d0e0f1000005f92a79201000000000000000004a85b3aad"
            + "6f3a346d85523141cb434e1caf4c642b2b3cc952cb07a635cb6a1df1bcbdadb66bae8eb25eca92f8b08b67be02cc736150dd7ffcd3"
            + "f019c875f9ffde" + repeat('0', 128);

    /** LZMA2 dictionary property bytes: 28 is 64 MiB, 30 is 128 MiB. */
    private static final int DICT_64_MIB = 28;
    private static final int DICT_128_MIB = 30;

    // ================================================= Valid bundle ==================================================

    /** A bundle whose digest matches unpacks to its identities, keys checked against certificates. */
    @Test
    public void unpack_valid_returnsIdentities() throws Exception {
        // :: Setup
        byte[] json = bundle(1, entry("a", SCALAR, CERT));

        // :: Act
        List<AppIdentity> identities = IdentityBundle.unpack(xz(json), sha256(json));

        // :: Verify
        assertThat(identities).hasSize(1);
        assertThat(identities.get(0).label()).isEqualTo("a");
        assertThat(identities.get(0).certificate()).isEqualTo(Hex.toBytes(CERT));
    }

    /** Decompressing gives back the JSON exactly, so it can be kept and verified later without unpacking again. */
    @Test
    public void decompress_valid_returnsJson() throws Exception {
        // :: Setup
        byte[] json = bundle(1, entry("a", SCALAR, CERT));

        // :: Act
        byte[] out = IdentityBundle.decompress(xz(json));

        // :: Verify
        assertThat(out).isEqualTo(json);
    }

    /** Verifying the decompressed JSON gives the same identities as unpacking the download. */
    @Test
    public void verify_valid_returnsIdentities() throws Exception {
        // :: Setup
        byte[] json = bundle(1, entry("a", SCALAR, CERT));

        // :: Act
        List<AppIdentity> identities = IdentityBundle.verify(json, sha256(json));

        // :: Verify
        assertThat(identities).hasSize(1);
        assertThat(identities.get(0).label()).isEqualTo("a");
    }

    /**
     * The pinned source matches the xDrip4iOS release it was taken from. The transaction is checked by its SHA-256 and
     * each gateway is built from a separately reversed host, so a wrong character in a reversed literal fails here.
     */
    @Test
    public void constants_matchPinnedRelease() throws NoSuchAlgorithmException {
        // :: Act
        String digest = IdentityBundle.DIGEST;

        // :: Verify
        assertThat(digest).isEqualTo("ab18ce8bcb978526e102cc5e16440f06d2aee19426c26a15dd0013d66e9995ed");
        assertThat(sha256(IdentityBundle.SOURCE_TX.getBytes(StandardCharsets.US_ASCII)))
                .isEqualTo("95f95cc7a90ee1fe394e941453b6605cd55118f67096005530c1e160cb3fc049");
        assertThat(IdentityBundle.VERSION).isEqualTo("app-identities-1");
        assertThat(IdentityBundle.GATEWAYS).asList()
                .containsExactly("https://" + Reversed.of("ten.evaewra") + "/",
                        "https://" + Reversed.of("moc.yawetag-obrut") + "/")
                .inOrder();
    }

    // =============================================== Rejected bundles ================================================

    /** A bundle that does not hash to the pinned digest is rejected. */
    @Test
    public void unpack_wrongDigest_throws() throws Exception {
        // :: Setup
        byte[] json = bundle(1, entry("a", SCALAR, CERT));
        String digest = sha256("other".getBytes(StandardCharsets.UTF_8));

        // :: Act
        ThrowingRunnable unpack = () -> IdentityBundle.unpack(xz(json), digest);

        // :: Verify
        assertThrows(GeneralSecurityException.class, unpack);
    }

    /** Kept JSON that no longer hashes to the pinned digest is rejected. */
    @Test
    public void verify_wrongDigest_throws() throws Exception {
        // :: Setup
        byte[] json = bundle(1, entry("a", SCALAR, CERT));
        String digest = sha256("other".getBytes(StandardCharsets.UTF_8));

        // :: Act
        ThrowingRunnable verify = () -> IdentityBundle.verify(json, digest);

        // :: Verify
        assertThrows(GeneralSecurityException.class, verify);
    }

    /** A cut-off download is rejected, not half-read. */
    @Test
    public void unpack_truncatedXz_throws() throws Exception {
        // :: Setup
        byte[] json = bundle(1, entry("a", SCALAR, CERT));
        byte[] packed = xz(json);
        byte[] truncated = Arrays.copyOf(packed, packed.length - 20);

        // :: Act
        ThrowingRunnable unpack = () -> IdentityBundle.unpack(truncated, sha256(json));

        // :: Verify
        assertThrows(GeneralSecurityException.class, unpack);
    }

    /** The published bundle is packed with a 64 MiB dictionary (xz -9), which needs a little over 64 MiB to unpack. */
    @Test
    public void unpack_64MibDictionary_unpacks() throws Exception {
        // :: Setup
        byte[] json = bundle(1, entry("a", SCALAR, CERT));
        byte[] packed = withDictionaryProperty(xz(json), DICT_64_MIB);

        // :: Act
        List<AppIdentity> identities = IdentityBundle.unpack(packed, sha256(json));

        // :: Verify
        assertThat(identities).hasSize(1);
    }

    /** A header asking for a larger dictionary is refused, and the message says it is about memory. */
    @Test
    public void unpack_128MibDictionary_throwsNamingMemory() throws Exception {
        // :: Setup
        byte[] json = bundle(1, entry("a", SCALAR, CERT));
        byte[] packed = withDictionaryProperty(xz(json), DICT_128_MIB);

        // :: Act
        ThrowingRunnable unpack = () -> IdentityBundle.unpack(packed, sha256(json));

        // :: Verify
        GeneralSecurityException e = assertThrows(GeneralSecurityException.class, unpack);
        assertThat(e).hasMessageThat().contains("memory");
    }

    /** A small download that expands past the cap is rejected before it is hashed or parsed. */
    @Test
    public void unpack_decompressedOverCap_throws() throws Exception {
        // :: Setup
        byte[] json = new byte[IdentityBundle.MAX_BYTES + 1];
        Arrays.fill(json, (byte) ' ');

        // :: Act
        ThrowingRunnable unpack = () -> IdentityBundle.unpack(xz(json), sha256(json));

        // :: Verify
        assertThrows(GeneralSecurityException.class, unpack);
    }

    /** A download larger than the cap is rejected before it is decompressed. */
    @Test
    public void unpack_compressedOverCap_throws() {
        // :: Setup
        byte[] packed = new byte[IdentityBundle.MAX_BYTES + 1];

        // :: Act
        ThrowingRunnable unpack = () -> IdentityBundle.unpack(packed, IdentityBundle.DIGEST);

        // :: Verify
        assertThrows(GeneralSecurityException.class, unpack);
    }

    /** A format other than 1 is rejected. */
    @Test
    public void unpack_otherFormat_throws() throws Exception {
        // :: Setup
        byte[] json = bundle(2, entry("a", SCALAR, CERT));

        // :: Act
        ThrowingRunnable unpack = () -> IdentityBundle.unpack(xz(json), sha256(json));

        // :: Verify
        assertThrows(GeneralSecurityException.class, unpack);
    }

    /** A bundle with no identities is rejected. */
    @Test
    public void unpack_noIdentities_throws() throws Exception {
        // :: Setup
        byte[] json = bundle(1);

        // :: Act
        ThrowingRunnable unpack = () -> IdentityBundle.unpack(xz(json), sha256(json));

        // :: Verify
        assertThrows(GeneralSecurityException.class, unpack);
    }

    /** More than 16 identities is rejected. */
    @Test
    public void unpack_seventeenIdentities_throws() throws Exception {
        // :: Setup
        JSONObject[] entries = new JSONObject[17];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = entry("id" + i, SCALAR, CERT);
        }
        byte[] json = bundle(1, entries);

        // :: Act
        ThrowingRunnable unpack = () -> IdentityBundle.unpack(xz(json), sha256(json));

        // :: Verify
        assertThrows(GeneralSecurityException.class, unpack);
    }

    /** Two identities under one label are rejected. */
    @Test
    public void unpack_duplicateLabel_throws() throws Exception {
        // :: Setup
        byte[] json = bundle(1, entry("a", SCALAR, CERT), entry("a", SCALAR, CERT));

        // :: Act
        ThrowingRunnable unpack = () -> IdentityBundle.unpack(xz(json), sha256(json));

        // :: Verify
        assertThrows(GeneralSecurityException.class, unpack);
    }

    /** A private key one hex digit short is rejected. */
    @Test
    public void unpack_shortKeyHex_throws() throws Exception {
        // :: Setup
        byte[] json = bundle(1, entry("a", SCALAR.substring(1), CERT));

        // :: Act
        ThrowingRunnable unpack = () -> IdentityBundle.unpack(xz(json), sha256(json));

        // :: Verify
        assertThrows(GeneralSecurityException.class, unpack);
    }

    /** A key written with a {@code 0x} prefix is not quietly cleaned up: the hex must be exact. */
    @Test
    public void unpack_prefixedKeyHex_throws() throws Exception {
        // :: Setup
        byte[] json = bundle(1, entry("a", "0x" + SCALAR.substring(2), CERT));

        // :: Act
        ThrowingRunnable unpack = () -> IdentityBundle.unpack(xz(json), sha256(json));

        // :: Verify
        assertThrows(GeneralSecurityException.class, unpack);
    }

    /** A private key that does not belong to its certificate is rejected. */
    @Test
    public void unpack_keyCertMismatch_throws() throws Exception {
        // :: Setup
        byte[] json = bundle(1, entry("a", OTHER_SCALAR, CERT));

        // :: Act
        ThrowingRunnable unpack = () -> IdentityBundle.unpack(xz(json), sha256(json));

        // :: Verify
        assertThrows(GeneralSecurityException.class, unpack);
    }

    /** A bundle that is not JSON at all is rejected. */
    @Test
    public void unpack_notJson_throws() throws Exception {
        // :: Setup
        byte[] json = "not json".getBytes(StandardCharsets.UTF_8);

        // :: Act
        ThrowingRunnable unpack = () -> IdentityBundle.unpack(xz(json), sha256(json));

        // :: Verify
        assertThrows(GeneralSecurityException.class, unpack);
    }

    private static JSONObject entry(String label, String keyHex, String certHex) throws Exception {
        return new JSONObject()
                .put("label", label)
                .put("privateKeyHex", keyHex)
                .put("certificateHex", certHex);
    }

    private static byte[] bundle(int format, JSONObject... entries) throws Exception {
        JSONArray identities = new JSONArray();
        for (JSONObject entry : entries) {
            identities.put(entry);
        }
        return new JSONObject()
                .put("format", format)
                .put("curve", "P-256")
                .put("identities", identities)
                .toString()
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] xz(byte[] plain) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XZOutputStream xz = new XZOutputStream(out, new LZMA2Options())) {
            xz.write(plain);
        }
        return out.toByteArray();
    }

    /**
     * Rewrites the LZMA2 dictionary size in the first block header and its CRC32. The data is small, so it decodes
     * the same with any dictionary; only the memory the header asks for changes.
     */
    private static byte[] withDictionaryProperty(byte[] packed, int property) {
        byte[] out = packed.clone();
        int header = 12;
        int size = ((out[header] & 0xff) + 1) * 4;
        assertThat(out[header + 2]).isEqualTo((byte) 0x21);
        out[header + 4] = (byte) property;
        CRC32 crc = new CRC32();
        crc.update(out, header, size - 4);
        long value = crc.getValue();
        for (int i = 0; i < 4; i++) {
            out[header + size - 4 + i] = (byte) (value >> (8 * i));
        }
        return out;
    }

    private static String sha256(byte[] data) throws NoSuchAlgorithmException {
        return Hex.of(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
