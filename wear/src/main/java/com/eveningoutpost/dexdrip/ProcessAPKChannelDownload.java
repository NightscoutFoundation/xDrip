package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;


import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utils.AdbInstaller;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.utils.VersionFixer;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.eveningoutpost.dexdrip.ListenerService.apkBytesRead;
import static com.eveningoutpost.dexdrip.ListenerService.apkBytesVersion;

// jamorham

// work around Oreo bugs

public class ProcessAPKChannelDownload extends JobIntentService {

    private static final String TAG = ProcessAPKChannelDownload.class.getSimpleName();
    private static volatile byte[] apkBytesOutput = new byte[0];
    private static volatile Channel channel;
    private static volatile GoogleApiClient googleApiClient;

    public synchronized void process() {

        if ((googleApiClient == null) || (channel == null)) {
            if (JoH.ratelimit("channel-error-msg", 10)) {
                UserError.Log.wtf(TAG, "Could not process as input parameters are null!");
            }
            return;
        }
        channel.getInputStream(googleApiClient).setResultCallback(new ResultCallback<Channel.GetInputStreamResult>() {
            @Override
            public void onResult(Channel.GetInputStreamResult getInputStreamResult) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final PowerManager.WakeLock wl = JoH.getWakeLock("receive-apk-update", 300000);
                        try {
                            android.util.Log.d(TAG, "onChannelOpened: onResult");
                            if (getInputStreamResult == null) {
                                UserError.Log.d(TAG, "Channel input stream result is NULL!");
                                return;
                            }

                            InputStream input = null;
                            BufferedReader reader = null;
                            try {
                                input = getInputStreamResult.getInputStream();
                                if (input == null) {
                                    UserError.Log.e(TAG, "Input stream is null!");
                                    return;
                                }
                                reader = new BufferedReader(new InputStreamReader(input));

                                // this protocol can never change
                                final String versionId = reader.readLine();
                                UserError.Log.d(TAG, "Source version identifier: " + versionId);
                                final String sizeText = reader.readLine();
                                final int size = Integer.parseInt(sizeText);
                                final String startText = reader.readLine();
                                final int startAt = Integer.parseInt(startText);
                                if (!versionId.equals(apkBytesVersion)) {
                                    UserError.Log.d(TAG, "New UUID to buffer: " + apkBytesVersion + " vs " + versionId);
                                    apkBytesOutput = new byte[size];
                                    apkBytesRead = 0;
                                    apkBytesVersion = versionId;
                                }

                                if (apkBytesOutput.length != size) {
                                    UserError.Log.d(TAG, "Buffer size wrong! us:" + apkBytesOutput.length + " vs " + size);
                                    return;
                                }

                                if (startAt > apkBytesRead) {
                                    UserError.Log.e(TAG, "Cannot start at position: " + startAt + " vs " + apkBytesRead);
                                    return;
                                }

                                if (startAt != apkBytesRead) {
                                    UserError.Log.d(TAG, "Setting start position to: " + startAt);
                                    apkBytesRead = startAt;
                                }

                                while (apkBytesRead < apkBytesOutput.length) {
                                    final int complete = (apkBytesRead * 100 / apkBytesOutput.length);
                                    android.util.Log.d(TAG, "Preparing to read, total: " + apkBytesRead + " out of " + apkBytesOutput.length + " complete " + complete + "%");
                                    if (JoH.quietratelimit("wear-update-notice", 5)) {
                                        JoH.static_toast_long("Updating xDrip " + complete + "%");
                                        if (JoH.quietratelimit("adb ping", 30)) {
                                            AdbInstaller.pingIfNoDemigod(null);
                                        }
                                    }

                                    final long startedWaiting = JoH.tsl();
                                    while (apkBytesRead < apkBytesOutput.length && input.available() == 0) {
                                        if (JoH.msSince(startedWaiting) > Constants.SECOND_IN_MS * 30) {
                                            UserError.Log.e(TAG, "Timed out waiting for new APK data!");
                                            Inevitable.task("re-request-apk", 5000, new Runnable() {
                                                @Override
                                                public void run() {
                                                    UserError.Log.d(TAG, "Asking to resume apk from: " + apkBytesRead);
                                                    ListenerService.requestAPK(apkBytesRead);
                                                }
                                            });
                                            return;
                                        }
                                        android.util.Log.d(TAG, "Pausing for new data");
                                        JoH.threadSleep(1000);
                                    }
                                    final int bytesToRead = Math.min(input.available(), apkBytesOutput.length - apkBytesRead);
                                    UserError.Log.d(TAG, "Before read: " + bytesToRead);
                                    if (bytesToRead > 0) {
                                        apkBytesRead += input.read(apkBytesOutput, apkBytesRead, bytesToRead);
                                    }
                                    UserError.Log.d(TAG, "After read");
                                }

                                android.util.Log.d(TAG, "onChannelOpened: onResult: Received the following COMPLETE message: " + apkBytesRead);
                                UserError.Log.d(TAG, "APK sha256: " + CipherUtils.getSHA256(apkBytesOutput));
                                VersionFixer.runPackageInstaller(apkBytesOutput);
                                apkBytesOutput = new byte[0];
                                apkBytesRead = 0;
                                apkBytesVersion = "";

                            } catch (final IOException e) {
                                if (channel != null) {
                                    android.util.Log.w(TAG, "Could not read channel message Node ID: " + channel.getNodeId() + " Path: " + channel.getPath() + " Error message: " + e.getMessage() + " Error cause: " + e.getCause());
                                } else {
                                    android.util.Log.w(TAG, "channel is null in ioexception: " + e);
                                }
                            } finally {
                                try {
                                    if (input != null) {
                                        input.close();
                                    }
                                    if (reader != null) {
                                        reader.close();
                                    }
                                } catch (final IOException e) {
                                    if (channel != null) {
                                        android.util.Log.d(TAG, "onChannelOpened: onResult: Could not close buffered reader. Node ID: " + channel.getNodeId() + " Path: " + channel.getPath() + " Error message: " + e.getMessage() + " Error cause: " + e.getCause());
                                    } else {
                                        android.util.Log.d(TAG, "channel is null in final ioexception: " + e);
                                    }
                                }
                            }

                        } finally {
                            android.util.Log.d(TAG, "Finally block before exit");
                            try {
                                channel.close(googleApiClient);
                            } catch (Exception e) {
                                //
                            }
                            channel = null;
                            googleApiClient = null;
                            JoH.releaseWakeLock(wl);
                        }
                    }
                }).start();
            }
        });

        UserError.Log.d(TAG, "Process exit with channel callback scheduled");

    }


    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        UserError.Log.d(TAG, "onHandleWork enter");
        process();
        UserError.Log.d(TAG, "onHandleWork exit");

    }

    static synchronized void enqueueWork(final GoogleApiClient client, final Channel current_channel) {
        UserError.Log.d(TAG, "EnqueueWork enter");
        if (client == null || current_channel == null) {
            UserError.Log.d(TAG, "Enqueue Work: Null input data!!");
            return;
        }
        googleApiClient = client;
        channel = current_channel;
        enqueueWork(xdrip.getAppContext(), ProcessAPKChannelDownload.class, Constants.APK_DOWNLOAD_JOB_ID, new Intent());
    }

}
