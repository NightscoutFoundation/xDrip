package com.eveningoutpost.dexdrip.utils;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.DemiGod;
import com.eveningoutpost.dexdrip.ListenerService;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressLint("LogNotTimber")
public class VersionFixer {

    private static final String TAG = VersionFixer.class.getSimpleName();

    private static final String PEER_VERSION = "PEER_VERSION";
    private static final String PEER_VERSION_UPDATED = "PEER_VERSION_UPDATED";
    private static final String PEER_VERSION_CHECKED = "PEER_VERSION_CHECKED";

    private static final int RETRY_TIME = (int) Constants.HOUR_IN_MS * 12;

    private static final String APK_PATH = "/data/local/tmp/installme.apk";
    // check peer version

    public static synchronized void updateAndCheck(String version, String processingVersion) {
        if (version == null) return;
        if (Build.VERSION.SDK_INT > 23) {
            updateVersion(version);
            checkAndActOnVersionDifference();
        } else {
            Log.d(TAG, "Ignoring update as we appear to be running on wear 1.x");
        }
    }


    private static boolean updateVersion(String version) {
        if ((!PersistentStore.getString(PEER_VERSION).equals(version)
                || (JoH.pratelimit("allow-version-recheck", RETRY_TIME)))) {
            PersistentStore.setString(PEER_VERSION, version);
            PersistentStore.setLong(PEER_VERSION_UPDATED, JoH.tsl());
            UserError.Log.d(TAG, "Updated peer version to: " + version);
            AdbInstaller.pingIfNoDemigod(null);
            return true;
        } else {
            return false;
        }
    }


    public static void runPackageInstaller() {
        try {
            UserError.Log.d(TAG, "Trying to run package installer");
            final File f = new File(APK_PATH);
            final InputStream i = new FileInputStream(f);

            installPackage(xdrip.getAppContext(), i, BuildConfig.APPLICATION_ID);
        } catch (Exception e) {
            UserError.Log.e(TAG, e.toString());
        }
    }

    public static void runPackageInstaller(final byte[] buffer) {
        if (buffer == null) return;
        if (DemiGod.isPresent()) {
            runDemiGodPackageInstaller(buffer);
        } else {
            UserError.Log.d(TAG, "Trying adb install");
            AdbInstaller.install(buffer);
        }
    }

    public static void runDemiGodPackageInstaller(final byte[] buffer) {
        if (buffer == null) return;
        try {
            UserError.Log.ueh(TAG, "Running demigod package installer with payload size: " + buffer.length);
            installPackage(xdrip.getAppContext(), buffer, BuildConfig.APPLICATION_ID);
        } catch (Exception e) {
            UserError.Log.e(TAG, e.toString());
        }
    }


    private static boolean installPackage(Context context, InputStream in, String packageName)
            throws IOException {
        final PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        final PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(packageName);

        final int sessionId = packageInstaller.createSession(params);
        final PackageInstaller.Session session = packageInstaller.openSession(sessionId);
        final OutputStream out = session.openWrite("COSU", 0, -1);
        final byte[] buffer = new byte[65536];
        int bytes_read;
        while ((bytes_read = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytes_read);
        }
        session.fsync(out);
        in.close();
        out.close();

        session.commit(createIntentSender(context, sessionId));
        return true;
    }

    private static boolean installPackage(Context context, byte[] buffer, String packageName)
            throws IOException {
        final PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        final PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(packageName);

        final int sessionId = packageInstaller.createSession(params);
        final PackageInstaller.Session session = packageInstaller.openSession(sessionId);
        final OutputStream out = session.openWrite("COSU", 0, -1);
        out.write(buffer, 0, buffer.length);

        session.fsync(out);
        out.close();
        session.commit(createIntentSender(context, sessionId));
        return true;
    }

    public static void disableUpdates() {
        if (DemiGod.isPresent()) {
            if (JoH.pratelimit("disable-gms-updates", 86400)) {
                try {
                    UserError.Log.e(TAG, "Attempting to disable system update");
                    xdrip.getAppContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.google.android.gms", "com.google.android.gms.update.SystemUpdateService"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Exception trying to disable system update: " + e);
                }
            }
        }
    }


    private static final String ACTION_INSTALL_COMPLETE = "jamorham-action-complete";

    private static IntentSender createIntentSender(Context context, int sessionId) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(ACTION_INSTALL_COMPLETE),
                0);
        return pendingIntent.getIntentSender();
    }


    private static void checkAndActOnVersionDifference() {
        if (checkVersion()) {
            UserError.Log.d(TAG, "Version report as different: " + getLocalVersion() + " vs " + getPeerVersion());
            resolveVersionDifference(getPeerVersion());
        }
    }

    private static void resolveVersionDifference(String version) {
        UserError.Log.ueh(TAG, "Attempting to resolve version difference");
        // Check rom permission level
            if (JoH.pratelimit("resolve-version-difference-download", 40000)) {
                UserError.Log.e(TAG, "Wanting update of wear app for: " + version);
                JoH.static_toast_long("Asking phone for updated wear app");
                downloadApk();
            } else {
                UserError.Log.e(TAG,"Wanting update but not requesting due to rate limit");
            }
    }


    // request the download
    public static void downloadApk() {
        JoH.static_toast_long("Requesting wear app download");
        UserError.Log.uel(TAG, "Requesting updated APK from phone, our version: " + getLocalVersion() + " vs " + getPeerVersion());
        ListenerService.requestAPK(getPeerVersion());
    }

    // return true if not the same
    private static boolean checkVersion() {
        if (isDataNew()) {
            UserError.Log.d(TAG, "We have new data");
            return !compareVersions();
        }
        return false; // do nothing
    }

    private static boolean isDataNew() {
        final long last_updated = PersistentStore.getLong(PEER_VERSION_UPDATED);
        if (last_updated > 0) {
            final long last_checked = PersistentStore.getLong(PEER_VERSION_CHECKED);
            return last_updated > last_checked;
        }
        return false;
    }


    private static boolean compareVersions() {
        final String peerVersion = getPeerVersion();
        UserError.Log.d(TAG, "Compare versions: " + getPeerVersion() + " vs " + getLocalVersion());
        return peerVersion == null || getLocalVersion().equals(peerVersion);
    }


    private static String getLocalVersion() {
        // any change must be replicated on phone
        return BuildConfig.VERSION_NAME;
    }


    private static String getPeerVersion() {
        final long updated = PersistentStore.getLong(PEER_VERSION_UPDATED);
        if (JoH.msSince(updated) < Constants.MONTH_IN_MS) {
            return PersistentStore.getString(PEER_VERSION);
        }
        return null;
    }

}
