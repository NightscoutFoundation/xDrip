package com.eveningoutpost.dexdrip;

/**
 * Created by jamorham on 08/01/16.
 */

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.google.android.gms.common.api.GoogleApiClient;

//import com.google.android.gms.drive.Drive;
//import com.google.android.gms.drive.DriveApi.DriveContentsResult;
//import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
//import com.google.android.gms.drive.DriveContents;
//import com.google.android.gms.drive.DriveFile;
//import com.google.android.gms.drive.DriveFolder;
//import com.google.android.gms.drive.DriveId;
//import com.google.android.gms.drive.DriveResource;
//import com.google.android.gms.drive.ExecutionOptions;
//import com.google.android.gms.drive.Metadata;
//import com.google.android.gms.drive.MetadataBuffer;
//import com.google.android.gms.drive.MetadataChangeSet;
//import com.google.android.gms.drive.events.ChangeEvent;
//import com.google.android.gms.drive.events.ChangeListener;
//import com.google.android.gms.drive.query.Filters;
//import com.google.android.gms.drive.query.Query;
//import com.google.android.gms.drive.query.SearchableField;


//public class GoogleDriveInterface extends Activity implements ConnectionCallbacks,
//        OnConnectionFailedListener {
public class GoogleDriveInterface extends FauxActivity {


    private static final String TAG = "jamorham drive";

    public static boolean isRunning = false;
    private static GoogleApiClient mGoogleApiClient;
    //   private static DriveId ourFolderID = null;
    private static String ourFolderResourceID = null;
    private static String ourFolderResourceIDHash = null;
    private static String ourFolderResourceKeyHash = null;
    private static SharedPreferences prefs;

