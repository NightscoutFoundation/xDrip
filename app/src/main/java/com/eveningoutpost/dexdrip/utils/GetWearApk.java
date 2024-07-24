package com.eveningoutpost.dexdrip.utils;

import android.content.pm.ApplicationInfo;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// jamorham

// extract the wear apk file from the running apk

public class GetWearApk {

    private static final String TAG = GetWearApk.class.getSimpleName();
    private static final int MAX_BYTES = 10000000;

    public static boolean isFindable() {
        return findOuterApkPath() != null;
    }


    public static byte[] getBytes() {
        if (isFindable()) {
            UserError.Log.d(TAG, "Outer apk found - extracting");
            final String path = findOuterApkPath();
            return path != null ? extractFromApk(new File(path)) : null;
        } else {
            UserError.Log.e(TAG, "Cannot find file");
            return null;
        }
    }


    private static String findOuterApkPath() {
        try {
            final ApplicationInfo applicationInfo = xdrip.getAppContext().getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
            return applicationInfo.publicSourceDir;
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception getting apk file: " + e);
            //
        }
        return findOuterApkPathLegacy();
    }

    // probably never reached anymore
    private static String findOuterApkPathLegacy() {
        for (int c = 0; c < 5; c++) {
            final String path = "/data/app/" + BuildConfig.APPLICATION_ID + "-" + c + "/base.apk";
            if (exists(path)) {
                return path;
            }
            final String alt_path = "/data/app/" + BuildConfig.APPLICATION_ID + "-" + c + ".apk";
            if (exists(alt_path)) {
                return alt_path;
            }
        }
        return null;
    }

    private static boolean exists(String path) {
        final File file = new File(path);
        return file.exists() && file.canRead();
    }


    private static byte[] extractFromApk(File file) {
        if (file == null) {
            UserError.Log.d(TAG, "null file passed to extractFromApk");
            return null;
        }
        try {
            final FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());
            final ZipInputStream zip_stream = new ZipInputStream(new BufferedInputStream(fileInputStream));
            ZipEntry zipEntry;
            while ((zipEntry = zip_stream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) continue;
                final String filename = zipEntry.getName();
                if (filename.endsWith(".apk")) {
                    final byte[] buffer = new byte[Math.min((int) zipEntry.getSize(), MAX_BYTES)];
                    final int read_bytes = readAllToBuffer(zip_stream, buffer);
                    if (read_bytes != buffer.length) {
                        throw new IOException("Read incorrect number of bytes: " + read_bytes + " vs " + buffer.length);
                    }
                    UserError.Log.d(TAG, "Got match: " + filename + " " + read_bytes);
                    return buffer; // got the data
                }
                //zip_stream.closeEntry();
            }
            UserError.Log.wtf(TAG, "Could not find Wear mini-apk");
            zip_stream.close();
            fileInputStream.close();
        } catch (IOException e) {
            UserError.Log.e(TAG, "Got exception: " + e);
            return null;
        }
        return null;
    }

    private static int readAllToBuffer(InputStream stream, byte[] buffer) throws IOException {
        int readBytes = 0;
        int thisRead;
        while ((thisRead = stream.read(buffer, readBytes, buffer.length - readBytes)) > 0) {
            readBytes += thisRead;
            if (readBytes > MAX_BYTES) break;
        }
        return readBytes;
    }
}
