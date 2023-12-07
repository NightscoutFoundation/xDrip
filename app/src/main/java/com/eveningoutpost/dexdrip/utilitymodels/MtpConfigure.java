package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import android.text.SpannableString;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.ui.helpers.Span;
import com.eveningoutpost.dexdrip.utils.usb.MtpTools.MtpDeviceHelper;
import com.eveningoutpost.dexdrip.utils.usb.UsbTools;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.GsonBuilder;

import java.nio.charset.Charset;

import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.BAD;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.GOOD;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NORMAL;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.DEX_COLLECTION_METHOD;
import static com.eveningoutpost.dexdrip.utils.GetWearApk.getBytes;

// jamorham

@RequiresApi(api = Build.VERSION_CODES.N)
public class MtpConfigure {

// TODO check what happens if device full

    private static final String TAG = "USBTOOLS-MTPconfig";
    private static volatile String status = "Not active";
    private static volatile boolean error = false;
    private static volatile long last_success = 0;

    public static void handleConnect(final Intent intent) {
        final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        handleConnect(device);
    }

    private static void msg(final String msg) {
        error = false;
        status = msg;
    }

    private static void msg(@StringRes final int id) {
        msg(xdrip.gs(id));
    }

    private static void err(final String msg) {
        status = msg;
        error = true;
    }

    private static void err(@StringRes final int id) {
        err(xdrip.gs(id));
    }

    public static void handleConnect(final UsbDevice device) {

        if (device != null) {
            if (Build.VERSION.SDK_INT < 24) {
                Log.e(TAG, "Doesn't work below android 7");
                return;
            }
            if (JoH.ratelimit("mtp-request-perms", 25)) {
                msg("Requesting permission");
                Log.d(TAG, "Processing handleConnect()");

                UsbTools.requestPermission(device, new UsbTools.PermissionReceiver() {
                    @Override
                    public void onGranted(final UsbDevice device) {
                        msg("Permission granted");
                        Log.d(TAG, "Permission granted - executing in background");
                        Inevitable.task("mtp-configure-action", 1000, () -> openDevice(device));
                    }
                });
            }
        }
    }


    private static synchronized void openDevice(final UsbDevice device) {
        if (JoH.msSince(last_success) < Constants.MINUTE_IN_MS * 2) {
            msg("Please disconnect device");
            return;
        }
        msg("Opening device");
        Log.d(TAG, "Attempting to open MTP device");
        final MtpDeviceHelper mtp = new MtpDeviceHelper(device);
        if (!mtp.ok()) {
            msg("Cannot connect to device");
            Log.e(TAG, "Cannot open MTP device");
            return;
        }

        try {
            msg("Device: " + mtp.name());
            Log.d(TAG, "mtp name: " + mtp.name());

            if (mtp.hash().startsWith("eb3b2846")) {

                if (mtp.numberOfStorageIds() != 1) {
                    err("Problem with device storage");
                    Log.e(TAG, "Invalid Size of storage ids: " + mtp.numberOfStorageIds()); // baulk if not 1
                    return;
                }

                final int storageID = mtp.getFirstStorageId();
                if (storageID == -1) {
                    err("Cannot open device storage");
                    Log.e(TAG, "Cannot get first storage id");
                    return;
                }

                if (mtp.recreateRootFile("xdrip-install.dat", getBytes()) > 0) {
                    msg("Stage 1 success");
                    final byte[] configBytes = new GsonBuilder().setPrettyPrinting().create().toJson(new XdripConfig()).getBytes(Charset.forName("UTF-8"));
                    if (mtp.recreateRootFile("xdrip.cfg", configBytes) > 0) {

                        msg("All stages successful\nWait for device to reboot itself and then disconnect cable");
                        last_success = JoH.tsl();
                    }
                }

            } else {
                err("Device type not known - cannot proceed");
                Log.e(TAG, "Device doesn't match");
            }
        } finally {
            mtp.close();
        }
    }


    private static class XdripConfig {
        final String collector = Pref.getString(DEX_COLLECTION_METHOD, null);
        final String txid = Pref.getString("dex_txid", null);
        final String units = Pref.getString("units", "mgdl");
        final boolean usingMgDl = Pref.getString("units", "mgdl").equals("mgdl");
        final boolean onlyEverUseWearCollector = Pref.getBooleanDefaultFalse("only_ever_use_wear_collector");

        @Override
        public String toString() {
            return collector + " " + txid + " " + units + " " + usingMgDl + " " + onlyEverUseWearCollector;
        }
    }

    public static SpannableString nanoStatus() {
        return Span.colorSpan(status, error ? BAD.color() : JoH.msSince(last_success) < Constants.MINUTE_IN_MS ? GOOD.color() : NORMAL.color());
    }

}