    /*
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;
    public static boolean staticGetFolderFileList = false;
    public static final Charset my_charset = Charset.forName("ISO-8859-1");

   private final String my_folder_name = "jamorham-xDrip+sync";
   private final boolean use_app_folder = true;
   private final double max_sync_file_age = 1000 * 60 * 60 * 24;


      final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
              ResultCallback<DriveFolder.DriveFileResult>() {
                  @Override
                  public void onResult(DriveFolder.DriveFileResult result) {
                      if (!result.getStatus().isSuccess()) {
                          showMessage("Error while trying to create the file");
                          return;
                      }
                      Log.d(TAG, "Created a file in App Folder: "
                              + result.getDriveFile().getDriveId());
                  }
              };
      final private ResultCallback<DriveResource.MetadataResult> metadataRetrievedCallback = new
              ResultCallback<DriveResource.MetadataResult>() {
                  @Override
                  public void onResult(DriveResource.MetadataResult result) {
                      if (!result.getStatus().isSuccess()) {
                          Log.v(TAG, "Problem while trying to fetch metadata.");
                          return;
                      }

                      Metadata metadata = result.getMetadata();
                      if (metadata.isTrashed()) {
                          Log.v(TAG, "Folder is trashed");
                      } else {
                          Log.v(TAG, "Folder is not trashed");
                      }

                  }
              };
      /**
       * Callback when call to trash or untrash is complete.
       */
    /*
    private final ResultCallback<Status> trashStatusCallback =
            new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (!status.isSuccess()) {
                        Log.e(TAG, "Unable to delete file: " +
                                status.getStatusMessage());
                        return;
                    } else {
                        Log.d(TAG, "Trash successful");
                    }
                }
            };
    /**
     * A listener to handle file change events.
     */
    /*
    final private ChangeListener changeListener = new ChangeListener() {
        @Override
        public void onChange(ChangeEvent event) {
            Log.i(TAG, String.format("File change event: %s", event));
        }
    };
    private boolean isSubscribed = false;
    private List<Metadata> remoteFiles = new ArrayList<Metadata>();
    private List<Treatments> localTreatments = new ArrayList<Treatments>();
    private List<Treatments> localTreatmentsBigger = new ArrayList<Treatments>();
    final private ResultCallback<MetadataBufferResult> fileListCallback = new
            ResultCallback<MetadataBufferResult>() {
                @Override
                public void onResult(MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Problem while retrieving filelist");
                        return;
                    }

                    try {
                        Log.i(TAG, "Looking at file metadata query result");
                        MetadataBuffer metabuffer = null;
                        try {
                            metabuffer = result.getMetadataBuffer();

                            if (metabuffer != null) remoteFiles.clear(); // reset
                            if (metabuffer != null) for (Metadata record : metabuffer) {
                                if (record == null || !record.isDataValid()) continue;
                                if (record.isTrashed()) continue;

                                remoteFiles.add(record);

                                Log.i(TAG, "First file metadata: " + record.getTitle());
                            }
                        } finally {
                            //    if (metabuffer != null) metabuffer.close(); // should we close and when?
                        }

                        Log.i(TAG, "endfile  query result");
                        new Thread() {
                            @Override
                            public void run() {
                                evaluateTreatmentsSync();
                            }
                        }.start();

                    } catch (Exception e) {
                        Log.e(TAG, "Got exception in file metadata: " + e.toString());
                    }
                }
            };
    // files!
    // Callback when requested subscribe returns.
    private ResultCallback<Status> subscribeCallBack = new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
            if (!status.isSuccess()) {
                Log.e(TAG, "Unable to subscribe." + status.getStatusMessage());
            } else {
                Log.i(TAG, "Subscribe result ok");
            }
        }
    };
    final ResultCallback<DriveFolder.DriveFolderResult> ourFolderCreateCallback = new ResultCallback<DriveFolder.DriveFolderResult>() {
        @Override
        public void onResult(DriveFolder.DriveFolderResult result) {
            if (!result.getStatus().isSuccess()) {
                showMessage("Error while trying to create the folder");
                return;
            }
            showMessage("Created a folder: " + result.getDriveFolder().getDriveId());
            ourFolderID = result.getDriveFolder().getDriveId();
            ourFolderResourceID = ourFolderID.getResourceId();
            PlusSyncService.speedup();
            getFolderFileList(ourFolderID);

        }
    };
    // THIS IS FOR FOLDER
    final private ResultCallback<MetadataBufferResult> folderSearchMetadataCallback = new
            ResultCallback<MetadataBufferResult>() {
                @Override
                public void onResult(MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Problem while retrieving results");
                        return;
                    }
                    try {
                        Log.i(TAG, "Looking at metadata query result");
                        MetadataBuffer metabuffer = null;
                        try {
                            metabuffer = result.getMetadataBuffer();

                            if (metabuffer != null) for (Metadata record : metabuffer) {
                                Log.i(TAG, "metdata business");
                                if (record == null || !record.isDataValid()) continue;
                                if (record.isTrashed()) continue;

                                ourFolderID = record.getDriveId();
                                ourFolderResourceID = ourFolderID.getResourceId();
                                PlusSyncService.speedup();
                                Log.i(TAG, "First metadata: " + record.getTitle());
                            }
                        } finally {
                            if (metabuffer != null) metabuffer.close();
                        }

                        Log.i(TAG, "end query result");

                        if (ourFolderID == null) {
                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setTitle(my_folder_name).build();
                            Drive.DriveApi.getRootFolder(getGoogleApiClient()).createFolder(
                                    getGoogleApiClient(), changeSet).setResultCallback(ourFolderCreateCallback);
                        } else {

                            Log.i(TAG, "Found our folder: " + ourFolderID.toString());
                            getFolderFileList(ourFolderID);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Got exception in metadata: " + e.toString());
                    }
                }
            };
    // Callback when requested sync returns.
    private ResultCallback<Status> syncCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
            if (!status.isSuccess()) {
                Log.e(TAG, "Unable to sync.");
            } else {
                Log.i(TAG, "Sync reports ok");
            }
            if (use_app_folder) {
                ourFolderID = Drive.DriveApi.getAppFolder(getGoogleApiClient()).getDriveId();
                ourFolderResourceID = ourFolderID.getResourceId();
                PlusSyncService.speedup();
                if (ourFolderID != null) {
                    getFolderFileList(ourFolderID);
                } else {
                    Log.e(TAG, "Could not get app_folder identity");
                }
            } else {

                new Thread() {
                    @Override
                    public void run() {
                        Query query = new Query.Builder()
                                .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"))
                                .addFilter(Filters.eq(SearchableField.TITLE, my_folder_name))
                                .build();
                        Drive.DriveApi.query(getGoogleApiClient(), query)
                                .setResultCallback(folderSearchMetadataCallback);

                    }
                }.start();
            }
        }
    };
*/
    public static boolean keyInitialized() {
        if (getDriveIdentityString() != null) return true;
        return false;
    }

