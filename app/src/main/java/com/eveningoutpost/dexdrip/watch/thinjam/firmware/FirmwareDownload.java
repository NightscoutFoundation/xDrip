package com.eveningoutpost.dexdrip.watch.thinjam.firmware;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJay;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayInfo;
import com.eveningoutpost.dexdrip.watch.thinjam.io.GetURL;

import java.util.List;
import java.util.Locale;

import lombok.val;

import static com.eveningoutpost.dexdrip.watch.thinjam.io.GetURL.getURL;

// jamorham

public class FirmwareDownload {

    private static final String TAG = "BluejayFirmware";
    private static final String UPDATE_URL = "https://cdn159875.bluejay.website/cdn/firmware/bluejay/";
    private static final long CACHE_MS = Constants.SECOND_IN_MS * 60;

    static final int FIRMWARE_TYPE_MAIN = 1;
    static final int FIRMWARE_TYPE_CORE = 2;
    static final int FIRMWARE_TYPE_EXPN = 3;

    public static List<BlueJayManifest> checkAvailability(final String mac_in) {
        if (mac_in == null) return null;
        val mac = mac_in.toUpperCase();
        val fwid = BlueJayFwId.getFwPrefix(mac, BlueJay.getIdentityKey(mac));
        if (fwid != null) {
            val metrics = BlueJayInfo.getInfo(mac).getMetrics();
            val manifest = getURL(String.format(Locale.US, "%s%s%s?p=%s&m=%s&rr=%d", UPDATE_URL, fwid, "-manifest", mac.replace(":", "-"), metrics, JoH.tsl() / CACHE_MS));

            if (manifest != null) {
                UserError.Log.d(TAG, "Manifest: " + manifest);
                return BlueJayManifest.parseToList(manifest);
            } else {
                UserError.Log.d(TAG, "No manifest found");
            }

        } else {
            UserError.Log.d(TAG, "Insufficient information to look for firmware");
        }
        return null;
    }

    public static byte[] getLatestFirmwareBytes(final String mac, final int type) {
        val available = checkAvailability(mac);
        if (available != null) {
            UserError.Log.d(TAG, "Parsing available list: " + available.size());
            val filename = getHighestVersionFilename(available, type);
            UserError.Log.d(TAG, "Download Filename: " + filename);
            if (filename != null) {
                return GetURL.getURLbytes(String.format(Locale.US, "%s%s", UPDATE_URL, filename));
            }
        }
        return null;
    }

    private static BlueJayManifest getHighestVersionRecord(final List<BlueJayManifest> list, final int type) {
        BlueJayManifest result = null;
        if (list != null) {
            for (val item : list) {
                UserError.Log.d(TAG, item.fileName + " " + item.type + " " + item.version);
                if (item.type == type) {
                    if (result == null || result.version < item.version) {
                        result = item;
                    }
                }
            }
        }
        return result;
    }

    private static String getHighestVersionFilename(final List<BlueJayManifest> list, final int type) {
        val result = getHighestVersionRecord(list, type);
        return result != null ? result.fileName : null;
    }

    public static int getHighestVersionNumber(final List<BlueJayManifest> list, final int type) {
        val result = getHighestVersionRecord(list, type);
        return result != null ? result.version : -1;
    }

}
