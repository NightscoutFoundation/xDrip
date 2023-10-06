package com.eveningoutpost.dexdrip.cloud.backup;


import static android.provider.OpenableColumns.DISPLAY_NAME;
import static android.provider.OpenableColumns.SIZE;
import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.models.JoH.getFieldFromURI;
import static com.eveningoutpost.dexdrip.models.JoH.getLocalBluetoothName;
import static com.eveningoutpost.dexdrip.models.JoH.hexStringToByteArray;
import static com.eveningoutpost.dexdrip.models.JoH.readLine;
import static com.eveningoutpost.dexdrip.utils.SdcardImportExport.PREFERENCES_FILE;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

import android.net.Uri;
import android.os.Build;
import android.util.Pair;

import com.activeandroid.Configuration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.utils.SdcardImportExport;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.val;

/**
 * JamOrHam
 * Backup and Restore to Document URI such as google drive file location
 * or using an xDrip managed Google Drive folder
 */


public class Backup {
    private static final String TAG = "xDrip-Backup";
    private static final String PREF_BACKUP_URI = "backup-document-uri";
    public static final String PREF_AUTO_BACKUP = "backup-automatic-enabled";
    public static final String PREF_AUTO_BACKUP_MOBILE = "backup-automatic-mobile";
    private static final String XDRIP_CONTENT_TYPE = "xDripBackup://";
    private static final String[] dbSuffix = {"-journal", "-shm", "-wal"};

    public static boolean compressEncryptFilesToUri(final BackupStatus status, final String destinationUri, final String... sourcePaths) {
        if (!emptyString(destinationUri)) {
            try {
                if (isXdripManagedUri(destinationUri)) {
                    return compressEncryptFilesXdripStream(status, destinationUri, sourcePaths);
                } else {
                    try (val pfd = xdrip.getAppContext().getContentResolver()
                            .openFileDescriptor(Uri.parse(destinationUri), "w")) {
                        try (val outputStream = new FileOutputStream(pfd.getFileDescriptor())) {
                            return compressEncryptFilesToOutputStream(outputStream, sourcePaths);
                        }
                    }
                }
            } catch (NullPointerException | IOException e) {
                UserError.Log.e(TAG, "Output IO Error " + e);
                status.status(xdrip.gs(R.string.output_error) + " " + e);
            } catch (SecurityException e) {
                UserError.Log.wtf(TAG, "Got weird security error: " + e);
                status.status(xdrip.gs(R.string.security_error) + " " + e);
            }
            return false;
        } else {
            UserError.Log.d(TAG, "No destination specified");
            return false;
        }
    }

    public static boolean compressEncryptFilesToOutputStream(final OutputStream outputStream, final String... sourcePaths) {
        try {
            val metaData = new BackupMetaData();
            metaData.sourceDevice = cleanPhoneName();
            metaData.ob2 = CipherUtils.getRandomHexKey();
            metaData.writeToOutputStream(outputStream);
            try (val cipherOutputStream = new CipherOutputStream(outputStream, getCipher(true, metaData))) {
                try (val gzipOutputStream = new GZIPOutputStream(cipherOutputStream)) {
                    for (val sourcePath : sourcePaths) {
                        pushFileToStreamWithHeader(sourcePath, gzipOutputStream);
                    }
                    gzipOutputStream.flush();
                }
                cipherOutputStream.flush();
            }
            outputStream.flush();
            UserError.Log.d(TAG, "Data written successfully");
            return true;
        } catch (NullPointerException | IOException e) {
            UserError.Log.e(TAG, "Output IO Error " + e);
        } catch (SecurityException e) {
            UserError.Log.wtf(TAG, "Got weird security error: " + e);
        }
        return false;
    }

    private static boolean isXdripManagedUri(final String uri) {
        return uri != null && uri.startsWith(XDRIP_CONTENT_TYPE);
    }

    private static Pair<String, String> getIdNameFromXdripUri(final String uri) {
        if (emptyString(uri)) return null;
        val parts = uri.split("/");
        if (parts.length != 4) {
            UserError.Log.e(TAG, "Invalid parts length: " + parts.length);
            return null;
        }
        return Pair.create(parts[2], parts[3]);
    }