    private static String getCustomSyncKey() {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        //if ((prefs != null) && (prefs.getBoolean("use_custom_sync_key", true))) {
        if ((prefs != null) && (true)) {
            if (prefs.getString("custom_sync_key", "").equals("")) {
                prefs.edit().putString("custom_sync_key", CipherUtils.getRandomHexKey()).commit();
            }
            final String mykey = prefs.getString("custom_sync_key", "");
            if ((mykey.length() > 16)) {
                return mykey;
            } else {
                Home.toaststaticnext("Custom sync key is too short - make it >16 characters");
            }
        }
        return null;
    }

    public static void invalidate() {
        // ourFolderID = null;
        ourFolderResourceID = null;
        ourFolderResourceIDHash = null;
        ourFolderResourceKeyHash = null;
    }

    public static String getDriveIdentityString() {

        // we should cache and detect preference change and invalidate a flag for optimization
        String customkey = getCustomSyncKey();
        if (customkey != null) {
            ourFolderResourceIDHash = CipherUtils.getSHA256(customkey).substring(0, 32);
            return ourFolderResourceIDHash;
        }

        //   if (ourFolderID == null) {
        //      return null;
        //  }
        if (ourFolderResourceID == null) {
            return null;
        }
        if (ourFolderResourceIDHash == null) {
            Log.d(TAG, "Using ResourceID String: " + ourFolderResourceID);
            ourFolderResourceIDHash = CipherUtils.getSHA256(ourFolderResourceID);
        }
        return ourFolderResourceIDHash;
    }

    public static String getDriveKeyString() {

        // we should cache and detect preference change and invalidate a flag for optimization
        final String customkey = getCustomSyncKey();
        if (customkey != null) {
            ourFolderResourceKeyHash = CipherUtils.getMD5(customkey);
            return ourFolderResourceKeyHash;
        }

        //  if (ourFolderID == null) {
        //      return "";
        //  }
        if (ourFolderResourceID == null) {
            UserError.Log.wtf(TAG, "Invalid null sync key!!!");
            return CipherUtils.getRandomHexKey();
        }
        if (ourFolderResourceKeyHash == null) {
            Log.d(TAG, "Using Key ResourceID String: " + ourFolderResourceID);
            ourFolderResourceKeyHash = CipherUtils.getMD5(ourFolderResourceID);
        }
        return ourFolderResourceKeyHash;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        startup();
    }

    /**
     * Create a new file and save it
     */
  /*  private void saveFileToDrive(final String filename, final byte[] source) {

        Log.i(TAG, "Creating new contents.");

        if (source.length == 0) {
            Log.e(TAG, "Not writing zero byte file to gdrive");
            return;
        }

        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveContentsResult>() {

                    @Override
                    public void onResult(DriveContentsResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }

                        final DriveContents driveContents = result.getDriveContents();
                        new Thread() {
                            @Override
                            public void run() {
                                Log.d(TAG, "Starting output stream.  size: " + source.length);
                                OutputStream outputStream = driveContents.getOutputStream();
                                try {
                                    outputStream.write(source);
                                    outputStream.close();
                                } catch (IOException e) {
                                    Log.e(TAG, e.getMessage());
                                }

                                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                        .setTitle(filename)
                                        .setMimeType("application/octet-stream")
                                        .setStarred(true).build();

                                DriveFolder folder = ourFolderID.asDriveFolder();
                                folder.createFile(getGoogleApiClient(), changeSet, driveContents, new ExecutionOptions.Builder()
                                        .setNotifyOnCompletion(true)
                                        .build())
                                        .setResultCallback(fileCallback);
                                Log.d(TAG, "Sent create file req");
                            }
                        }.start();

                    }


                });
    }*/
    private void startup() {
        isRunning = true;
        // if (!prefs.getBoolean("use_custom_sync_key", true)) {
        if (false) {
            //      connectGoogleAPI();
        } else {
            Log.d(TAG, "Using custom sync key");
            shutdown();
        }
    }

