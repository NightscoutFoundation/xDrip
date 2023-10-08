package com.eveningoutpost.dexdrip;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.View;

import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.models.GlucoseData;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Libre2SensorData;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.models.LibreOOPAlgorithm;
import com.eveningoutpost.dexdrip.models.ReadingData;
import com.eveningoutpost.dexdrip.models.SensorSanity;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.LibreUtils;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import com.eveningoutpost.dexdrip.models.LibreOOPAlgorithm.SensorType;
import com.eveningoutpost.dexdrip.utils.LibreTrendUtil;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.eveningoutpost.dexdrip.xdrip.gs;

// From LibreAlarm et al

// TODO have we always checked checksum on this data? what about LibreAlarm path?


public class NFCReaderX {

    private static final String TAG = "NFCReaderX";
    private static final boolean d = false; // global debug flag
    public static final int REQ_CODE_NFC_TAG_FOUND = 19312;
    public static boolean used_nfc_successfully = false;
    private static final int MINUTE = 60000;
    private static NfcAdapter mNfcAdapter;
    private static boolean foreground_enabled = false;
    private static boolean tag_discovered = false;
    private static long last_tag_discovered = -1;
    private static boolean last_read_succeeded = false;
    private static final Object tag_lock = new Object();
    private static final Lock read_lock = new ReentrantLock();
    private static final boolean useReaderMode = true;
    private static boolean nfc_enabled = false;
    private static final String ENABLE_BLUETOOTH_TIMESTAMP = "enable_bluetooth_timestamp";

    // Constants for libre1/2 FRAM
    final static int FRAM_RECORD_SIZE = 6;
    final static int TREND_START = 28;
    final static int HISTORY_START = 124;

    // Constants for libre pro
    static final int LPRO_SENSORMINUTES = 74;
    static final int LPRO_TRENDPOINTER = 76;
    static final int LPRO_TRENDOFFSET = 80;