    private static boolean compressEncryptFilesXdripStream(final BackupStatus status, final String uri, final String... sourcePaths) throws IOException {
        val parts = getIdNameFromXdripUri(uri);
        if (parts == null) {
            UserError.Log.e(TAG, "Could not parse: " + uri);
            status.status("Invalid file uri");
            return false;
        }
        val temp = File.createTempFile("backup" + JoH.tsl(), "dat", xdrip.getAppContext().getCacheDir());
        try {
            status.status(xdrip.gs(R.string.creating_local_backup));
            val fos = new FileOutputStream(temp);
            val writeResult = compressEncryptFilesToOutputStream(fos, sourcePaths);
            fos.close();
            if (!writeResult) {
                UserError.Log.e(TAG, "Failed to write backup to temporary file");
                status.status(xdrip.gs(R.string.error_with_local_backup));
                return false;
            }
            val fis = new FileInputStream(temp);
            try {
                status.status(xdrip.gs(R.string.uploading_to_cloud));
                DriveManager.getInstance().saveFromStreamSync(parts.first, parts.second, fis); // TODO catch null
                status.status(xdrip.gs(R.string.upload_successful));
                return true;
            } catch (NullPointerException e) {
                UserError.Log.e(TAG, "Could not create drive service");
                status.status(xdrip.gs(R.string.error_uploading_to_cloud));
            }
        } finally {
            temp.delete();
        }

        return false;
    }

    private static boolean processXdripStream(final String sourceUri, final BackupMetaData metaData) throws IOException {
        try {
            val id = getIdNameFromXdripUri(sourceUri).first;
            val fileMeta = DriveManager.getInstance().getFileInfo(id);
            if (fileMeta == null) {
                UserError.Log.e(TAG, "Cannot get file info for: " + sourceUri);
                return false;
            }
            metaData.displayName = fileMeta.getName();
            metaData.size = fileMeta.getSize();
            try (val is = DriveManager.getInstance().getInputStream(id, metaData)) {
                return restoreBackFromInputStream(is, metaData);
            }
        } catch (NullPointerException e) {
            UserError.Log.e(TAG, "Null pointer from uri " + e);
            e.printStackTrace();
        }
        return false;
    }

    private static BackupMetaData restoreBackFromDefaultUri(final boolean metaOnly) {
        val metaData = new BackupMetaData();
        metaData.getMetaDataOnly = metaOnly;
        metaData.successResult = restoreBackFromUri(getBackupUri(), metaData);
        return metaData;
    }

    public static BackupMetaData restoreBackFromDefaultUri() {
        return restoreBackFromDefaultUri(false); // do full restore
    }

    public static BackupMetaData getBackupMetaDataDefaultUri() {
        return restoreBackFromDefaultUri(true); // get metadata only
    }


    @SuppressWarnings("UnstableApiUsage")
    public static synchronized boolean restoreBackFromUri(final String sourceUri, BackupMetaData metaData) {
        if (metaData == null) {
            metaData = new BackupMetaData();
        }
        if (!emptyString(sourceUri)) {
            try {
                if (isXdripManagedUri(sourceUri)) {
                    return processXdripStream(sourceUri, metaData);
                } else {
                    val sourceURI = Uri.parse(sourceUri);
                    populateMetaDataAsync(metaData, sourceURI);
                    try (val pfd = xdrip.getAppContext().getContentResolver()
                            .openFileDescriptor(sourceURI, "r")) {
                        try (val inputStream = new FileInputStream(pfd.getFileDescriptor())) {
                            return restoreBackFromInputStream(inputStream, metaData);
                        }
                    }
                }
            } catch (SecurityException e) {
                UserError.Log.e(TAG, "Input IO Security Error " + e);
                metaData.exception = e;
                BackupActivity.notifySecurityError();
            } catch (NullPointerException | IOException e) {
                UserError.Log.e(TAG, "Input IO Error " + e);
                metaData.exception = e;
            }
        } else {
            UserError.Log.d(TAG, "No destination specified");
            return false;
        }
        return false;
    }

    public static boolean restoreBackFromInputStream(final InputStream inputStream, final BackupMetaData metaData) throws IOException {
        metaData.populateFromInputStream(inputStream);
        if (metaData.getMetaDataOnly) {
            UserError.Log.d(TAG, "Got metadata which is all that was requested");
            return true;
        }
        if (metaData.looksOkay()) {
            try (val cipherInputStream = new CipherInputStream(inputStream, getCipher(false, metaData))) {
                try (val gzipInputStream = new GZIPInputStream(cipherInputStream)) {
                    val completedFileList = new ArrayList<String>();
                    while (pullFileFromStreamWithHeader(gzipInputStream, completedFileList)) {
                        UserError.Log.d(TAG, "More data in stream...");
                    }
                    if (completedFileList.size() == 0) {
                        UserError.Log.e(TAG, "Didn't get a single valid element");
                        return false;
                    } else {
                        for (val file : completedFileList) {
                            moveFileToFinalDestination(file);
                        }
                        Inevitable.task("kill all", 1000, SdcardImportExport::hardReset);
                    }
                }
            }
            return true;
        } else {
            UserError.Log.wtf(TAG, "Unrecognized backup type " + metaData.id);
            return false;
        }
    }