    private void shutdown() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
            Log.i(TAG, "DISCONNECTED GOOGLE DRIVE API");
        } else {
            Log.i(TAG, "No drive instance to shutdown");
        }
        mGoogleApiClient = null;
        isRunning = false;
        try {
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Got exception doing finish in shutdown");
        }
    }
/*
    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CREATOR:
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "successfully saved.");
                } else {
                    Log.i(TAG, "Bad result code: " + resultCode);
                }
                break;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        PlusSyncService.backoff();
        if (!result.hasResolution()) {
            // show the localized error dialog.
            try {
                GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            } catch (Exception e) {
                Log.e(TAG, "Ouch could not display the error dialog from play services: " + e.toString());
            }
            return;
        }
        try {
            if (result.getErrorCode() == ConnectionResult.SIGN_IN_REQUIRED) {
                PlusSyncService.backoff_a_lot();
            }
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
        shutdown();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "API client connected. starting query thread");
        Drive.DriveApi.requestSync(getGoogleApiClient()).setResultCallback(syncCallback);
        PlusSyncService.speedup();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    void getFolderFileList(DriveId driveId) {
        if (!staticGetFolderFileList) {
            try {
                Log.d(TAG, "Calling shutdown");
                shutdown();
            } catch (Exception e) {
            }
            return;
        }
        Log.d(TAG, "Asking drive file list");
        Query query = new Query.Builder()
                .build();
        DriveFolder folder = driveId.asDriveFolder();

        if (!isSubscribed) {
            Log.d(TAG, "Starting to listen to the file changes.");
            folder.addChangeListener(getGoogleApiClient(), changeListener).setResultCallback(subscribeCallBack);
            folder.addChangeSubscription(getGoogleApiClient());
            //  isSubscribed = true;
        } else {
            Log.d(TAG, "Already listening to the file changes.");
        }
        folder.queryChildren(getGoogleApiClient(), query)
                .setResultCallback(fileListCallback);
    }

    protected byte[] getDriveFile(DriveId driveid, long bytesize) {

        byte[] data = new byte[(int) bytesize];
        DriveFile file = driveid.asDriveFile();
        DriveContentsResult driveContentsResult =
                file.open(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null).await();
        if (!driveContentsResult.getStatus().isSuccess()) {
            Log.d(TAG, "Could not get drive file");
            return null;
        }
        DriveContents driveContents = driveContentsResult.getDriveContents();
        InputStream reader = driveContents.getInputStream();

        try {
            int readbytes = reader.read(data);
            Log.i(TAG, "Read bytes from file: " + readbytes + "/" + data.length + " vs total: " + bytesize);
        } catch (IOException e) {
            Log.e(TAG, "IOException while reading from the stream", e);
        }
        driveContents.discard(getGoogleApiClient());
        return data;
    }

    boolean evaluateTreatmentsSync() {
        boolean modified = false;
        Log.i(TAG, "Evaluating treatments for sync");
        double timeNow = new Date().getTime();
        double earliestTime = timeNow - (25 * 60 * 60 * 1000); // look back 25 hours
        double evenearliestTime = timeNow - (72 * 60 * 60 * 1000); // look back 25 hours
        localTreatments = Treatments.latestForGraph(1000, earliestTime);
        localTreatmentsBigger = Treatments.latestForGraph(1000, evenearliestTime);

        for (Treatments thistreatment : localTreatments) {
            if (!doesRemoteFileExist(thistreatment.uuid)) {
                Log.d(TAG, "Remote treatment: " + thistreatment.uuid + " does not exist yet");
                pushTreatmentToRemote(thistreatment);
                // create it!
            } else {
                Log.d(TAG, "Remote treatment: " + thistreatment.uuid + " already exists");
            }
        }
        double timenow = new Date().getTime();
        // does the remote end have anything we don't have
        for (Metadata record : remoteFiles) {
            String thistitle = record.getTitle();
            // TODO: is it a deletion? - handle separately with inversed logic

            if ((timenow - record.getCreatedDate().getTime()) > (max_sync_file_age)) {
                if (record.isTrashable()) {
                    Log.i(TAG, "Attempting to delete old data: " + record.getTitle());
                    DriveResource driveResource = record.getDriveId().asDriveResource();
                    com.google.android.gms.common.api.Status deleteStatus =
                            driveResource.delete(mGoogleApiClient).await();
                    if (!deleteStatus.isSuccess()) {
                        Log.e(TAG, "Unable to delete app data: " + deleteStatus.getStatus().getStatusMessage());
                    } else {
                        Log.i(TAG, "Successfully deleted: " + record.getTitle());
                    }
                } else {
                    showMessage("Resource is not owned by the user or is in the AppFolder." + record.getTitle());
                }
            } else {
                // check length = 36 which would be standard uuid length
                if ((thistitle.length() == 36) && !doesTreatmentUuidExist(thistitle)) {
                    Log.d(TAG, "Local treatment: " + thistitle + " does not exist yet");
                    pullTreatmentFromRemote(record.getDriveId(), record.getFileSize());
                    modified = true;
                } else {
                    Log.d(TAG, "Local treatment: " + thistitle + " already exists or is invalid uuid length");
                }
            }
        }
        try {
            finish();
        } catch (Exception e) {
        }
        return modified;
    }

    void pullTreatmentFromRemote(DriveId remoteid, long bytesize) {
        byte[] data = getDriveFile(remoteid, bytesize);
        byte[] plain = CipherUtils.decryptBytes(data);
        String json = new String(plain, my_charset);
        Log.d(TAG, "json downloaded: " + json);
        Treatments.pushTreatmentFromJson(json);
    }

    void pushTreatmentToRemote(Treatments thistreatment) {
        String json = thistreatment.toJSON();
        byte[] plain = json.getBytes(my_charset);
        Log.d(TAG, "json prepared: " + json);
        byte[] crypted = CipherUtils.encryptBytes(plain);
        saveFileToDrive(thistreatment.uuid, crypted);
    }

    public void deleteTreatmentAtRemote(String uuid) {
        byte[] bogus = new byte[0];
        Log.d(TAG, "Sending deletion marker to drive sync for uuid: " + uuid);
        saveFileToDrive(uuid + ".deleted", bogus);
    }

    boolean doesRemoteFileExist(String filename) {
        // will this get significant size list that we should do a hashmap lookup for the filename
        // instead of iterating - total size should be number of treatments in a 24 hour period but
        // we may want to reuse code to sync larger datasets
        for (Metadata record : remoteFiles) {
            if (record.getTitle().contentEquals(filename) && (record.getFileSize() > 0)) {
                return true;
            }
        }
        return false;
    }

    boolean doesTreatmentUuidExist(String uuid) {
        // Log.d(TAG,"Debug doestreatmentuuidexist: start: "+uuid);
        // will this get significant size list that we should do a hashmap lookup for the filename
        // instead of iterating - total size should be number of treatments in a 24 hour period but
        // we may want to reuse code to sync larger datasets
        for (Treatments t : localTreatmentsBigger) {
            if (t.uuid.contentEquals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    public void connectGoogleAPI() {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnecting()) {
            // Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection
            // failures.
            // Since no account name is passed, the user is prompted to choose.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                            // .useDefaultAccount()
                    .build();
        }
        mGoogleApiClient.connect();
    }
*/
}



