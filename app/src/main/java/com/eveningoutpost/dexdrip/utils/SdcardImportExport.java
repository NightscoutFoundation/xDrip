package com.eveningoutpost.dexdrip.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static com.eveningoutpost.dexdrip.utils.FileUtils.getExternalDir;


public class SdcardImportExport extends AppCompatActivity {

    private final static String TAG = "jamorham sdcard";
    private final static int MY_PERMISSIONS_REQUEST_STORAGE = 104;
    private final static String PREFERENCES_FILE = "shared_prefs/" + xdrip.getAppContext().getString(R.string.local_target_package) + "_preferences.xml";
    private static Activity activity;
    public static boolean deleteFolder(File path, boolean recursion) {
        try {
            Log.d(TAG, "deleteFolder called with: " + path.toString());
            if (path.exists()) {
                File[] files = path.listFiles();
                if (files == null) {
                    return true;
                }
                for (File file : files) {
                    if ((recursion) && (file.isDirectory())) {
                        deleteFolder(file, recursion);
                    } else {
                        Log.d(TAG, "Calling delete for file: " + file.getName());
                        file.delete();
                    }
                }
            }
            return (path.delete());
        } catch (Exception e) {
            Log.e(TAG, "Got exception in delete: " + e.toString());
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdcard_import_export);
        JoH.fixActionBar(this);

        // SUPER DATABASE DEBUG COPY FOR NON ROOTED
        //directCopyFile(new File("/data/data/com.eveningoutpost.dexdrip/databases/DexDrip.db"),new File("/sdcard/DexDrip-debug.db"));

    }