    public static void setXdripManagedBackupUri(final String fileId, final String name) {
        if (fileId == null || name == null) {
            UserError.Log.e(TAG, "Cannot set managed backup uri of null");
            return;
        }
        setBackupUri(XDRIP_CONTENT_TYPE + fileId + "/" + name);
    }

    public static void setBackupUri(final String uri) {
        PersistentStore.setString(PREF_BACKUP_URI, uri);
    }

    public static void clearBackupUri() {
        setBackupUri("");
    }

    public static String getBackupUri() {
        return PersistentStore.getString(PREF_BACKUP_URI);
    }

    public static boolean isBackupUriSet() {
        return !emptyString(getBackupUri());
    }

    public static boolean isBackupSuitableForAutomatic() {
        return isBackupUriSet() && isXdripManagedUri(getBackupUri());
    }

    public static void doCompleteBackupIfEnabled() {
        if (Pref.getBooleanDefaultFalse(PREF_AUTO_BACKUP)
                && isBackupSuitableForAutomatic() && ((Pref.getBoolean(PREF_AUTO_BACKUP_MOBILE, true)) || (JoH.isLANConnected()))) {
            UserError.Log.e(TAG, "Attempting automatic backup");
            val success = doCompleteBackup(new LogStatus());
            if (!success) {
                UserError.Log.e(TAG, "Automatic backup failed");
            }
        }
    }

    public static boolean doCompleteBackup(final BackupStatus status) {
        UserError.Log.d(TAG, "doCompleteBackup() called");
        return compressEncryptFilesToUri(status, getBackupUri(), getPreferencesPath(), getDatabasePath());
    }

    public static String cleanPhoneName() {
        String btName = getLocalBluetoothName();
        if (btName.equalsIgnoreCase(Build.MODEL)) {
            btName = "";
        } else {
            btName = "-" + btName;
        }
        return (Build.MODEL + btName)
                .replace(" ", "-")
                .replace("/", "-")
                .replace(":", ".");
    }

    public static String getDefaultFileName() {
        return "xdrip-backup-" + cleanPhoneName() + ".xdrip";
    }

    public static String getDefaultFolderName() {
        return "xDrip-Backups";
    }

    private static String getDatabasePath() {
        val databaseName = new Configuration.Builder(xdrip.getAppContext()).create().getDatabaseName();
        return xdrip.getAppContext().getDatabasePath(databaseName).getPath();
    }

