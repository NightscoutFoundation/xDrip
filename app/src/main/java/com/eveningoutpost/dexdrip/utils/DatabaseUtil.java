package com.eveningoutpost.dexdrip.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.activeandroid.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import static com.eveningoutpost.dexdrip.utils.FileUtils.*;

/**
 * Save the SQL database to file.
 */
public class DatabaseUtil {

    public static String saveSql(Context context) {
        try {

            final String databaseName = new Configuration.Builder(context).create().getDatabaseName();

            final String dir = getExternalDir();
            makeSureDirectoryExists(dir);

            final StringBuilder sb = new StringBuilder();
            sb.append(dir);
            sb.append("/export");
            sb.append(DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()));
            sb.append(".sqlite");

            final String filename = sb.toString();
            final File sd = Environment.getExternalStorageDirectory();
            if (sd.canWrite()) {
                final File currentDB = context.getDatabasePath(databaseName);
                final File backupDB = new File(filename);
                if (currentDB.exists()) {
                    final FileInputStream srcStream = new FileInputStream(currentDB);
                    final FileChannel src = srcStream.getChannel();
                    final FileOutputStream destStream = new FileOutputStream(backupDB);
                    final FileChannel dst = destStream.getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    srcStream.close();
                    dst.close();
                    destStream.close();
                } else {
                    Toast.makeText(context, "Problem: No current DB found!", Toast.LENGTH_LONG);
                    Log.d("DatabaseUtil",  "Problem: No current DB found");
                }
            } else {
                Toast.makeText(context, "SD card not writable!", Toast.LENGTH_LONG);
                Log.d("DatabaseUtil",  "SD card not writable!");
            }

            return filename;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
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
            if (!replacement.exists()){
                Log.d("DatabaseUtil", "File does not exist: " + path);
                return;
            }
            if (currentDB.canWrite()) {
                srcStream = new FileInputStream(replacement);
                src = srcStream.getChannel();
                destStream = new FileOutputStream(currentDB);
                dst = destStream.getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                srcStream.close();
                dst.close();
                destStream.close();
            } else {
                Log.v("DatabaseUtil", "loadSql: No Write access");
            }
        } catch (Exception e) {
            Log.e("DatabaseUtil", "Something went wrong importing Database", e);

        } finally {
            if(src != null) try {
                src.close();
            } catch (IOException e1) {
                Log.e("DatabaseUtil", "Something went wrong closing: ", e1);
            }
            if(destStream != null) try {
                destStream.close();
            } catch (IOException e1) {
                Log.e("DatabaseUtil", "Something went wrong closing: ", e1);
            }
            if(srcStream != null) try {
                srcStream.close();
            } catch (IOException e1) {
                Log.e("DatabaseUtil", "Something went wrong closing: ", e1);
            }
            if(dst != null) try {
                dst.close();
            } catch (IOException e1) {
                Log.e("DatabaseUtil", "Something went wrong closing: ", e1);

            }
        }
    }
}
