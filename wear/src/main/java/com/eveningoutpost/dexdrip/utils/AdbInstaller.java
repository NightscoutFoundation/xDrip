package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.DemiGod;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;
import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import java.security.NoSuchAlgorithmException;

import android.os.PowerManager;
import android.util.Base64;

import lombok.val;

/**
 * jamorham
 * <p>
 * AdbInstaller - handles installing packages via a loopback adb interface
 */

public class AdbInstaller implements AdbBase64 {

    private static final String TAG = AdbInstaller.class.getSimpleName();
    private static final PowerManager.WakeLock wl = JoH.getWakeLock("AdbInstallerW", 100);
    private static final String PACKAGE_DESTINATION = "exec:cmd package";
    private static final String INSTALL_COMMAND = " 'install' '-r'";
    private static final String INSTALL_DESTINATION = PACKAGE_DESTINATION + INSTALL_COMMAND;
    private static final String ID_DESTINATION = "shell:id";

    public static void pingIfNoDemigod(final Runnable gotPing) {
        if (!DemiGod.isPresent()) {
            ping(gotPing);
        }
    }

    public static void ping(final Runnable gotPing) {
        new Thread(() -> {
            wl.acquire(35_000);
            try {
                val stream = openDestination(ID_DESTINATION);
                if (stream != null) {
                    stream.close();
                    UserError.Log.d(TAG, "Ping success");
                    if (gotPing != null) gotPing.run();
                } else {
                    UserError.Log.d(TAG, "Ping failure");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                JoH.releaseWakeLock(wl);
            }
        }).start();
    }

    public static void install(final byte[] updateBytes) {
        UserError.Log.d(TAG, "Install called");
        new Thread(() -> {
            val destination = INSTALL_DESTINATION + " -S " + updateBytes.length;
            wl.acquire(65_000);
            try {
                val stream = openDestination(destination);
                if (stream == null) {
                    UserError.Log.d(TAG, "Could not get stream, returning");
                    return;
                }

                UserError.Log.d(TAG, "Writing");
                int ptr = 0;
                while (ptr < updateBytes.length) {
                    val segment = new byte[Math.min(4096, updateBytes.length - ptr)];
                    System.arraycopy(updateBytes, ptr, segment, 0, segment.length);
                    stream.write(segment);
                    ptr += segment.length;
                }

                UserError.Log.d(TAG, "Data sent");
                stream.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                JoH.releaseWakeLock(wl);
            }
            UserError.Log.d(TAG, "Install subroutine finished");
        }
        ).start();
    }

    private static synchronized AdbCrypto getKeys() throws NoSuchAlgorithmException, IOException {
        val path = xdrip.getAppContext().getFilesDir().getPath();
        val filePriv = new File(path + "/adbpriv");
        val filePub = new File(path + "/adbpub");
        try {
            return AdbCrypto.loadAdbKeyPair(new AdbInstaller(), filePriv, filePub);
        } catch (Exception e) {
            UserError.Log.d(TAG, "Generating new keys");
            val crypto = AdbCrypto.generateAdbKeyPair(new AdbInstaller());
            crypto.saveAdbKeyPair(filePriv, filePub);
            return crypto;
        }
    }

    private static AdbStream openDestination(final String destination) {
        try {
            val socket = new Socket("127.0.0.1", 5555);
            val connection = AdbConnection.create(socket, getKeys());
            UserError.Log.d(TAG, "Connecting");
            connection.connect();
            UserError.Log.d(TAG, "Opening stream");

            val stream = connection.open(destination);
            if (stream.isClosed()) {
                UserError.Log.d(TAG, "Stream already closed");
                return null;
            }
            return stream;
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String encodeToString(final byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }
}
