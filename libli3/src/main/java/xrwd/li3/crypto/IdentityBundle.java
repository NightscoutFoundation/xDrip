package xrwd.li3.crypto;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tukaani.xz.XZInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import xrwd.li3.util.Hex;
import xrwd.li3.util.Reversed;

/**
 * The app identities a Li3 sensor accepts, as the user downloads them: an XZ-packed JSON file pinned by its
 * SHA-256 digest. Nothing in it is shipped with the app. Every check here must pass before an identity is used, and
 * any failure is the same {@link GeneralSecurityException}, so a bad download never reaches pairing half-read.
 * Mirrors xDrip4iOS {@code *IdentityStore.swift} {@code validate(json:digest:)} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class IdentityBundle {

    /** The permanent storage transaction the bundle is fetched from. */
    public static final String SOURCE_TX = Reversed.of("kpu0bQLws2NzcG-xCkfSuolYdtYo15Us2UHIXd3AbWq");
    public static final String[] GATEWAYS = {
            Reversed.of("/ten.evaewra//:sptth"), Reversed.of("/moc.yawetag-obrut//:sptth")};
    public static final String VERSION = "app-identities-1";
    /** SHA-256 of the decompressed JSON. */
    public static final String DIGEST = "ab18ce8bcb978526e102cc5e16440f06d2aee19426c26a15dd0013d66e9995ed";
    /** Cap on both the download and what it decompresses to. */
    public static final int MAX_BYTES = 1_000_000;

    private static final int MAX_IDENTITIES = 16;
    /** xz -9, as the published bundle uses, needs 65640 KiB to unpack; a header asking for more is refused. */
    private static final int XZ_MEMORY_LIMIT_KIB = 66 * 1024;
    private static final Pattern KEY_HEX = Pattern.compile("[0-9a-fA-F]{64}");
    private static final Pattern CERT_HEX = Pattern.compile("[0-9a-fA-F]{324}");

    private IdentityBundle() {
    }

    /**
     * @param xz             the downloaded file
     * @param expectedDigest lower-case hex SHA-256 the decompressed JSON must hash to
     * @return the identities, in file order
     * @throws GeneralSecurityException on any failure: size, decompression, digest, shape or key
     */
    public static List<AppIdentity> unpack(byte[] xz, String expectedDigest) throws GeneralSecurityException {
        return verify(decompress(xz), expectedDigest);
    }

    /**
     * Decompresses the download without checking it; {@link #verify} does that.
     *
     * @throws GeneralSecurityException when it is too large or does not decompress
     */
    public static byte[] decompress(byte[] xz) throws GeneralSecurityException {
        if (xz.length > MAX_BYTES) {
            throw new GeneralSecurityException("Identity bundle is larger than " + MAX_BYTES + " bytes");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = new XZInputStream(new ByteArrayInputStream(xz), XZ_MEMORY_LIMIT_KIB)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (out.size() > MAX_BYTES) {
                    throw new GeneralSecurityException("Identity bundle unpacks to more than " + MAX_BYTES
                            + " bytes");
                }
            }
        } catch (IOException e) {
            throw new GeneralSecurityException("Identity bundle does not decompress: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    /**
     * @param json           the decompressed bundle
     * @param expectedDigest lower-case hex SHA-256 the JSON must hash to
     * @return the identities, in file order
     * @throws GeneralSecurityException on a digest, shape or key failure
     */
    public static List<AppIdentity> verify(byte[] json, String expectedDigest) throws GeneralSecurityException {
        String digest = Hex.of(MessageDigest.getInstance("SHA-256").digest(json));
        if (!MessageDigest.isEqual(digest.getBytes(StandardCharsets.US_ASCII),
                expectedDigest.getBytes(StandardCharsets.US_ASCII))) {
            throw new GeneralSecurityException("Identity bundle does not match its pinned digest");
        }
        try {
            return parse(new JSONObject(new String(json, StandardCharsets.UTF_8)));
        } catch (JSONException e) {
            throw new GeneralSecurityException("Identity bundle is not the expected JSON", e);
        }
    }

    private static List<AppIdentity> parse(JSONObject file) throws JSONException, GeneralSecurityException {
        if (!Integer.valueOf(1).equals(file.get("format")) || !"P-256".equals(file.getString("curve"))) {
            throw new GeneralSecurityException("Identity bundle has an unknown format or curve");
        }
        JSONArray entries = file.getJSONArray("identities");
        if (entries.length() == 0 || entries.length() > MAX_IDENTITIES) {
            throw new GeneralSecurityException("Identity bundle holds " + entries.length() + " identities, not 1 to "
                    + MAX_IDENTITIES);
        }
        List<AppIdentity> identities = new ArrayList<>();
        Set<String> labels = new HashSet<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            String label = entry.getString("label");
            if (!labels.add(label)) {
                throw new GeneralSecurityException("Identity bundle repeats a label");
            }
            String keyHex = entry.getString("privateKeyHex");
            String certHex = entry.getString("certificateHex");
            if (!KEY_HEX.matcher(keyHex).matches() || !CERT_HEX.matcher(certHex).matches()) {
                throw new GeneralSecurityException("Identity '" + label + "' has malformed key or certificate hex");
            }
            identities.add(AppIdentity.of(label, Hex.toBytes(keyHex), Hex.toBytes(certHex)));
        }
        return Collections.unmodifiableList(identities);
    }
}
