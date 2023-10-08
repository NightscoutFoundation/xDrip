package com.eveningoutpost.dexdrip.utils;

import android.content.Context;
import android.os.Environment;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.val;

public class FileUtils {

    public static boolean makeSureDirectoryExists(final String dir) {
        final File file = new File(dir);
        return file.exists() || file.mkdirs();
    }

    public static String getExternalDir() {
        final StringBuilder sb = new StringBuilder();
        sb.append(Environment.getExternalStorageDirectory().getAbsolutePath());
        sb.append("/xdrip");

        final String dir = sb.toString();
        return dir;
    }

    public static String combine(final String path1, final String path2) {
        final File file1 = new File(path1);
        final File file2 = new File(file1, path2);
        return file2.getPath();
    }

    public static void writeToFileWithCurrentDate(String TAG, String file, byte[] data) {
        Context context = xdrip.getAppContext();

        String dir = context.getFilesDir().getPath();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateandTime = sdf.format(new Date());

        String fileName = dir + '/' + file + "_" + currentDateandTime + ".dat";
        writeToFile(TAG, fileName, data);
    }

    public static void writeToFile(String TAG, String fileName, byte[] data) {
        UserError.Log.d(TAG, "Writing to file" + fileName);
        try {
            FileOutputStream f = new FileOutputStream(fileName);
            if (data != null) {
                // if no data exists, file will be written with zero length to let the user know what is happening.
                f.write(data);
                f.flush();
            }
            f.close();
        } catch (IOException e) {
            UserError.Log.e(TAG, "Caught exception when trying to write file", e);
        }
    }

    public static byte[] readFromFile(final String TAG, final String fileName) {
        val fl = new File(fileName);
        return readFromFile(TAG, fl);
    }

    public static byte[] readFromFile(final String TAG, final File fl) {
        if (!fl.exists()) {
            UserError.Log.e(TAG, "File does not exist " + fl.getName());
            return null;
        }
        val data = new byte[(int) fl.length()];
        try {
            val f = new FileInputStream(fl);
            int size = f.read(data);
            f.close();
            if (size != data.length) {
                UserError.Log.e(TAG, "File read size mismatch");
                return null;
            }
            return data;
        } catch (IOException e) {
            UserError.Log.e(TAG, "Caught exception when trying to read file " + e);
        }
        return null;
    }


    /**
     * Recursively deletes a directory with all the files and directories inside of it
     *
     * @param dir the directory to be deleted
     * @return success
     */
    public static boolean deleteDirWithFiles(File dir) {
        if (!dir.exists()) return true;
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDirWithFiles(new File(dir, child));
                    if (!success) return false;
                }
            }
        }
        return dir.delete();
    }
}
