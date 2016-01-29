package com.eveningoutpost.dexdrip.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class SdcardImportExport extends Activity {

    private final static String TAG = "jamorham sdcard";

    private final static String PREFERENCES_FILE = "shared_prefs/com.eveningoutpost.dexdrip_preferences.xml";

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdcard_import_export);
    }

    public void savePreferencesToSD(View myview) {

        if (savePreferencesToSD()) {
            toast("Preferences saved in sdcard Downloads");
        } else {
            toast("Couldn't write to sdcard - check permissions?");
        }
    }

    public void loadPreferencesToSD(View myview) {
        if (loadPreferencesFromSD()) {
            toast("Loaded Preferences! - Restarting");
            // shared preferences are cached so we need a hard restart
            android.os.Process.killProcess(android.os.Process.myPid());
        } else {
            toast("Could not load preferences - check permissions or file?");
        }
    }

    public static void storePreferencesFromBytes(byte[] bytes, Context context) {
        if (dataFromBytes(bytes, PREFERENCES_FILE, context)) {
            Log.i(TAG, "Restarting as new preferences loaded from bytes");
            android.os.Process.killProcess(android.os.Process.myPid());
        } else {
            Log.e(TAG, "Failed to write preferences from bytes");
        }
    }

    public void deletePreferencesOnSD(View myview) {
        if (!isExternalStorageWritable()) {
            toast("External storage is not writable");
            return;
        }
        if (deleteFolder(new File(getCustomSDcardpath()), false)) {
            toast("Successfully deleted");
        } else {
            toast("Deletion problem");
        }
    }

    public static byte[] getPreferencesFileAsBytes(Context context) {
        return dataToBytes(PREFERENCES_FILE, context);
    }

    public boolean savePreferencesToSD() {
        if (isExternalStorageWritable()) {
            return dataToSDcopy(PREFERENCES_FILE);
        } else {
            toast("SDcard not writable - cannot save");
            return false;
        }
    }

    public boolean loadPreferencesFromSD() {
        if (isExternalStorageWritable()) {
            return dataFromSDcopy(PREFERENCES_FILE);
        } else {
            toast("SDcard not readable");
            return false;
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private String getCustomSDcardpath() {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS) + "/xDrip-export";
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
