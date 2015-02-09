package com.eveningoutpost.dexdrip.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateFormat;

import com.activeandroid.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
                }
            }

            return filename;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadSql(Context context, Uri uri) {
        try {
            final String databaseName = new Configuration.Builder(context).create().getDatabaseName();

            final File currentDB = context.getDatabasePath(databaseName);
            final File replacement = new File(uri.getPath());
            if (currentDB.canWrite()) {
                final FileInputStream srcStream = new FileInputStream(replacement);
                final FileChannel src = srcStream.getChannel();
                final FileOutputStream destStream = new FileOutputStream(currentDB);
                final FileChannel dst = destStream.getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                srcStream.close();
                dst.close();
                destStream.close();
            } else {
                throw new RuntimeException("Couldn't write to " + currentDB);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
