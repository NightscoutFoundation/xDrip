package com.eveningoutpost.dexdrip.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.activeandroid.Configuration;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.eveningoutpost.dexdrip.utils.FileUtils.*;

/**
 * Save the SQL database to file.
 */
public class DatabaseUtil {

    public static final String TAG = DatabaseUtil.class.getSimpleName();
    public static final int BUFFER_SIZE =  2048;


    public static String saveSql(Context context) {

        FileInputStream srcStream = null;
        BufferedInputStream biStream = null;
        FileOutputStream foStream = null;
        ZipOutputStream zipOutputStream =null;
        String zipFilename = null;


        try {

            final String databaseName = new Configuration.Builder(context).create().getDatabaseName();

            final String dir = getExternalDir();
            makeSureDirectoryExists(dir);

            final StringBuilder sb = new StringBuilder();
            sb.append(dir);
            sb.append("/export");
            sb.append(DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()));
            sb.append(".zip");
            zipFilename = sb.toString();
            final File sd = Environment.getExternalStorageDirectory();
            if (sd.canWrite()) {
                final File currentDB = context.getDatabasePath(databaseName);
                final File zipOutputFile = new File(zipFilename);
                if (currentDB.exists()) {
                    srcStream = new FileInputStream(currentDB);
                    biStream =  new BufferedInputStream(srcStream, BUFFER_SIZE);

                    foStream = new FileOutputStream(zipOutputFile);
                    zipOutputStream = new ZipOutputStream(new BufferedOutputStream(foStream));
                    zipOutputStream.putNextEntry(new ZipEntry("export" + DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()) + ".sqlite"));

                    byte buffer[] = new byte[BUFFER_SIZE];
                    int count;
                    while ((count = biStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
                        zipOutputStream.write(buffer, 0, count);
                    }
                } else {
                    Toast.makeText(context, "Problem: No current DB found!", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Problem: No current DB found");
                }
            } else {
                Toast.makeText(context, "SD card not writable!", Toast.LENGTH_LONG).show();
                Log.d(TAG, "SD card not writable!");
            }

        } catch (IOException e) {
            Toast.makeText(context, "SD card not writable!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Exception while writing DB", e);
        } finally {
            if (biStream != null) try {
                biStream.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
            if (zipOutputStream != null) try {
                zipOutputStream.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
        }
        return zipFilename;
    }


    public static String saveSqlUnzipped(Context context) {

        FileInputStream srcStream = null;
        FileChannel src = null;
        FileOutputStream destStream = null;
        FileChannel dst = null;
        String filename = null;

        try {

            final String databaseName = new Configuration.Builder(context).create().getDatabaseName();

            final String dir = getExternalDir();
            makeSureDirectoryExists(dir);

            final StringBuilder sb = new StringBuilder();
            sb.append(dir);
            sb.append("/export");
            sb.append(DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()));
            sb.append(".sqlite");

            filename = sb.toString();
            final File sd = Environment.getExternalStorageDirectory();
            if (sd.canWrite()) {
                final File currentDB = context.getDatabasePath(databaseName);
                final File backupDB = new File(filename);
                if (currentDB.exists()) {
                    srcStream = new FileInputStream(currentDB);
                    src = srcStream.getChannel();
                    destStream = new FileOutputStream(backupDB);
                    dst = destStream.getChannel();
                    dst.transferFrom(src, 0, src.size());
                } else {
                    Toast.makeText(context, "Problem: No current DB found!", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Problem: No current DB found");
                }
            } else {
                Toast.makeText(context, "SD card not writable!", Toast.LENGTH_LONG).show();
                Log.d(TAG, "SD card not writable!");
            }

        } catch (IOException e) {
            Toast.makeText(context, "SD card not writable!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Exception while writing DB", e);
        } finally {
            if (src != null) try {
                src.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
            if (destStream != null) try {
                destStream.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
            if (srcStream != null) try {
                srcStream.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
            if (dst != null) try {
                dst.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
        }
        return filename;
    }

    public static void loadSql(Context context, String path) {

        FileInputStream srcStream = null;
        FileChannel src = null;
        FileOutputStream destStream = null;
        FileChannel dst = null;

        try {
            String databaseName = new Configuration.Builder(context).create().getDatabaseName();
            File currentDB = context.getDatabasePath(databaseName);
            File replacement = new File(path);
            if (!replacement.exists()) {
                Log.d(TAG, "File does not exist: " + path);
                return;
            }
            if (currentDB.canWrite()) {
                srcStream = new FileInputStream(replacement);
                src = srcStream.getChannel();
                destStream = new FileOutputStream(currentDB);
                dst = destStream.getChannel();
                dst.transferFrom(src, 0, src.size());
            } else {
                Log.v(TAG, "loadSql: No Write access");
            }
        } catch (IOException e) {
            Log.e(TAG, "Something went wrong importing Database", e);

        } finally {
            if (src != null) try {
                src.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
            if (destStream != null) try {
                destStream.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
            if (srcStream != null) try {
                srcStream.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
            if (dst != null) try {
                dst.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);

            }
        }
    }
}