    private boolean checkPermissions()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_STORAGE);
                    return false;
            }
        }
        return true;
    }

    public void savePreferencesToSD(View myview) {

        if (savePreferencesToSD()) {
            toast(getString(R.string.preferences_saved_in_sdcard_downloads));
        } else {
            toast(getString(R.string.could_not_write_to_sdcard_check_perms));
        }
    }

    public static void hardReset() {
        // shared preferences are cached so we need a hard restart
        GcmActivity.last_sync_request = 0;
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public static void forceGMSreset() {
        final Context context = xdrip.getAppContext();
        final String gmsfiles = "shared_prefs/com.google.android.gms.measurement.prefs.xml,shared_prefs/com.google.android.gms.appid.xml,databases/google_app_measurement.db,databases/google_app_measurement.db-journal";
        final String[] filenames = gmsfiles.split(",");
        for (String filename : filenames) {
            if (deleteFolder(new File(context.getFilesDir().getParent() + "/" + filename), false)) {
                Log.d(TAG, "Successfully deleted: " + filename);
            } else {
                Log.e(TAG, "Error deleting: " + filename);
            }
        }
        hardReset();
    }

    public void loadPreferencesToSD(View myview) {
        if (loadPreferencesFromSD()) {
            toast(getString(R.string.loaded_preferences_restarting));
            hardReset();
        } else {
            toast(getString(R.string.could_not_load_preferences_check_pers));
        }
    }

    public static void storePreferencesFromBytes(byte[] bytes, Context context) {
        if (dataFromBytes(bytes, PREFERENCES_FILE, context)) {
            Log.i(TAG, "Restarting as new preferences loaded from bytes");
            hardReset();
        } else {
            Log.e(TAG, "Failed to write preferences from bytes");
        }
    }

    public void deletePreferencesOnSD(View myview) {
        if (!isExternalStorageWritable()) {
            toast(getString(R.string.external_storage_not_writable));
            return;
        }
        if (deleteFolder(new File(getCustomSDcardpath()), false)) {
            toast(getString(R.string.successfully_deleted));
        } else {
            toast(getString(R.string.deletion_problem));
        }
    }

    public static byte[] getPreferencesFileAsBytes(Context context) {
        return dataToBytes(PREFERENCES_FILE, context);
    }

    public boolean savePreferencesToSD() {
        if (isExternalStorageWritable()) {
            boolean succeeded = AlertType.toSettings(getApplicationContext());
            if (succeeded) {
                succeeded &= dataToSDcopy(PREFERENCES_FILE);
            }
            Home.setPreferencesString("saved_alerts","");
            return succeeded;
        } else {
            toast(getString(R.string.sdcard_not_writable_cannot_save));
            return false;
        }
    }

    public boolean loadPreferencesFromSD() {
        if (isExternalStorageWritable()) {
            return dataFromSDcopy(PREFERENCES_FILE);
        } else {
            toast(getString(R.string.sdcard_not_readable));
            return false;
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        checkPermissions();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private String getCustomSDcardpath() {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS) + "/xDrip-export";
    }

    private String getxDripCustomSDcardpath() {
        return getExternalDir() + "/settingsExport";
    }

    private static byte[] dataToBytes(String filename, Context context) {
        File source_file = new File(context.getFilesDir().getParent() + "/" + filename);
        try {
            return directReadFile(source_file);
        } catch (Exception e) {
            Log.e(TAG, "Got exception in datatoBytes: " + e.toString());
            return null;
        }
    }

    private boolean dataToSDcopy(String filename) {
        File source_file = new File(getFilesDir().getParent() + "/" + filename);
        String path = getCustomSDcardpath();
        File fpath = new File(path);
        try {
            fpath.mkdirs();
            File dest_file = new File(path, source_file.getName());

            if (directCopyFile(source_file, dest_file)) {
                Log.i(TAG, "Copied success: " + filename);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error making directory: " + path.toString());
            return false;
        }
        return false;
    }

    private boolean dataFromSDcopy(String filename) {
        File dest_file = new File(getFilesDir().getParent() + "/" + filename);
        File source_file = new File(getCustomSDcardpath() + "/" + dest_file.getName());
        File source_file_xdrip = new File(getxDripCustomSDcardpath() + "/" + dest_file.getName());

        if (source_file.exists() && source_file_xdrip.exists())
        {
            toast(getString(R.string.warning_settings_from_xdrip_and_plus_exist));
        } else {
            if (source_file_xdrip.exists())
            {
                source_file = source_file_xdrip;
                toast(getString(R.string.loading_settings_from_xdrip_mainline));
            }
        }
        try {
            dest_file.mkdirs();
            if (directCopyFile(source_file, dest_file)) {
                Log.i(TAG, "Copied success: " + filename);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error making directory: " + dest_file.toString());
            return false;
        }
        return false;
    }

    private boolean directCopyFile(File source_filename, File dest_filename) {
        Log.i(TAG, "Attempt to copy: " + source_filename.toString() + " to " + dest_filename.toString());
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(source_filename);
            out = new FileOutputStream(dest_filename);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return false;
    }

    private static byte[] directReadFile(File source_filename) {
        Log.i(TAG, "Attempt to read: " + source_filename.toString());
        InputStream in;
        try {
            in = new FileInputStream(source_filename);
            byte[] buffer = new byte[(int) source_filename.length()];
            in.read(buffer);
            in.close();
            Log.d(TAG, "Read file size: " + buffer.length);
            return buffer;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    private static boolean dataFromBytes(byte[] bytes, String filename, Context context) {
        if ((bytes == null) || (bytes.length == 0)) {
            Log.e(TAG, "Got zero bytes in datafrom bytes");
            return false;
        }
        File dest_file = new File(context.getFilesDir().getParent() + "/" + filename);
        try {
            // dest_file.mkdirs();
            FileOutputStream out = new FileOutputStream(dest_file);
            out.write(bytes);
            out.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error writing file: " + dest_file.toString() + " " + e.toString());
            return false;
        }
    }


    private void toast(String msg) {
        try {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Toast msg: " + msg);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't display toast: " + msg);
        }
    }

    public void closeButton(View myview) {
        finish();
    }

}