    // For libre2 emulation only
    static final byte[] de_new_packet = {(byte) 0x87, (byte) 0x88, (byte) 0xd2, (byte) 0xe8, (byte) 0xdd, (byte) 0x28, (byte) 0x9b, (byte) 0x95, (byte) 0xb5, (byte) 0x9d, (byte) 0xe1, (byte) 0x1f, (byte) 0x47, (byte) 0x2c, (byte) 0x61, (byte) 0x4f, (byte) 0xcb, (byte) 0x81, (byte) 0x5e, (byte) 0xc8, (byte) 0x36, (byte) 0x4a, (byte) 0x4c, (byte) 0x1f, (byte) 0xa4, (byte) 0xc8, (byte) 0x59, (byte) 0x81, (byte) 0x72, (byte) 0xbf, (byte) 0x9e, (byte) 0xae, (byte) 0xa4, (byte) 0x1b, (byte) 0x51, (byte) 0x5b, (byte) 0xb9, (byte) 0x9f, (byte) 0x6a, (byte) 0x6b, (byte) 0xf4, (byte) 0x55, (byte) 0x78, (byte) 0xe1, (byte) 0xa3, (byte) 0x4f, (byte) 0x3a, (byte) 0x60, (byte) 0x49, (byte) 0x8f, (byte) 0x1f, (byte) 0xcb, (byte) 0xdf, (byte) 0x2e, (byte) 0xdc, (byte) 0xbe, (byte) 0x59, (byte) 0xc9, (byte) 0x28, (byte) 0x8e, (byte) 0xf4, (byte) 0x83, (byte) 0x16, (byte) 0x11, (byte) 0xa, (byte) 0xd2, (byte) 0x74, (byte) 0x6f, (byte) 0x9d, (byte) 0xbf, (byte) 0x29, (byte) 0x44, (byte) 0x37, (byte) 0x8d, (byte) 0xe9, (byte) 0xf7, (byte) 0x47, (byte) 0x1, (byte) 0x2b, (byte) 0x3, (byte) 0x5e, (byte) 0x9b, (byte) 0x72, (byte) 0x25, (byte) 0x1f, (byte) 0x82, (byte) 0x11, (byte) 0xb5, (byte) 0xdb, (byte) 0x19, (byte) 0x42, (byte) 0x9c, (byte) 0xfe, (byte) 0x91, (byte) 0x63, (byte) 0x94, (byte) 0xf7, (byte) 0x14, (byte) 0x67, (byte) 0xb6, (byte) 0x25, (byte) 0xf3, (byte) 0xf9, (byte) 0xee, (byte) 0x30, (byte) 0x54, (byte) 0xa4, (byte) 0x89, (byte) 0x2b, (byte) 0xa8, (byte) 0xe4, (byte) 0x6f, (byte) 0x7a, (byte) 0x4f, (byte) 0xa3, (byte) 0xdc, (byte) 0xc0, (byte) 0x43, (byte) 0xfc, (byte) 0x38, (byte) 0x1c, (byte) 0x32, (byte) 0x76, (byte) 0x1b, (byte) 0x17, (byte) 0xb6, (byte) 0x81, (byte) 0x87, (byte) 0xf8, (byte) 0xd3, (byte) 0x97, (byte) 0xca, (byte) 0xd5, (byte) 0x67, (byte) 0xf6, (byte) 0x4a, (byte) 0xbd, (byte) 0x6f, (byte) 0x2b, (byte) 0x90, (byte) 0xd6, (byte) 0xd2, (byte) 0x4e, (byte) 0x96, (byte) 0x87, (byte) 0x98, (byte) 0xcf, (byte) 0xf6, (byte) 0x82, (byte) 0xb3, (byte) 0x7b, (byte) 0xf1, (byte) 0xd4, (byte) 0xf2, (byte) 0x3b, (byte) 0xb3, (byte) 0xc3, (byte) 0x76, (byte) 0x33, (byte) 0xe5, (byte) 0xa3, (byte) 0xe9, (byte) 0x27, (byte) 0xde, (byte) 0x6a, (byte) 0x21, (byte) 0xc2, (byte) 0xb2, (byte) 0xfc, (byte) 0x2, (byte) 0x87, (byte) 0xb1, (byte) 0x55, (byte) 0x7c, (byte) 0xc9, (byte) 0xe0, (byte) 0x5b, (byte) 0x9f, (byte) 0x63, (byte) 0x61, (byte) 0x67, (byte) 0x18, (byte) 0x3d, (byte) 0xe9, (byte) 0x92, (byte) 0x1f, (byte) 0xed, (byte) 0xad, (byte) 0x41, (byte) 0xee, (byte) 0x8d, (byte) 0xd7, (byte) 0x5e, (byte) 0x3d, (byte) 0x4b, (byte) 0xa4, (byte) 0x20, (byte) 0xfa, (byte) 0x6c, (byte) 0xc, (byte) 0xf7, (byte) 0x68, (byte) 0xe5, (byte) 0xfb, (byte) 0x90, (byte) 0xc6, (byte) 0x54, (byte) 0x49, (byte) 0x4d, (byte) 0xfe, (byte) 0x1e, (byte) 0xa3, (byte) 0x25, (byte) 0x2b, (byte) 0xa5, (byte) 0x6f, (byte) 0xf9, (byte) 0xc0, (byte) 0xce, (byte) 0x18, (byte) 0x67, (byte) 0x6e, (byte) 0x33, (byte) 0xc1, (byte) 0x43, (byte) 0x53, (byte) 0x35, (byte) 0x44, (byte) 0x52, (byte) 0x91, (byte) 0xd2, (byte) 0x8, (byte) 0x5a, (byte) 0x9d, (byte) 0x18, (byte) 0xea, (byte) 0x2d, (byte) 0xcb, (byte) 0x11, (byte) 0x2b, (byte) 0xe0, (byte) 0xb, (byte) 0xe3, (byte) 0x84, (byte) 0x18, (byte) 0x54, (byte) 0xc0, (byte) 0xc1, (byte) 0x74, (byte) 0xfb, (byte) 0x53, (byte) 0x4d, (byte) 0x3a, (byte) 0x29, (byte) 0x56, (byte) 0x6d, (byte) 0xce, (byte) 0x7e, (byte) 0x28, (byte) 0x4, (byte) 0xf, (byte) 0xd4, (byte) 0xb7, (byte) 0xaa, (byte) 0x19, (byte) 0x4f, (byte) 0x5f, (byte) 0x60, (byte) 0x5a, (byte) 0x59, (byte) 0x9, (byte) 0x89, (byte) 0xa3, (byte) 0xed, (byte) 0x24, (byte) 0xcc, (byte) 0x6f, (byte) 0x88, (byte) 0xf8, (byte) 0x53, (byte) 0xd7, (byte) 0xe3, (byte) 0x74, (byte) 0x7, (byte) 0x6d, (byte) 0xe1, (byte) 0x6e, (byte) 0xe9, (byte) 0xed, (byte) 0x64, (byte) 0xf, (byte) 0x46, (byte) 0x58, (byte) 0xe, (byte) 0x8f, (byte) 0x30, (byte) 0x6b, (byte) 0xdb, (byte) 0xd6, (byte) 0xbd, (byte) 0x56, (byte) 0xe0, (byte) 0x89, (byte) 0x87, (byte) 0x51, (byte) 0x4e, (byte) 0xad, (byte) 0xe3, (byte) 0x63, (byte) 0xf, (byte) 0x18, (byte) 0x41, (byte) 0x45, (byte) 0x52, (byte) 0xdd, (byte) 0x3e, (byte) 0x21, (byte) 0xe, (byte) 0x74, (byte) 0x6b, (byte) 0xd9, (byte) 0xcf, (byte) 0x4f, (byte) 0xa3, (byte) 0x94, (byte) 0x62, (byte) 0xff, (byte) 0x6a, (byte) 0x52, (byte) 0xbe, (byte) 0x15, (byte) 0x37, (byte) 0xbb, (byte) 0xad, (byte) 0xd4, (byte) 0x63, (byte) 0x28, (byte) 0x23, (byte) 0x26, (byte) 0x60, (byte) 0x90, (byte) 0xe7, (byte) 0xcd, (byte) 0xf6};
    static final byte[] de_new_patch_uid = {(byte) 0xd6, (byte) 0xf1, (byte) 0x0f, (byte) 0x01, (byte) 0x00, (byte) 0xa4, (byte) 0x07, (byte) 0xe0};
    static final byte[] de_new_patch_info = {(byte) 0x9d, (byte) 0x08, (byte) 0x30, (byte) 0x01, (byte) 0x9c, (byte) 0x16};

    // Never in production. Used to emulate German sensor behavior.
    public static boolean use_fake_de_data() {
        //Pref.setBoolean("use_fake_de_data", true);
        //
        boolean ret = Pref.getBooleanDefaultFalse("use_fake_de_data");
        Log.d(TAG, "using fake data = " + ret);
        return ret;
    }

    static boolean enable_bluetooth_ask_user = false;

    enum ENABLE_BLUETOOTH_SET {
        ALWAYS_ALLOW,
        NEVER_ALLOW,
        ASK
    }


    @Deprecated
    public static void stopNFC(Activity context) {
        if (foreground_enabled) {
            try {
                NfcAdapter.getDefaultAdapter(context).disableForegroundDispatch(context);
            } catch (Exception e) {
                Log.d(TAG, "Got exception disabling foregrond dispatch");
            }
            foreground_enabled = false;
        }
    }

    public static boolean useNFC() {
        return Pref.getBooleanDefaultFalse("use_nfc_scan") && (DexCollectionType.hasLibre());
    }

    @SuppressLint("NewApi")
    public static void disableNFC(final Activity context) {
        if (nfc_enabled) {
            try {
                if ((Build.VERSION.SDK_INT >= 19) && (useReaderMode)) {
                    Log.d(TAG, "Shutting down NFC reader mode");
                    mNfcAdapter.disableReaderMode(context);
                    nfc_enabled = false;
                }
                // TODO ALSO handle api < 19 ?
            } catch (Exception e) {
                //
            }
        }
    }

