package com.eveningoutpost.dexdrip.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import android.widget.Toast;

import com.activeandroid.Cache;
import com.activeandroid.Configuration;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.eveningoutpost.dexdrip.utils.FileUtils.*;

/**
 * Save the SQL database to file.
 */
public class DatabaseUtil {

    private static final String TAG = DatabaseUtil.class.getSimpleName();
    private static final int BUFFER_SIZE =  2048;

    private static void toastText(final Context context, final String text) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();
            }
        });
    }

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
                    toastText(context, "Problem: No current DB found!");
                    Log.d(TAG, "Problem: No current DB found");
                }
            } else {
                toastText(context, "SD card not writable!");
                Log.d(TAG, "SD card not writable!");
                zipFilename = null;
            }

        } catch (IOException e) {
            toastText(context, "SD card not writable!");
            Log.e(TAG, "Exception while writing DB", e);
            zipFilename = null;
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
                    toastText(context, "Problem: No current DB found!");
                    Log.d(TAG, "Problem: No current DB found");
                }
            } else {
                toastText(context, "SD card not writable!");
                Log.d(TAG, "SD card not writable!");
            }

        } catch (IOException e) {
            toastText(context, "SD card not writable!");
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


    /**
     * Generate a csv that can be imported by SiDiary
     * */
    public static String saveCSV(Context context) {

        FileOutputStream foStream = null;
        PrintStream printStream = null;
        ZipOutputStream zipOutputStream =null;
        String zipFilename = null;


        try {

            final String databaseName = new Configuration.Builder(context).create().getDatabaseName();

            final String dir = getExternalDir();
            makeSureDirectoryExists(dir);

            final StringBuilder sb = new StringBuilder();
            sb.append(dir);
            sb.append("/exportCSV");
            sb.append(DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()));
            sb.append(".zip");
            zipFilename = sb.toString();
            final File sd = Environment.getExternalStorageDirectory();
            if (sd.canWrite()) {
                final File zipOutputFile = new File(zipFilename);

                foStream = new FileOutputStream(zipOutputFile);
                zipOutputStream = new ZipOutputStream(new BufferedOutputStream(foStream));
                zipOutputStream.putNextEntry(new ZipEntry("export" + DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()) + ".csv"));
                printStream = new PrintStream(zipOutputStream);

                printStream.println("DAY;TIME;UDT_CGMS");


                SQLiteDatabase db = Cache.openDatabase();
                //Cursor cur = db.query("bgreadings", new String[]{"timestamp", "calculated_value"}, "timestamp >= ? AND timestamp <=  ? AND calculated_value > ?", new String[]{"" + bounds.start, "" + bounds.stop, CUTOFF}, null, null, orderBy);

                Cursor cur = db.query("bgreadings", new String[]{"timestamp", "calculated_value"}, null, null, null, null, null);

                double value;
                long timestamp;
                java.text.DateFormat df = new SimpleDateFormat("dd.MM.yyyy;HH:mm;");
                Date date = new Date();

                if (cur.moveToFirst()) {
                    do {
                        timestamp = cur.getLong(0);
                        value = cur.getDouble(1);
                        if(value > 13){
                            date.setTime(timestamp);
                            printStream.println(df.format(date) + Math.round(value));
                        }


                    } while (cur.moveToNext());
                }

                printStream.flush();


            } else {
                toastText(context, "SD card not writable!");
                Log.d(TAG, "SD card not writable!");
            }

        } catch (IOException e) {
            toastText(context, "SD card not writable!");
            Log.e(TAG, "Exception while writing DB", e);
        } finally {
            if (printStream != null) {
                printStream.close();
            }
            if (zipOutputStream != null) try {
                zipOutputStream.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
        }
        return zipFilename;
    }




    public static String loadSql(Context context, String path) {

        FileInputStream srcStream = null;
        FileChannel src = null;
        FileOutputStream destStream = null;
        FileChannel dst = null;

        String returnString = "";

        try {
            String databaseName = new Configuration.Builder(context).create().getDatabaseName();
            File currentDB = context.getDatabasePath(databaseName);
            File replacement = new File(path);
            if (!replacement.exists()) {
                Log.d(TAG, "File does not exist: " + path);
                return "File does not exist: " + path;
            }
            if (currentDB.canWrite()) {
                srcStream = new FileInputStream(replacement);
                src = srcStream.getChannel();
                destStream = new FileOutputStream(currentDB);
                dst = destStream.getChannel();
                dst.transferFrom(src, 0, src.size());
                returnString = "Successfully imported database";
            } else {
                Log.v(TAG, "loadSql: No Write access");
                returnString = "loadSql: No Write access";
            }
        } catch (IOException e) {
            Log.e(TAG, "Something went wrong importing Database", e);
            returnString = "Something went wrong importing database";


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
            return returnString;
        }
    }
}
