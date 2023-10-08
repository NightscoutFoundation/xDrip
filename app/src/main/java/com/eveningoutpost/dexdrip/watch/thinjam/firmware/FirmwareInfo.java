package com.eveningoutpost.dexdrip.watch.thinjam.firmware;

// jamorham

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJay;

import java.util.List;

import lombok.val;

public class FirmwareInfo {

    private static volatile int latestCoreFirmware = -2;
    private static volatile int latestMainFirmware = -2;

    private static volatile long lastManifestPoll = 0;
    private static volatile List<BlueJayManifest> manifest = null;
    private static volatile String manifestMac = null;

    private static boolean okayToPoll() {
        val mac = BlueJay.getMac();
        if (mac != null) {
            if (manifestMac == null || !manifestMac.equals(mac)) {
                manifest = null;
                manifestMac = mac;
            }
        }
        if (JoH.msSince(lastManifestPoll) > (manifest != null ? (Constants.MINUTE_IN_MS * 20) : Constants.MINUTE_IN_MS)) {
            lastManifestPoll = JoH.tsl();
            return JoH.pratelimit("bj-manifest-poll-rate", 60);
        } else {
            return false;
        }
    }

    private static void updateManifest(final Runnable runnable) {
        Inevitable.task("bj-poll-firmware", 1000, new Runnable() {
            @Override
            public void run() {
                val result = FirmwareDownload.checkAvailability(BlueJay.getMac());
                if (result != null) manifest = result;
                if (runnable != null) runnable.run();
            }
        });
    }

    private static void refreshPoll() {
        if (okayToPoll()) {
            updateManifest(() -> {
                latestCoreFirmware = FirmwareDownload.getHighestVersionNumber(manifest, FirmwareDownload.FIRMWARE_TYPE_CORE);
                latestMainFirmware = FirmwareDownload.getHighestVersionNumber(manifest, FirmwareDownload.FIRMWARE_TYPE_MAIN);
            });
        }
    }

    public static int getLatestCoreFirmware() {
        refreshPoll();
        return latestCoreFirmware;
    }

    public static int getLatestMainFirmware() {
        refreshPoll();
        return latestMainFirmware;
    }
}