    private static String getPreferencesPath() {
        return xdrip.getAppContext().getFilesDir().getParent() + "/" + PREFERENCES_FILE;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void moveFileToFinalDestination(final String sourcePath) {

        val s = new File(sourcePath);
        UserError.Log.d(TAG, "Processing final destination: " + s.getName());
        switch (s.getName()) {
            case "com.eveningoutpost.dexdrip_preferences.xml":
                moveIt(sourcePath, getPreferencesPath());
                break;
            case "DexDrip.db":
                val dbpath = getDatabasePath();
                moveIt(sourcePath, dbpath);
                for (val suffix : dbSuffix) {
                    new File(dbpath + suffix).delete();
                }
                break;
            default:
                UserError.Log.wtf(TAG, "Don't know how to move to destination for: " + s.getName());
        }
    }

    private static boolean robustMove(final File source, final File dest) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                UserError.Log.d(TAG, "Using new move method " + source + " -> " + dest);
                Files.move(source.toPath(), dest.toPath(), ATOMIC_MOVE);
                return true;
            } else {
                val dr = source.renameTo(dest);
                if (!dr) {
                    UserError.Log.e(TAG, "Failed to move: " + source + " to " + dest);
                    return false;
                } else {
                    return true;
                }
            }
        } catch (UnsupportedOperationException | SecurityException | IOException e) {
            UserError.Log.wtf(TAG, "Got exception trying to move files: " + source + " -> " + dest);
        }
        return false;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean moveIt(final String sourcePath, final String destPath) {
        try {
            UserError.Log.d(TAG, "moveIt called " + sourcePath + " -> " + destPath);
            val b = new File(destPath + ".xdripbak");
            b.delete();     // delete any existing backup
            val d = new File(destPath);
            if (d.exists()) {
                if (robustMove(d, b)) {
                    if (!b.exists()) {
                        UserError.Log.e(TAG, "Backup reportedly made but doesn't exist - skipping");
                        return false;
                    }
                } else {
                    UserError.Log.e(TAG, "Failed to make backup file " + b.getName());
                    return false;
                }
            } else {
                UserError.Log.d(TAG, "Not backing up " + d.getName() + " as it doesn't exist");
            }
            val s = new File(sourcePath);
            val sr = robustMove(s, d);
            if (!sr) {
                UserError.Log.e(TAG, "Failed to move " + s.getName() + " to " + d.getName() + " - restoring backup");
                val br = robustMove(b, d);
                if (!br) {
                    UserError.Log.wtf(TAG, "Failed to restore backup before overwrite!");
                }
                return false;
            }

            UserError.Log.d(TAG, "moveIt success");
            return true;
        } catch (SecurityException e) {
            UserError.Log.wtf(TAG, "Move failed due to security exception: " + e);
        }
        return false;
    }

    private static void populateMetaDataAsync(final BackupMetaData metaData, final Uri sourceURI) {
        JoH.runOnUiThread(() -> {
            try {
                metaData.size = Long.parseLong(getFieldFromURI(SIZE, sourceURI));
            } catch (Exception e) {
                UserError.Log.e(TAG, "Could not get size " + e);
            }
            try {
                metaData.displayName = getFieldFromURI(DISPLAY_NAME, sourceURI);
            } catch (Exception e) {
                UserError.Log.e(TAG, "Could not get display name " + e);
            }
        });
    }

    private static void pushFileToStreamWithHeader(final String sourcePath, final OutputStream outputStream) {
        try {
            val file = new File(sourcePath);
            if (file.exists()) {
                try (val input = new FileInputStream(file)) {
                    UserError.Log.d(TAG, "Saving: " + sourcePath + " (" + file.length() + ")");

                    outputStream.write((file.getName() + "\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.write((file.length() + "\n").getBytes(StandardCharsets.UTF_8));

                    final byte[] buffer = new byte[1024 * 1024];
                    int bytes_read;
                    while ((bytes_read = input.read(buffer)) != -1) {
                        UserError.Log.d(TAG, "Writing buffer bytes: " + bytes_read);
                        outputStream.write(buffer, 0, bytes_read);
                    }
                }
            } else {
                UserError.Log.wtf(TAG, "Cannot find file to backup: " + sourcePath);
            }
        } catch (IOException e) {
            UserError.Log.wtf(TAG, "Error reading input file: " + e);
        }
    }

    private static boolean pullFileFromStreamWithHeader(final InputStream inputStream, final List<String> completedFileList) {
        try {
            val filename = readLine(inputStream);
            if (filename == null) return false;
            val sizes = readLine(inputStream);
            if (sizes == null) return false;
            long size = Long.parseLong(sizes);

            if (size > 1024 * 1024 * 500) {
                UserError.Log.wtf(TAG, "Can't process files > 500MB !");
                return false;
            }

            UserError.Log.d(TAG, "Found file: " + filename + " size: " + sizes);

            val outputDir = xdrip.getAppContext().getFilesDir().getAbsolutePath();
            val outputFile = new File(outputDir, filename);
            UserError.Log.d(TAG, "Writing to: " + outputFile.getAbsolutePath());
            val outputStream = new FileOutputStream(outputFile);

            final byte[] buffer = new byte[1024 * 1024];
            long bytes_total = 0;
            int bytes_read;
            while (((bytes_read = inputStream.read(buffer, 0, (int) Math.min(size - bytes_total, buffer.length))) != -1) && bytes_total < size) {
                bytes_total += bytes_read;
                outputStream.write(buffer, 0, bytes_read);
                // UserError.Log.d(TAG, "Reading buffer bytes: " + bytes_read);
            }

            outputStream.close();

            if (bytes_total == size) {
                UserError.Log.d(TAG, "Correct loading of file size: " + bytes_total);
                completedFileList.add(outputFile.getPath());
                return true;

            } else {
                UserError.Log.wtf(TAG, "Invalid file size for: " + filename + " " + bytes_total + " instead of " + size);
                outputFile.delete();
                return false;
            }

        } catch (NumberFormatException | NullPointerException | IOException e) {
            UserError.Log.wtf(TAG, "Error reading data: " + e);
        }
        return false;
    }


    private static Cipher getCipher(final boolean encrypt, final BackupMetaData metaData) {
        try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            val randomKey = new SecretKeySpec(hexStringToByteArray(metaData.OB1), "AES");
            cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, randomKey, new IvParameterSpec(hexStringToByteArray(metaData.ob2)));
            return cipher;
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            UserError.Log.wtf(TAG, "Error getting cipher: " + e);
            return null;
        }
    }

}



