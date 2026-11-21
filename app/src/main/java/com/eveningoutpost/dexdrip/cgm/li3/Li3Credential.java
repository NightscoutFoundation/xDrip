package com.eveningoutpost.dexdrip.cgm.li3;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import xrwd.li3.crypto.AppIdentity;
import xrwd.li3.crypto.IdentityBundle;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The app credential the sensor pairs with, kept as the bundle's verified JSON in the app's private files. Keeping
 * the JSON rather than the xz means loading it checks the digest again without unpacking a 64 MiB dictionary. A
 * download replaces the stored bundle only after it unpacks against the pinned digest, so a bad one never loses a
 * good one. The bundle's contents are never logged.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class Li3Credential {

    private static final String TAG = "Li3";
    private static final String FILE_NAME = "identities.json";
    private static final String ERROR_FILE = "download-error.txt";

    private final File dir;
    private final String digest;

    /**
     * @param dir    where the bundle is kept
     * @param digest the SHA-256 the unpacked bundle must have
     */
    public Li3Credential(File dir, String digest) {
        this.dir = dir;
        this.digest = digest;
    }

    /** The digest {@link #get} pins: the release's, or in a test a synthetic bundle's. */
    @VisibleForTesting
    static String pinnedDigest = IdentityBundle.DIGEST;

    /** The credential for the pinned release, in the app's private files. */
    public static Li3Credential get(Context context) {
        return new Li3Credential(new File(context.getFilesDir(), "li3"), pinnedDigest);
    }

    /**
     * Keeps the bundle if it unpacks against the digest.
     *
     * @throws GeneralSecurityException when it does not; the stored bundle is left as it was
     */
    public synchronized void store(byte[] xz) throws GeneralSecurityException {
        byte[] json = IdentityBundle.decompress(xz);
        IdentityBundle.verify(json, digest);
        File tmp = new File(dir, FILE_NAME + ".tmp");
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) {
                throw new IOException("Cannot create " + dir);
            }
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(json);
            }
            if (!tmp.renameTo(new File(dir, FILE_NAME))) {
                throw new IOException("Cannot rename the downloaded credential");
            }
            JoH.cancelNotification(Constants.LI3_CREDENTIAL_NOTIFICATION_ID);
        } catch (IOException e) {
            tmp.delete();
            throw new GeneralSecurityException("Could not save the credential: " + e.getMessage(), e);
        }
    }

    /** The stored identities, or none when nothing usable is stored. */
    public synchronized List<AppIdentity> load() {
        File file = new File(dir, FILE_NAME);
        if (!file.isFile()) {
            return Collections.emptyList();
        }
        try {
            return IdentityBundle.verify(Files.readAllBytes(file.toPath()), digest);
        } catch (IOException | GeneralSecurityException e) {
            UserError.Log.e(TAG, "Stored Li3 credential is unusable: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetches the pinned release from each gateway in turn and keeps the first that unpacks. Why the last download
     * failed is kept for {@link #lastDownloadError}, and cleared when one works.
     *
     * @throws GeneralSecurityException when no gateway gave a bundle that unpacks
     */
    public void download(OkHttpClient client) throws GeneralSecurityException {
        StringBuilder errors = new StringBuilder();
        for (String gateway : IdentityBundle.GATEWAYS) {
            try {
                store(fetch(client, gateway + IdentityBundle.SOURCE_TX));
                new File(dir, ERROR_FILE).delete();
                UserError.Log.uel(TAG, "Li3 credential downloaded from " + gateway);
                return;
            } catch (IOException | GeneralSecurityException e) {
                failed(errors, gateway, e.getMessage());
            } catch (RuntimeException | OutOfMemoryError e) {
                // a fault in a reply or in unpacking it must not crash xDrip, and the next gateway may still work
                failed(errors, gateway, e.toString());
            }
        }
        String failure = errors.length() == 0 ? "no gateway" : errors.toString();
        saveError(failure);
        throw new GeneralSecurityException("Credential download failed (" + failure + ")");
    }

    private static void failed(StringBuilder errors, String gateway, String reason) {
        String error = gateway + ": " + reason;
        UserError.Log.e(TAG, "Li3 credential download failed from " + error);
        errors.append(errors.length() == 0 ? "" : "; ").append(error);
    }

    /** Why the last download failed, or empty when it worked or none was tried. */
    public synchronized String lastDownloadError() {
        try {
            File file = new File(dir, ERROR_FILE);
            return file.isFile() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            return "";
        }
    }

    private synchronized void saveError(String error) {
        try {
            if (dir.isDirectory() || dir.mkdirs()) {
                Files.write(new File(dir, ERROR_FILE).toPath(), error.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            UserError.Log.e(TAG, "Could not save the credential download error: " + e.getMessage());
        }
    }

    private static byte[] fetch(OkHttpClient client, String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IOException("HTTP " + response.code());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            try (InputStream in = body.byteStream()) {
                int n;
                while ((n = in.read(buffer)) != -1) {
                    if (out.size() + n > IdentityBundle.MAX_BYTES) {
                        throw new IOException("Larger than " + IdentityBundle.MAX_BYTES + " bytes");
                    }
                    out.write(buffer, 0, n);
                }
            }
            return out.toByteArray();
        }
    }
}