    @SuppressLint("NewApi")
    @Deprecated
    public static void doNFC(final Activity context) {

        if (!useNFC()) return;

        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
        try {
            if (mNfcAdapter == null) {

                JoH.static_toast_long(gs(R.string.phone_has_no_nfc_reader));
                //finish();
                return;

            } else if (!mNfcAdapter.isEnabled()) {
                JoH.static_toast_long(gs(R.string.nfc_is_not_enabled));
                return;
            }
        } catch (NullPointerException e) {
            JoH.static_toast_long(gs(R.string.phone_nfc_is_having_problems));
            return;
        }

        nfc_enabled = true;

        NfcManager nfcManager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
        if (nfcManager != null) {
            mNfcAdapter = nfcManager.getDefaultAdapter();
        }

        if (mNfcAdapter != null) {
            try {
                mNfcAdapter.isEnabled();
            } catch (NullPointerException e) {
                return;
            }
            // some superstitious code here
            try {
                mNfcAdapter.isEnabled();
            } catch (NullPointerException e) {
                return;
            }


            if ((Build.VERSION.SDK_INT >= 19) && (useReaderMode)) {
                try {
                    mNfcAdapter.disableReaderMode(context);
                    final Bundle options = new Bundle();
                    options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 5000);
                    mNfcAdapter.enableReaderMode(context, new NfcAdapter.ReaderCallback() {
                        @Override
                        public void onTagDiscovered(Tag tag) {
                            Log.d(TAG, "Reader mode tag discovered");
                            doTheScan(context, tag, false);
                        }
                    }, NfcAdapter.FLAG_READER_NFC_V | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, options);
                } catch (NullPointerException e) {
                    Log.wtf(TAG, "Null pointer exception from NFC subsystem: " + e.toString());
                }
            } else {
                PendingIntent pi = context.createPendingResult(REQ_CODE_NFC_TAG_FOUND, new Intent(), 0);
                if (pi != null) {
                    try {
                        mNfcAdapter.enableForegroundDispatch(
                                context,
                                pi,
                                new IntentFilter[]{
                                        new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
                                },
                                new String[][]{
                                        new String[]{"android.nfc.tech.NfcV"}
                                });
                        foreground_enabled = true;
                    } catch (NullPointerException e) {
                        //
                    }
                }
            }
        }


    }

    public static synchronized void doTheScan(final Activity context, Tag tag, boolean showui) {
        synchronized (tag_lock) {
            if (!tag_discovered) {
                if (!useNFC()) return;
                if ((!last_read_succeeded) && (JoH.ratelimit("nfc-debounce", 5)) || (JoH.ratelimit("nfc-debounce", 30))) {
                    tag_discovered = true;
                    Home.staticBlockUI(context, true);
                    last_tag_discovered = JoH.tsl();
                    if (showui) {
                        context.startActivity(new Intent(context, NFCScanningX.class));
                    } else {
                        NFCReaderX.vibrate(context, 0);
                        JoH.static_toast_short(gs(R.string.scanning));
                    }
                    if (d)
                        Log.d(TAG, "NFC tag discovered - going to read data");
                    new NfcVReaderTask(context).executeOnExecutor(xdrip.executor, tag);
                } else {
                    if (JoH.tsl() - last_tag_discovered > 5000) {
                        vibrate(context, 4);
                        JoH.static_toast_short(gs(R.string.not_so_quickly_wait_30_seconds));
                    }
                }
            } else {
                Log.d(TAG, "Tag already discovered!");
                if (JoH.tsl() - last_tag_discovered > 30000)
                    tag_discovered = false; // don't lock too long
            }
        } // lock
    }


    // via intents
    public static void tagFound(Activity context, Intent data) {

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(data.getAction())) {
            Tag tag = data.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            doTheScan(context, tag, true);
        }
    }
    
        public static void sendLibrereadingToFollowers(final String tagId, byte[] data1, final long CaptureDateTime, byte[] patchUid, byte[] patchInfo) {
        if (!Home.get_master()) {
            return;
        }
        LibreBlock libreBlock = LibreBlock.getForTimestamp(CaptureDateTime);
        if (libreBlock != null) {
            // We already have this one, so we have already sent it, so let's not crate storms.
            return;
        }
        // Create the object to send
        libreBlock = LibreBlock.create(tagId, CaptureDateTime, data1, 0, patchUid, patchInfo);
        if (libreBlock == null) {
            Log.e(TAG, "Error could not create libreBlock for libre-allhouse");
            return;
        }
        final String json = libreBlock.toExtendedJson();

        GcmActivity.pushLibreBlock(json);

    }

    public static boolean HandleGoodReading(String tagId, byte[] data1, final long CaptureDateTime) {
        return HandleGoodReading(tagId, data1, CaptureDateTime, false, null, null);
    }

    public static boolean HandleGoodReading(final String tagId, byte[] data1, final long CaptureDateTime, final boolean allowUpload, byte[] patchUid, byte[] patchInfo) {
        return HandleGoodReading(tagId, data1, CaptureDateTime, allowUpload, patchUid, patchInfo, false, null, null);
    }


    // returns true if checksum passed.
    public static boolean HandleGoodReading(final String tagId, byte[] data1, final long CaptureDateTime, final boolean allowUpload, byte[] patchUid, byte[] patchInfo,
        boolean decripted_data, int[] trend_bg_vals, int[] history_bg_vals) {
            Log.e(TAG, "HandleGoodReading called dat1 len = " + data1.length);
            if (data1.length > Constants.LIBRE_1_2_FRAM_SIZE) {
                // It seems that some times we read a buffer that is bigger than 0x158, but we should only use the first 0x158 bytes.
                data1 = java.util.Arrays.copyOfRange(data1, 0, Constants.LIBRE_1_2_FRAM_SIZE);
            }

            Log.e(TAG, "HANDLE GOOD!!!!");
            if (LibreOOPAlgorithm.isDecodeableData(patchInfo) && decripted_data == false
                    && !Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
                // Send to OOP2 for decryption.
                LibreOOPAlgorithm.logIfOOP2NotAlive();
                Log.e(TAG, "WILL TRY TO SEND!!!!");
                LibreOOPAlgorithm.sendData(data1, CaptureDateTime, patchUid, patchInfo, tagId);
                return true;
        }

        sendLibrereadingToFollowers(tagId, data1, CaptureDateTime, patchUid, patchInfo);

if (Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            // If oop is used, there is no need to  do the checksum It will be done by the oop.
            // (or actually we don't know how to do it, for us 14/de sensors).
            // Save raw block record (we start from block 0)
            LibreBlock.createAndSave(tagId, CaptureDateTime, data1, 0, allowUpload, patchUid, patchInfo);
            LibreOOPAlgorithm.sendData(data1, CaptureDateTime, patchUid, patchInfo, tagId);
        } else {
            final boolean checksum_ok = LibreUtils.verify(data1, patchInfo);
            if (!checksum_ok) {
                Log.e(TAG, "bad cs");
                return false;
            }
            
            // The 4'th byte is where the sensor status is (for libre1 libre2 and libre pro).
            if(!LibreUtils.isSensorReady(data1[4])) {
                Log.e(TAG, "Sensor is not ready, Ignoring reading!");
                return true;
            }
            
            final ReadingData mResult = parseData(data1, patchInfo, CaptureDateTime, trend_bg_vals, history_bg_vals);
            new Thread() {
                @Override
                public void run() {
                    final PowerManager.WakeLock wl = JoH.getWakeLock("processTransferObject", 60000);
                    try {
                        // Protect against wifi reader and gmc reader coming at the same time.
                        synchronized (NFCReaderX.class) {
                            if (mResult != null) {
                                boolean bg_val_exists = trend_bg_vals != null && history_bg_vals != null;
                                LibreAlarmReceiver.processReadingDataTransferObject(mResult, CaptureDateTime, tagId, allowUpload, patchUid, patchInfo, bg_val_exists);
                        Home.staticRefreshBGCharts();
                            }
                        }
                    } finally {
                        JoH.releaseWakeLock(wl);
                    }
                }
            }.start();
        }
        return true; // Checksum tests have passed.
    }

    private static class NfcVReaderTask extends AsyncTask<Tag, Void, Tag> {

        static ENABLE_BLUETOOTH_SET readEnableBluetoothAllowed(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String enable_streaming = prefs.getString("libre2_enable_bluetooth_streaming", "enable_streaming_ask");
            switch (enable_streaming) {
                case "enable_streaming_always":
                    return ENABLE_BLUETOOTH_SET.ALWAYS_ALLOW;
                case "enable_streaming_never":
                    return ENABLE_BLUETOOTH_SET.NEVER_ALLOW;
                case "enable_streaming_ask":
                    return ENABLE_BLUETOOTH_SET.ASK;

            }
            Log.e(TAG, "libre2_enable_bluetooth_streaming bad value - not connecting" + enable_streaming);
            return ENABLE_BLUETOOTH_SET.NEVER_ALLOW;
        }

        static boolean enableBluetoothAllowed(Context context) {
            enable_bluetooth_ask_user = false;
            ENABLE_BLUETOOTH_SET ebs = readEnableBluetoothAllowed(context);
            switch (ebs) {
                case NEVER_ALLOW:
                    return false;
                case ALWAYS_ALLOW:
                    return true;
                default:
                    // act based on elapsed time,
            }
            if (JoH.msSince(Pref.getLong(ENABLE_BLUETOOTH_TIMESTAMP, 0)) < 2 * MINUTE) {
                return true;
            }
            // We still don't know what to do, so returning false, but we will ask the user.
            enable_bluetooth_ask_user = true;
            return false;
        }


        Activity context;
        boolean succeeded = false;

        public NfcVReaderTask(Activity context) {
            this.context = context;
            last_read_succeeded = false;
            JoH.ratelimit("nfc-debounce", 1); // ping the timer
        }

        private byte[] data = new byte[360];
        private byte[] patchInfo = null;


        @Override
        protected void onPostExecute(Tag tag) {
        	Log.d(TAG, "onPostExecute called");
            try {
                if (tag == null) return;
                if (!NFCReaderX.useNFC()) return;
                if (succeeded) {
                    long now = JoH.tsl();
                    String SensorSn = LibreUtils.decodeSerialNumberKey(tag.getId());

                    if (SensorSanity.checkLibreSensorChangeIfEnabled(SensorSn)) {
                        Log.e(TAG, "Problem with Libre Serial Number - not processing");
                        return;
                    }
                    // Set the time of the current reading
                    PersistentStore.setLong("libre-reading-timestamp", JoH.tsl());

                    boolean checksum_ok;
                    if (use_fake_de_data()) {
                        checksum_ok = HandleGoodReading(SensorSn, de_new_packet, now, false, de_new_patch_uid, de_new_patch_info);
                    } else {
                        checksum_ok = HandleGoodReading(SensorSn, data, now, false, tag.getId(), patchInfo);
                    }
                    if(checksum_ok == false) {
                        Log.e(TAG, "Read data but checksum is wrong");
                    }
                    PersistentStore.setString("LibreSN", SensorSn);
                } else {
                    Log.d(TAG, "Scan did not succeed so ignoring buffer");
                }
                Log.d(TAG, "calling startHomeWithExtra");
                if (enable_bluetooth_ask_user) {
                    Home.startHomeWithExtra(context, Home.ENABLE_STREAMING_DIALOG, "");
                } else {
                Home.startHomeWithExtra(context, null, null);
                }

            } catch (IllegalStateException e) {
                Log.e(TAG, "Illegal state exception in postExecute: " + e);

            } finally {
                tag_discovered = false; // right place?
                Home.staticBlockUI(context, false);
            }
        }
        
        void LogLibre2StartStreaming(ENABLE_STREAMING success, String extraData) {
            switch (success) {
                case SUCCESS:
                    UserError.Log.ueh("Libre 2", "Bluetooth connection with sensor enabled, you should be able to receive BG data using Bluetooth");
                    break;
                case FAILED:
                    UserError.Log.ueh("Libre 2", "Bluetooth connection with sensor failed." +
                            " You wont get any readings. Try scanning again to fix this. (Extra data: " +
                            extraData + " )");
                    break;
            }
        }

        void startLibre2Streaming(NfcV nfcvTag, byte[] patchUid, byte[] patchInfo) throws InterruptedException {
            // Since this is libre2 we can remove the physical devices battery.
            Pref.setInt("bridge_battery", 0);
            PersistentStore.setString("Tomatobattery", "0");
            PersistentStore.setString("Bubblebattery", "0");
            if (!enableBluetoothAllowed(context)) {
                Log.e(TAG, "Sensor is libre 2, enabeling BT not allowed");
                LogLibre2StartStreaming(ENABLE_STREAMING.FAILED, "Sensor is Libre 2 but enabling BT is not allowed by the settings");
                return;
            }
            Log.e(TAG, "Sensor is libre 2, enabeling BT");

            String SensorSN = LibreUtils.decodeSerialNumberKey(patchUid);

            Libre2SensorData.setLibre2SensorData(patchUid, patchInfo, 42, 1, "");
            // This is the nfc command to enable streaming
            Pair<byte[], String> unlockData = LibreOOPAlgorithm.nfcSendgetBluetoothEnablePayload();
            if (unlockData == null) {
                Log.e(TAG, "unlockData is null, not enabeling streaming");
                LogLibre2StartStreaming(ENABLE_STREAMING.FAILED, "Failure in communicating with OOP2. Is OOP2 installed?");
                return;
            }
            Libre2SensorData.setLibre2SensorData(patchUid, patchInfo, 42, 1, unlockData.second);
            byte[] nfc_command = unlockData.first;

            final byte[] cmd = new byte[]{0x02, (byte) 0xa1, 0x07};
            final byte[] full_cmd = new byte[cmd.length + nfc_command.length];
            System.arraycopy(cmd, 0, full_cmd, 0, cmd.length);
            System.arraycopy(nfc_command, 0, full_cmd, cmd.length, nfc_command.length);

            Log.e(TAG, "nfc_command to enable streaming = " + HexDump.dumpHexString(full_cmd));

            Long time_patch = System.currentTimeMillis();
            byte[] res = null;
            while (true) {
                try {
                    if (use_fake_de_data()) {
                        // DC:A6:32:0F:4F:92
                        res = new byte[]{(byte) 0x12, (byte) 0x92, (byte) 0x4f, (byte) 0x0f, (byte) 0x32, (byte) 0xa6, (byte) 0xdc};
                    } else {
                        res = nfcvTag.transceive(full_cmd);
                    }
                    Log.e(TAG, "enable streaming command returned: " + HexDump.dumpHexString(res));
                    break;
                } catch (IOException e) {
                    if ((System.currentTimeMillis() > time_patch + 2000)) {
                        Log.e(TAG, "enable streaming command read timeout");
                        LogLibre2StartStreaming(ENABLE_STREAMING.FAILED, "Enable streaming command read timeout");
                        JoH.static_toast_short(gs(R.string.nfc_read_timeout));
                        vibrate(context, 3);
                        return;
                    }
                    Thread.sleep(100);
                }
            }
            if (res.length == 7) {
                // The mac addresses of the device is the returned data, after removing the first byte, and reversing it.
                res = Arrays.copyOfRange(res, 1, res.length);
                res = JoH.reverseBytes(res);

                ActiveBluetoothDevice.setDevice(LibreOOPAlgorithm.getLibreDeviceName() + SensorSN, JoH.bytesToHexMacFormat(res));
                CollectionServiceStarter.restartCollectionServiceBackground();
                LogLibre2StartStreaming(ENABLE_STREAMING.SUCCESS, null);
            } else {
                Log.e(TAG, "enable streaming returned bad data. BT will not work." + HexDump.dumpHexString(res));
                LogLibre2StartStreaming(ENABLE_STREAMING.FAILED, "Enable streaming command returned bad data");
            }
        }

        @Override
        protected Tag doInBackground(Tag... params) {
            if (!NFCReaderX.useNFC()) return null;
            if (read_lock.tryLock()) {

                try {
                    Tag tag = params[0];
                    // try {
                    //     Thread.sleep(50);
                    //  } catch (InterruptedException e) {
                    //      //
                    //  }
                    NfcV nfcvTag = NfcV.get(tag);
                    if (d) Log.d(TAG, "Attempting to read tag data");
                    try {
                        //boolean connected = false;
                        try {
                            nfcvTag.connect();
                        } catch (IOException e) {
                            Log.d(TAG, "Trying second nfc connect");
                            Thread.sleep(250);
                            nfcvTag.connect();
                        }
                        final byte[] uid = tag.getId();

                        try {
                            final byte[] diag = JoH.hexStringToByteArray(Pref.getStringDefaultBlank("nfc_test_diagnostic"));
                            if ((diag != null) && (diag.length > 0)) {
                                Log.d(TAG, "Diagnostic ->: " + HexDump.dumpHexString(diag, 0, diag.length).trim() + " len: " + diag.length);
                                Long time = System.currentTimeMillis();
                                byte[] replyBlock;
                                while (true) {
                                    try {
                                        replyBlock = nfcvTag.transceive(diag);
                                        break;
                                    } catch (IOException e) {
                                        if ((System.currentTimeMillis() > time + 2000)) {
                                            Log.e(TAG, "tag diagnostic read timeout");
                                            JoH.static_toast_short(gs(R.string.nfc_diag_timeout));
                                            vibrate(context, 3);
                                            return null;
                                        }
                                        Thread.sleep(100);
                                    }
                                }
                                Log.d(TAG, "Diagnostic <-: " + HexDump.dumpHexString(replyBlock, 0, replyBlock.length).trim() + " len: " + replyBlock.length);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Exception in NFC Diagnostic: " + e);
                            Pref.setString("nfc_test_diagnostic", "");
                        }

                        final boolean multiblock = Pref.getBoolean("use_nfc_multiblock", true);
                        boolean addressed = !Pref.getBoolean("use_nfc_any_tag", true);
                        // if multiblock mode
                        JoH.benchmark(null);

                        Long time_patch = System.currentTimeMillis();
                        while (true) {
                            try {
                                
                                final byte[] cmd = new byte[] {0x02, (byte)0xa1, 0x07};
                                patchInfo = nfcvTag.transceive(cmd);
                                if(patchInfo != null) {
                                    // We need to throw away the first byte.
                                    patchInfo = Arrays.copyOfRange(patchInfo, 1, patchInfo.length);
                                }
                                break;
                            } catch (IOException e) {
                                if ((System.currentTimeMillis() > time_patch + 2000)) {
                                    Log.e(TAG, "patchInfo tag read timeout");
                                    JoH.static_toast_short(gs(R.string.nfc_read_timeout));
                                    vibrate(context, 3);
                                    return null;
                                }
                                Thread.sleep(100);
                            }
                        }
                        Log.d(TAG, "patchInfo = " + HexDump.dumpHexString(patchInfo));
                        byte[] patchUid = tag.getId();
                        Log.d(TAG, "patchUid = " + HexDump.dumpHexString(patchUid));
                        if (use_fake_de_data()) {
                            patchUid = de_new_patch_uid;
                            patchInfo = de_new_patch_info;
                        }

                        SensorType sensorType = LibreOOPAlgorithm.getSensorType(patchInfo);
                        Log.uel(TAG, "Libre sensor of type " + sensorType.name() + " detected.");
                        if (addressed && sensorType != SensorType.Libre1 && sensorType != SensorType.Libre1New) {
                            Log.d(TAG, "Not using addressed mode since not a libre 1 sensor");
                            addressed = false;
                        }
                        if (sensorType == SensorType.Libre2) {
                            startLibre2Streaming(nfcvTag, patchUid, patchInfo);
                            PersistentStore.setString("LibreVersion", "2");
                        } else {
                            PersistentStore.setString("LibreVersion", "1");
                        }
                        
                        if (multiblock) {
                            Log.e(TAG, "starting multiple block reads");
                            for (int i = 0; i < 43; i = i + 3) {
                                int read_blocks = 3;
                                int correct_reply_size = addressed ? 28 : 25;
                                if (i == 42 && sensorType == SensorType.Libre2) {
                                    read_blocks = 1;
                                    correct_reply_size = 9;
                                }

                                final byte[] cmd;
                                if (addressed) {
                                    cmd = new byte[]{0x60, 0x23, 0, 0, 0, 0, 0, 0, 0, 0, (byte) i, (byte) (read_blocks - 1)};
                                    System.arraycopy(uid, 0, cmd, 2, 8);
                                } else {
                                    cmd = new byte[]{0x02, 0x23, (byte) i, (byte) (read_blocks - 1)};
                                }

                                byte[] replyBlock;
                                Long time = System.currentTimeMillis();
                                while (true) {
                                    try {
                                        replyBlock = nfcvTag.transceive(cmd);
                                        break;
                                    } catch (IOException e) {
                                        if ((System.currentTimeMillis() > time + 2000)) {
                                            Log.e(TAG, "tag read timeout");
                                            JoH.static_toast_short(gs(R.string.nfc_read_timeout));
                                            vibrate(context, 3);
                                            return null;
                                        }
                                        Thread.sleep(100);
                                    }
                                }

                                if (d)
                                    Log.d(TAG, "Received multiblock reply, offset: " + i + " sized: " + replyBlock.length);
                                if (d)
                                    Log.d(TAG, HexDump.dumpHexString(replyBlock, 0, replyBlock.length));
                                if (replyBlock.length != correct_reply_size) {
                                    Log.e(TAG, "Incorrect block size (multiply): " + replyBlock.length + " vs " + correct_reply_size);
                                    JoH.static_toast_short(gs(R.string.nfc_invalid_data__try_again));
                                    if (!addressed) {
                                        if (PersistentStore.incrementLong("nfc-address-failures") > 2) {
                                            Pref.setBoolean("use_nfc_any_tag", false);
                                            JoH.static_toast_short(gs(R.string.turned_off_anytag_feature));
                                        }
                                    }
                                    vibrate(context, 3);
                                    return null;
                                }
                                if (addressed) {
                                    for (int j = 0; j < read_blocks; j++) {
                                        System.arraycopy(replyBlock, 2 + (j * 9), data, i * 8 + (j * 8), 8);
                                    }
                                } else {
                                    System.arraycopy(replyBlock, 1, data, i * 8, replyBlock.length - 1);
                                }
                            }
                        } else {
                            // always addressed
                            Log.e(TAG, "starting single blobk reads");
                            int correct_reply_size;
                            for (int i = 0; i < 43; i++) {
                                final byte[] cmd;
                                int startBlock;
                                if (addressed) {
                                    cmd = new byte[]{0x60, 0x20, 0, 0, 0, 0, 0, 0, 0, 0, (byte) i, 0};
                                System.arraycopy(uid, 0, cmd, 2, 8);
                                    correct_reply_size = 10;
                                    startBlock = 2;
                                } else {
                                    cmd = new byte[]{(byte) 0x02, (byte) 0x23, 0, (byte) 0x0};
                                    correct_reply_size = 9;
                                    startBlock = 1;
                                }
                                cmd[2] = (byte) i;

                                byte[] oneBlock;
                                Long time = System.currentTimeMillis();
                                while (true) {
                                    try {
                                        Log.e(TAG, "sending command " + HexDump.toHexString(cmd));
                                        oneBlock = nfcvTag.transceive(cmd);
                                        break;
                                    } catch (IOException e) {
                                        if ((System.currentTimeMillis() > time + 2000)) {
                                            Log.e(TAG, "tag read timeout");
                                            JoH.static_toast_short(gs(R.string.nfc_read_timeout));
                                            vibrate(context, 3);
                                            return null;
                                        }
                                        Thread.sleep(100);
                                    }
                                }
                                if (d)
                                    Log.e(TAG, HexDump.dumpHexString(oneBlock, 0, oneBlock.length));
                                if (oneBlock.length != correct_reply_size) {
                                    Log.e(TAG, "Incorrect block size: " + oneBlock.length + " vs " + correct_reply_size);
                                    JoH.static_toast_short(gs(R.string.nfc_invalid_data));
                                    vibrate(context, 3);
                                    return null;
                                }
                                System.arraycopy(oneBlock, startBlock, data, i * 8, 8);
                            }
                        }
                        JoH.benchmark("Tag read");
                        Log.d(TAG, "GOT TAG DATA!\n" + HexDump.toHexString(data));
                        last_read_succeeded = true;
                        succeeded = true;
                        used_nfc_successfully = true;
                        vibrate(context, 1);
                        JoH.static_toast_short(gs(R.string.scanned_ok));
                        PersistentStore.setLongZeroIfSet("nfc-address-failures");

                    } catch (IOException e) {
                        JoH.static_toast_short(gs(R.string.nfc_io_error));
                        vibrate(context, 3);
                    } catch (Exception e) {
                        Log.e(TAG, "Got exception reading nfc in background: ", e);
                        return null;
                    } finally {
                        try {
                            nfcvTag.close();
                        } catch (Exception e) {
                            Log.e(TAG, "Error closing tag!");
                            JoH.static_toast_short(gs(R.string.nfc_error));
                            vibrate(context, 3);
                        }
                    }
                    if (d) Log.d(TAG, "Tag data reader exiting");
                    return tag;
                } finally {
                    read_lock.unlock();
                    Home.staticBlockUI(context, false);
                }
            } else {
                Log.d(TAG, "Already read_locked! - skipping");
                return null;
            }

        }

    }

    public static boolean verifyTime(long time, String caller, byte[] extra_data) {
        if ((time < 0) || time >= LibreTrendUtil.MAX_POINTS) {
            // This is an illegal value
            Log.e(TAG, "We have an illegal time at " + caller + " " + time + JoH.bytesToHex(extra_data));
            return false;
        }
        return true;
    }


    // Get the history data for libre1/2
    private static ArrayList<GlucoseData> parseHistoryData(byte[] data, int sensorTime, long sensorStartTime, Long CaptureDateTime) {
        ArrayList<GlucoseData> historyList = new ArrayList<>();
        int indexHistory = data[27] & 0xFF;
        // loads history values (ring buffer, starting at index_trent. byte 124-315)
        for (int index = 0; index < 32; index++) {
            int i = indexHistory - index - 1;
            if (i < 0) i += 32;
            GlucoseData glucoseData = new GlucoseData();

            // If the data is decoded for some reason, we might have a wrong index.
            // The 6 is because we read up to 6 bytes.
            if (i * FRAM_RECORD_SIZE + HISTORY_START + FRAM_RECORD_SIZE >= data.length) {
                Log.e(TAG, "Failing to parse data from " + JoH.dateTimeText(CaptureDateTime));
                return null;
            }

            glucoseData.glucoseLevelRaw =
                    getGlucoseRaw(new byte[]{data[(i * FRAM_RECORD_SIZE + HISTORY_START + 1)], data[(i * FRAM_RECORD_SIZE + HISTORY_START)]});
            glucoseData.flags = LibreOOPAlgorithm.readBits(data, i * FRAM_RECORD_SIZE + HISTORY_START, 0xe, 0xc);
            glucoseData.temp = LibreOOPAlgorithm.readBits(data, i * FRAM_RECORD_SIZE + HISTORY_START, 0x1a, 0xc);
            glucoseData.source = GlucoseData.DataSource.FRAM;

            int time = Math.max(0, Math.abs((sensorTime - 3) / 15) * 15 - index * 15);

            glucoseData.realDate = sensorStartTime + time * MINUTE;
            glucoseData.sensorTime = time;
            if (verifyTime(time, "parseData history", data)) {
            historyList.add(glucoseData);
        }
        }
        return historyList;
    }

    // Get the trend data for libre1/2
    private static ArrayList<GlucoseData> parseTrendData(byte[] data, int sensorTime, long sensorStartTime, Long CaptureDateTime) {
        ArrayList<GlucoseData> trendList = new ArrayList<>();
        int indexTrend = data[26] & 0xFF;

        // loads trend values (ring buffer, starting at index_trent. byte 28-123)
        for (int index = 0; index < 16; index++) {
            int i = indexTrend - index - 1;
            if (i < 0) i += 16;
            GlucoseData glucoseData = new GlucoseData();
            if (i * FRAM_RECORD_SIZE + TREND_START + FRAM_RECORD_SIZE >= data.length) {
                Log.e(TAG, "Failing to parse data from " + JoH.dateTimeText(CaptureDateTime));
                return null;
            }
            glucoseData.glucoseLevelRaw =
                    getGlucoseRaw(new byte[]{data[(i * FRAM_RECORD_SIZE + TREND_START + 1)], data[(i * FRAM_RECORD_SIZE + TREND_START)]});
            glucoseData.flags = LibreOOPAlgorithm.readBits(data, i * FRAM_RECORD_SIZE + TREND_START, 0xe, 0xc);
            glucoseData.temp = LibreOOPAlgorithm.readBits(data, i * FRAM_RECORD_SIZE + TREND_START, 0x1a, 0xc);
            glucoseData.source = GlucoseData.DataSource.FRAM;
            int time = Math.max(0, sensorTime - index);

            glucoseData.realDate = sensorStartTime + time * MINUTE;
            glucoseData.sensorTime = time;
            if (verifyTime(time, "parseData trendList", data)) {
                trendList.add(glucoseData);
            }
        }
        return trendList;
    }


    private static ArrayList<GlucoseData> parseTrendDataLibrePro(byte[] data, int sensorTime, long sensorStartTime, Long CaptureDateTime) {
        int sensorMinutesElapse = 256 * (data[LPRO_SENSORMINUTES + 1] & 0xFF) + (data[LPRO_SENSORMINUTES] & 0xFF);

        byte trendPointer = data[LPRO_TRENDPOINTER];

        if (trendPointer == 0)
            trendPointer = 0x0F;
        else
            trendPointer -= 1;
        int trendPos = LPRO_TRENDOFFSET + trendPointer * 6;

        ArrayList<GlucoseData> trendList = new ArrayList<>();
        GlucoseData glucoseData = new GlucoseData();

        glucoseData.glucoseLevelRaw =
                getGlucoseRaw(new byte[]{data[trendPos + 1], data[trendPos]});
        glucoseData.flags = 0;//???
        glucoseData.temp = 0;//????
        glucoseData.source = GlucoseData.DataSource.FRAM;

        glucoseData.realDate = CaptureDateTime;
        glucoseData.sensorTime = sensorMinutesElapse;
        if (verifyTime(sensorMinutesElapse, "parseData trendList", data)) {
            trendList.add(glucoseData);
        }
        Log.e(TAG, "Creating librepro data  " + glucoseData);
        return trendList;
    }

    // Sensor structure is described at  https://github.com/UPetersen/LibreMonitor/wiki
    public static ReadingData parseData(byte[] data, byte[] patchInfo, Long CaptureDateTime, int[] trend_bg_vals, int[] history_bg_vals) {
        final int sensorTime = 256 * (data[317] & 0xFF) + (data[316] & 0xFF);
        LibreOOPAlgorithm.SensorType sensorType = LibreOOPAlgorithm.getSensorType(patchInfo);

        long sensorStartTime = CaptureDateTime - sensorTime * MINUTE;

        ArrayList<GlucoseData> historyList;
        if (sensorType != LibreOOPAlgorithm.SensorType.LibreProH) {
            historyList = parseHistoryData(data, sensorTime, sensorStartTime, CaptureDateTime);
        } else {
            historyList = new ArrayList<GlucoseData>();
        }

        ArrayList<GlucoseData> trendList;
        if (sensorType != LibreOOPAlgorithm.SensorType.LibreProH) {
            trendList = parseTrendData(data, sensorTime, sensorStartTime, CaptureDateTime);
        } else {
            trendList = parseTrendDataLibrePro(data, sensorTime, sensorStartTime, CaptureDateTime);
        }
        if(trendList == null || historyList == null) {
            Log.e(TAG,"Failed parsing trendList or historyList");
            return null;
        }
        Collections.sort(trendList);
        Collections.sort(historyList);
        // Adding the bg vals must be done after the sort.
        if (trend_bg_vals != null && trend_bg_vals.length == trendList.size() && history_bg_vals != null
                && history_bg_vals.length == historyList.size()) {
            for (int i = 0; i < trend_bg_vals.length; i++) {
                trendList.get(i).glucoseLevel = trend_bg_vals[i];
                Log.e(TAG, "Adding bg val for trend at time " + trendList.get(i).sensorTime + " val =  " + trend_bg_vals[i]);
            }
            for (int i = 0; i < history_bg_vals.length; i++) {
                historyList.get(i).glucoseLevel = history_bg_vals[i];
                Log.e(TAG, "Adding bg val for history at time " + historyList.get(i).sensorTime + " val =  " + history_bg_vals[i]);
            }
        }

        final ReadingData readingData = new ReadingData(trendList, historyList);
        readingData.raw_data = data;
        return readingData;
    }


    private static int getGlucoseRaw(byte[] bytes) {
            return ((256 * (bytes[0] & 0xFF) + (bytes[1] & 0xFF)) & 0x1FFF);
    }

    public static void vibrate(Context context, int pattern) {

        // 0 = scanning
        // 1 = scan ok
        // 2 = warning
        // 3 = lesser error

        final long[][] patterns = {{0, 150}, {0, 150, 70, 150}, {0, 2000}, {0, 1000}, {0, 100}};

        if (Pref.getBooleanDefaultFalse("nfc_scan_vibrate")) {
            final Vibrator vibrate = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if ((vibrate == null) || (!vibrate.hasVibrator())) return;
            vibrate.cancel();
            if (d) Log.d(TAG, "About to vibrate, pattern: " + pattern);
            try {
                vibrate.vibrate(patterns[pattern], -1);
            } catch (Exception e) {
                Log.d(TAG, "Exception in vibrate: " + e);
            }
        }
    }

    public static void handleHomeScreenScanPreference(Context context) {
        handleHomeScreenScanPreference(context, useNFC() && Pref.getBooleanDefaultFalse("nfc_scan_homescreen"));
    }

    public static void handleHomeScreenScanPreference(Context context, boolean state) {
        try {
            Log.d(TAG, "HomeScreen Scan State: " + (state ? "enable" : "disable"));
            context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, NFCFilterX.class),
                    state ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        } catch (Exception e) {
            Log.wtf(TAG, "Exception in handleHomeScreenScanPreference: " + e);
        }
    }

    public static synchronized void scanFromActivity(final Activity context, final Intent intent) {
        if (NFCReaderX.useNFC()) {
            // sanity checking is in onward function
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        context.finish();
                    } catch (Exception e) {
                        //
                    }
                }
            }, 10000);
            if (JoH.ratelimit("nfc-filterx", 5)) {
                NFCReaderX.vibrate(context, 0);

                NFCReaderX.tagFound(context, intent);
            } else {
                Log.e(TAG, "Rate limited start nfc-filterx");
            }
        } else {
            context.finish();
        }
    }

    public static void windowFocusChange(final Activity context, boolean hasFocus, View decorView) {
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= 19) {
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        } else {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        context.finish();
                    } catch (Exception e) {
                        //
                    }
                }
            }, 1000);
        }
    }
    
    static public List<GlucoseData> getLibreTrend(LibreBlock libreBlock) {
        if (libreBlock.byte_start != 0) {
            Log.i(TAG, "libreBlock does not start with 0, don't know how to parse it " + libreBlock.timestamp);
            return null;
        }
        List<GlucoseData> result;
        if (libreBlock.byte_end == Constants.LIBRE_1_2_FRAM_SIZE) {
            ReadingData reading_data = parseData(libreBlock.blockbytes, libreBlock.patchInfo, libreBlock.timestamp, null, null);
            if (reading_data == null) {
                return null;
            }
            result = reading_data.trend;
        } else if (libreBlock.byte_end == 44) {
            // This is the libre2 ble data
            result = LibreOOPAlgorithm.parseBleDataPerMinute(libreBlock.blockbytes, null, libreBlock.timestamp);
        } else {
            Log.i(TAG, "libreBlock exists but size is " + libreBlock.byte_end + " don't know how to parse it " + libreBlock.timestamp);
            return null;
        }
        if (result.size() == 0) {
            Log.i(TAG, "libreBlock exists but no trend data exists, or first value is zero " + libreBlock.timestamp);
            return null;
        }
        return result;
    }
        
    enum ENABLE_STREAMING {
        SUCCESS,
        FAILED,
    }
    
}
