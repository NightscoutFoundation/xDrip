package com.eveningoutpost.dexdrip.cloud.backup;

import androidx.annotation.Nullable;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.Scopes;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import lombok.val;

/**
 * JamOrHam
 * Google Drive Manager
 * Provides various utility methods and manages its own lifecycle
 * Methods must be called off the main thread.
 */

public class DriveManager {

    private static final String TAG = DriveManager.class.getSimpleName();
    private static final String DRIVE_FOLDER_TYPE = "application/vnd.google-apps.folder";
    public static final String BINARY_FILE_TYPE = "application/octet-stream";
    private static DriveManager instance = null;
    private final Drive mDriveService;

    public DriveManager(Drive driveService) {
        mDriveService = driveService;
    }

    public synchronized static void deleteInstance() {
        UserError.Log.d(TAG, "Deleting instance");
        instance = null;
    }

    public synchronized static DriveManager getInstance() {
        if (instance == null) {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(xdrip.getAppContext());
            if (lastAccount == null) {
                UserError.Log.wtf(TAG, "lastAccount is null");
            } else {
                val credential = GoogleAccountCredential.usingOAuth2(
                        xdrip.getAppContext(), Collections.singletonList(Scopes.DRIVE_FILE));
                val account = lastAccount.getAccount();
                if (account == null) {
                    UserError.Log.wtf(TAG, "Account is null!!");
                } else {
                    credential.setSelectedAccount(account);
                    val googleDriveService =
                            new Drive.Builder(
                                    AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(), credential)
                                    .setApplicationName("Nightscout xDrip+")
                                    .build();
                    instance = new DriveManager(googleDriveService);
                }
            }
        }
        return instance;
    }

    public void saveFromStreamSync(final String fileId, final String name, final InputStream inputStream) throws IOException {
        val metadata = new File().setName(name);
        mDriveService.files().update(fileId, metadata, new InputStreamContent(BINARY_FILE_TYPE, inputStream)).execute();
    }

    public InputStream getInputStream(final String fileId, final BackupMetaData metaData) throws IOException {
        val file = mDriveService.files().get(fileId);
        val rangeLimit = 10000;
        if (metaData.getMetaDataOnly && metaData.size > rangeLimit) {
            file.getRequestHeaders().setRange("bytes=0-" + rangeLimit);
        }
        return file.executeMediaAsInputStream();
    }

    public File getFileInfo(final String fileId) throws IOException {
        return mDriveService.files().get(fileId)
                .setFields("*")
                .execute();
    }

    public File findFolderSync(final String name) throws IOException {
        val results = mDriveService.files().list()
                .setSpaces("drive").execute();
        if (results.getIncompleteSearch()) {
            UserError.Log.e(TAG, "Incomplete folder search");
        }
        for (val result : results.getFiles()) {
            if (result.getMimeType().equals(DRIVE_FOLDER_TYPE)
                    && result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    public File findFileSync(final String name) throws IOException {        // TODO order by modified?
        val results = mDriveService.files().list()
                .setOrderBy("modifiedTime desc")
                .setSpaces("drive").execute();
        if (results.getIncompleteSearch()) {
            UserError.Log.wtf(TAG, "Incomplete file search - please report error");
        }
        for (val result : results.getFiles()) {
            if (!result.getMimeType().equals(DRIVE_FOLDER_TYPE)
                    && result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    public File getOrCreateFolderSync(final String name) throws IOException {
        val folder = findFolderSync(name);
        if (folder == null) {
            return createFolderSync(name, null);
        } else {
            return folder;
        }
    }

    public File getOrCreateFileSync(final String path) throws IOException {
        if (JoH.emptyString(path)) {
            UserError.Log.e(TAG, "Empty path in getOrCreate");
            return null;
        }
        val parts = path.split("/");
        if (parts.length != 2) {
            UserError.Log.e(TAG, "Invalid path in getOrCreate: " + path);
            return null;
        }
        val filename = parts[parts.length - 1];
        val existing = findFileSync(filename);
        if (existing != null) {
            return existing;
        } else {
            val folder = parts[parts.length - 2];
            UserError.Log.d(TAG, "Creating: " + filename + " in " + folder);
            val folderResult = getOrCreateFolderSync(folder);
            val metadata = new File()
                    .setParents(Collections.singletonList(folderResult.getId()))
                    .setMimeType(BINARY_FILE_TYPE)
                    .setName(filename);
            return mDriveService.files().create(metadata).execute();
        }
    }

    public File createFolderSync(final String folderName, @Nullable final String parentFolderId) throws IOException {
        final List<String> root = parentFolderId == null ? Collections.singletonList("root")
                : Collections.singletonList(parentFolderId);
        val metadata = new File()
                .setParents(root)
                .setMimeType(DRIVE_FOLDER_TYPE)
                .setName(folderName);
        val file = mDriveService.files().create(metadata).execute();
        if (file == null) {
            throw new IOException("Null result when requesting file creation.");
        }
        return file;
    }

}