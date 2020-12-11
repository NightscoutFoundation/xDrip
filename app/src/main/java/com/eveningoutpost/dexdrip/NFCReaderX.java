package com.eveningoutpost.dexdrip;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.support.v7.preference.PreferenceManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.GlucoseData;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Libre2SensorData;
import com.eveningoutpost.dexdrip.Models.LibreBlock;
import com.eveningoutpost.dexdrip.Models.LibreOOPAlgorithm;
import com.eveningoutpost.dexdrip.Models.ReadingData;
import com.eveningoutpost.dexdrip.Models.SensorSanity;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.LibreUtils;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.WholeHouse;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import com.eveningoutpost.dexdrip.Models.LibreOOPAlgorithm.SensorType;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.BaseTx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

    
    // For libre2 emulation only
    static final byte []de_new_packet = {(byte)0x36, (byte)0x3c, (byte)0x68, (byte)0x7b, (byte)0x5a, (byte)0xb9, (byte)0x74, (byte)0xba, (byte)0xd4, (byte)0x34, (byte)0xb8, (byte)0x0d, (byte)0xde, (byte)0xbb, (byte)0x70, (byte)0xd8, (byte)0x25, (byte)0xf3, (byte)0xd0, (byte)0xee, (byte)0xa2, (byte)0x4e, (byte)0xba, (byte)0xe7, (byte)0x61, (byte)0x4d, (byte)0x0d, (byte)0x86, (byte)0xf5, (byte)0x2b, (byte)0x8f, (byte)0x09, (byte)0xe9, (byte)0x71, (byte)0x3d, (byte)0x88, (byte)0x4c, (byte)0x3f, (byte)0x96, (byte)0x53, (byte)0x33, (byte)0xe7, (byte)0x31, (byte)0x19, (byte)0x9b, (byte)0xf3, (byte)0xa1, (byte)0x5f, (byte)0x25, (byte)0x21, (byte)0x00, (byte)0x06, (byte)0xae, (byte)0x00, (byte)0x60, (byte)0x16, (byte)0x3b, (byte)0x79, (byte)0x9e, (byte)0x76, (byte)0x4c, (byte)0xc2, (byte)0x0c, (byte)0x2c, (byte)0xbe, (byte)0x03, (byte)0x9c, (byte)0x71, (byte)0x9e, (byte)0xd3, (byte)0xde, (byte)0xb6, (byte)0xd4, (byte)0x4e, (byte)0x27, (byte)0xfd, (byte)0x90, (byte)0xfd, (byte)0x3b, (byte)0x24, (byte)0xb2, (byte)0xed, (byte)0x5b, (byte)0x1a, (byte)0xa3, (byte)0x48, (byte)0x66, (byte)0x4f, (byte)0xd6, (byte)0xb5, (byte)0x03, (byte)0x50, (byte)0xc7, (byte)0x6c, (byte)0x27, (byte)0x01, (byte)0xe2, (byte)0xcc, (byte)0x16, (byte)0x3a, (byte)0x2c, (byte)0x13, (byte)0x77, (byte)0x69, (byte)0xd0, (byte)0x17, (byte)0xd6, (byte)0x57, (byte)0xb8, (byte)0x32, (byte)0x8b, (byte)0x8f, (byte)0x09, (byte)0xd5, (byte)0xef, (byte)0xf9, (byte)0x9c, (byte)0xfb, (byte)0x4b, (byte)0xb5, (byte)0x31, (byte)0xd8, (byte)0x6c, (byte)0xc7, (byte)0x2b, (byte)0x98, (byte)0xec, (byte)0xca, (byte)0x04, (byte)0x75, (byte)0x87, (byte)0x44, (byte)0x72, (byte)0x76, (byte)0x5d, (byte)0xe4, (byte)0xf2, (byte)0xc5, (byte)0x6a, (byte)0x64, (byte)0xea, (byte)0xac, (byte)0xd0, (byte)0x02, (byte)0xeb, (byte)0x7c, (byte)0x1f, (byte)0x4b, (byte)0x01, (byte)0xdf, (byte)0x8c, (byte)0xa9, (byte)0xf1, (byte)0x5c, (byte)0x8f, (byte)0xb5, (byte)0x7a, (byte)0xed, (byte)0xe1, (byte)0x73, (byte)0x08, (byte)0x18, (byte)0xc2, (byte)0xd7, (byte)0x24, (byte)0x62, (byte)0x35, (byte)0xfd, (byte)0x37, (byte)0x32, (byte)0x5f, (byte)0xaf, (byte)0x1e, (byte)0x72, (byte)0xe8, (byte)0x2b, (byte)0x9e, (byte)0x45, (byte)0xe8, (byte)0x44, (byte)0x8b, (byte)0xfb, (byte)0x7a, (byte)0xc0, (byte)0xd8, (byte)0x11, (byte)0xb7, (byte)0x42, (byte)0x2f, (byte)0xef, (byte)0x34, (byte)0x82, (byte)0xaa, (byte)0x14, (byte)0xf1, (byte)0xbb, (byte)0x2a, (byte)0x5d, (byte)0xb9, (byte)0x34, (byte)0xee, (byte)0x4c, (byte)0x9d, (byte)0xaa, (byte)0xcb, (byte)0x9c, (byte)0x22, (byte)0xd6, (byte)0xe1, (byte)0x8d, (byte)0xf5, (byte)0xca, (byte)0xac, (byte)0x6d, (byte)0xf2, (byte)0xef, (byte)0x03, (byte)0xaf, (byte)0x73, (byte)0x38, (byte)0xad, (byte)0x88, (byte)0x87, (byte)0x3b, (byte)0xdf, (byte)0xe2, (byte)0xfd, (byte)0x6f, (byte)0x23, (byte)0x0f, (byte)0x6e, (byte)0x23, (byte)0xcd, (byte)0x74, (byte)0xaa, (byte)0x4a, (byte)0xf6, (byte)0xef, (byte)0xe0, (byte)0x2d, (byte)0x17, (byte)0x4a, (byte)0x98, (byte)0xe1, (byte)0x37, (byte)0x1e, (byte)0x9a, (byte)0xc2, (byte)0x0a, (byte)0xea, (byte)0x73, (byte)0x91, (byte)0x23, (byte)0x52, (byte)0xf5, (byte)0x5c, (byte)0x27, (byte)0x94, (byte)0x07, (byte)0xc6, (byte)0x3d, (byte)0xcf, (byte)0xb5, (byte)0xc7, (byte)0x7b, (byte)0xe9, (byte)0x1d, (byte)0x78, (byte)0x4c, (byte)0xc9, (byte)0x05, (byte)0x04, (byte)0xd0, (byte)0x66, (byte)0xd4, (byte)0x98, (byte)0x9d, (byte)0xf4, (byte)0x96, (byte)0x9f, (byte)0x94, (byte)0x39, (byte)0xf4, (byte)0xd1, (byte)0x37, (byte)0x58, (byte)0x0a, (byte)0xd7, (byte)0x67, (byte)0x94, (byte)0x35, (byte)0x59, (byte)0xb0, (byte)0x98, (byte)0xa3, (byte)0xa5, (byte)0x95, (byte)0x37, (byte)0x60, (byte)0x34, (byte)0x7e, (byte)0x57, (byte)0x9f, (byte)0x3b, (byte)0x77, (byte)0xf3, (byte)0xc2, (byte)0xf2, (byte)0x1f, (byte)0xf6, (byte)0x6b, (byte)0x07, (byte)0xb4, (byte)0x98, (byte)0x07, (byte)0x24, (byte)0x36, (byte)0x06, (byte)0x39, (byte)0x4e, (byte)0x6b, (byte)0x08, (byte)0x37, (byte)0x24, (byte)0x98, (byte)0xaa, (byte)0xee, (byte)0x81, (byte)0x6a, (byte)0x84, (byte)0xec, (byte)0xe9, (byte)0x7d, (byte)0x29, (byte)0x99, (byte)0xb4, (byte)0x81, (byte)0x18, (byte)0x08, (byte)0x8a, (byte)0x5b, (byte)0x7b, (byte)0x24, (byte)0x5d};
    static final byte []de_new_patch_uid = {(byte)0x2f, (byte)0x58, (byte)0x3f, (byte)0x00, (byte)0x00, (byte)0xa4, (byte)0x07, (byte)0xe0};
    static final byte []de_new_patch_info = {(byte)0x9d, (byte)0x08, (byte)0x30, (byte)0x01, (byte)0xd8, (byte)0x13};
    // Never in production. Used to emulate German sensor behavior.
    public static boolean use_fake_de_data() {
        //Pref.setBoolean("use_fake_de_data", true);
        return Pref.getBooleanDefaultFalse("use_fake_de_data");
    }

    static boolean enable_bluetooth_ask_user = false;
    enum ENABLE_BLUETOOTH_SET {
        ALWAYS_ALLOW,
        NEVER_ALLOW,
        ASK
    }

    
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

    private static synchronized void doTheScan(final Activity context, Tag tag, boolean showui) {
        synchronized (tag_lock) {
            if (!tag_discovered) {
                if (!useNFC()) return;
                if ((!last_read_succeeded) && (JoH.ratelimit("nfc-debounce", 5)) || (JoH.ratelimit("nfc-debounce", 60))) {
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
                        JoH.static_toast_short(gs(R.string.not_so_quickly_wait_60_seconds));
                    }
                }
            } else {
                Log.d(TAG, "Tag already discovered!");
                if (JoH.tsl() - last_tag_discovered > 60000)
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

    public static void SendLibrereading(final String tagId, byte[] data1, final long CaptureDateTime, byte []patchUid,  byte []patchInfo){
        if(!Home.get_master()) {
            return;
        }
        LibreBlock libreBlock = LibreBlock.getForTimestamp(CaptureDateTime);
        if (libreBlock != null) {
            // We already have this one, so we have already sent it, so let's not crate storms.
            return;
        }
        // Create the object to send
        libreBlock = LibreBlock.create(tagId, CaptureDateTime, data1, 0, patchUid, patchInfo);
        if(libreBlock == null) {
            Log.e(TAG, "Error could not create libreBlock for libre-allhouse");
            return;
        }
        final String json = libreBlock.toExtendedJson();
        
        GcmActivity.pushLibreBlock(json);
    
    }

    public static boolean HandleGoodReading(final String tagId, byte[] data1, final long CaptureDateTime, final boolean allowUpload, byte []patchUid,  byte []patchInfo ) {
        return HandleGoodReading(tagId, data1, CaptureDateTime, allowUpload, patchUid,  patchInfo, false ) ;
    }
    
    
    // returns true if checksum passed.
    public static boolean HandleGoodReading(final String tagId, byte[] data1, final long CaptureDateTime, final boolean allowUpload, byte []patchUid,  byte []patchInfo, boolean decripted_data ) {
        Log.e(TAG, "HandleGoodReading called");
        SendLibrereading(tagId, data1, CaptureDateTime, patchUid, patchInfo);
        
        if(LibreOOPAlgorithm.isDecodeableData(patchInfo) && decripted_data == false 
                && !Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            // Send to OOP2 for drcryption.
            LibreOOPAlgorithm.logIfOOP2NotAlive();
            LibreOOPAlgorithm.sendData(data1, CaptureDateTime, patchUid, patchInfo, tagId);
            return true;
        }
        
        if (Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            // If oop is used, there is no need to  do the checksum It will be done by the oop.
            // (or actually we don't know how to do it, for us 14/de sensors).
            // Save raw block record (we start from block 0)
            LibreBlock.createAndSave(tagId, CaptureDateTime, data1, 0, allowUpload, patchUid, patchInfo);
            LibreOOPAlgorithm.sendData(data1, CaptureDateTime, patchUid, patchInfo, tagId);
        } else {
            final boolean checksum_ok = LibreUtils.verify(data1);
            if (!checksum_ok) {
                Log.e(TAG, "bad cs");
                return false;
            }
            
            // The 4'th byte is where the sensor status is.
            if(!LibreUtils.isSensorReady(data1[4])) {
                Log.e(TAG, "Sensor is not ready, Ignoring reading!");
                return true;
            }
            
            final ReadingData mResult = parseData(0, tagId, data1, CaptureDateTime);
            new Thread() {
                @Override
                public void run() {
                    final PowerManager.WakeLock wl = JoH.getWakeLock("processTransferObject", 60000);
                    try {
                        // Protect against wifi reader and gmc reader coming at the same time.
                        synchronized (NFCReaderX.class) {
                            mResult.CalculateSmothedData();
                            LibreAlarmReceiver.processReadingDataTransferObject(new ReadingData.TransferObject(1, mResult), CaptureDateTime, tagId, allowUpload, patchUid, patchInfo );
                            Home.staticRefreshBGCharts();
                        }
                    } finally {
                        JoH.releaseWakeLock(wl);
                    }
                }
            }.start();
        }
        return true; // Checksum tests have passed.
    }

    public static void enableBluetoothAskUser(Activity context) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater inflater = context.getLayoutInflater();
        View  dialogView = inflater.inflate(R.layout.activity_enable_bluetooth, null);

        dialogBuilder.setView(dialogView);
        final AlertDialog show = dialogBuilder.show();
        
        final CheckBox cbx = (CheckBox) dialogView.findViewById(R.id.enable_streaming_dont_ask_again);

        Button enableStreamingYesButton = (Button) dialogView.findViewById(R.id.enable_streaming_yes);
        enableStreamingYesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if(cbx.isChecked()) {
                    prefs.edit().putString("libre2_enable_bluetooth_streaming", "enable_streaming_always").apply();
                }
                Pref.setLong(ENABLE_BLUETOOTH_TIMESTAMP, JoH.tsl());
                show.dismiss();
            }
        });

        Button enableStreamingNoButton = (Button) dialogView.findViewById(R.id.enable_streaming_no);
        enableStreamingNoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if(cbx.isChecked()) {
                    prefs.edit().putString("libre2_enable_bluetooth_streaming", "enable_streaming_never").apply();
                }
                Pref.setLong(ENABLE_BLUETOOTH_TIMESTAMP, 0);
                show.dismiss();
            }
        });

    }

    private static class NfcVReaderTask extends AsyncTask<Tag, Void, Tag> {

        static ENABLE_BLUETOOTH_SET readEnableBluetoothAllowed(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String enable_streaming = prefs.getString("libre2_enable_bluetooth_streaming", "enable_streaming_ask");
            switch(enable_streaming) {
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
            switch (ebs)  {
                case NEVER_ALLOW:
                    return false;
                case ALWAYS_ALLOW:
                    return true;
                default:
                    // act based on elapsed time,
            }
            if(JoH.msSince(Pref.getLong(ENABLE_BLUETOOTH_TIMESTAMP, 0)) < 2 * MINUTE) {
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
                    if(use_fake_de_data()) {
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
                Log.d(TAG,"calling startHomeWithExtra");
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

        void startLibre2Streaming(NfcV nfcvTag, byte[] patchUid, byte[] patchInfo) throws InterruptedException {
            if(!enableBluetoothAllowed(context)) {
                Log.e(TAG, "Sensor is libre 2, enabeling BT not allowed");
                return;
            }
            Log.e(TAG, "Sensor is libre 2, enabeling BT");
            
            String SensorSN = LibreUtils.decodeSerialNumberKey(patchUid);

            // This is the nfc command to enable streaming
            Pair<byte[], String> unlockData = LibreOOPAlgorithm.nfcSendgetBlutoothEnablePayload();
            if (unlockData == null) {
                Log.e(TAG, "unlockData is null, not enabeling streaming");
                return;
            }
            byte[] nfc_command = unlockData.first;
            Libre2SensorData.setLibre2SensorData(patchUid, patchInfo, 42, 1 , unlockData.second);
            
            final byte[] cmd = new byte[]{0x02, (byte) 0xa1, 0x07};
            final byte[] full_cmd = new byte[cmd.length + nfc_command.length];
            System.arraycopy(cmd, 0, full_cmd, 0, cmd.length);
            System.arraycopy(nfc_command, 0, full_cmd, cmd.length, nfc_command.length);

            Log.e(TAG, "nfc_command to enable streaming = " + HexDump.dumpHexString(full_cmd));

            Long time_patch = System.currentTimeMillis();
            byte[] res = null;
            while (true) {
                try {
                    res = nfcvTag.transceive(full_cmd);
                    if(use_fake_de_data()) {
                        // DC:A6:32:0F:4F:92
                        res = new byte[]{(byte)0x12, (byte)0x92, (byte)0x4f, (byte)0x0f, (byte)0x32, (byte)0xa6, (byte)0xdc};
                    }
                    Log.e(TAG, "enable streaming command returned: " + HexDump.dumpHexString(res));
                    break;
                } catch (IOException e) {
                    if ((System.currentTimeMillis() > time_patch + 2000)) {
                        Log.e(TAG, "enablestraming command read timeout");
                        JoH.static_toast_short(gs(R.string.nfc_read_timeout));
                        vibrate(context, 3);
                        return;
                    }
                    Thread.sleep(100);
                }
            }
            if(res.length == 7) {
                // The mac addresses of the device is the returned data, after removing the first byte, and reversing it.
                res = Arrays.copyOfRange(res, 1, res.length);
                res = BaseTx.reverseBytes(res);

                ActiveBluetoothDevice.setDevice(LibreOOPAlgorithm.getLibreDeviceName() + SensorSN, JoH.bytesToHexMacFormat(res));
                CollectionServiceStarter.restartCollectionServiceBackground();
            } else {
                Log.e(TAG, "enable streaming returned bad data. BT will not work." + HexDump.dumpHexString(res));
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
                        byte []patchUid = tag.getId();
                        if(use_fake_de_data()) {
                            patchUid = de_new_patch_uid;
                            patchInfo = de_new_patch_info;
                        }
                        
                        SensorType sensorType = LibreOOPAlgorithm.getSensorType(patchInfo);
                        if(addressed && sensorType != SensorType.Libre1 ) {
                            Log.d(TAG, "Not using addressed mode since not a libre 1 sensor");
                            addressed = false;
                        }
                        if(sensorType == SensorType.Libre2) {
                            startLibre2Streaming(nfcvTag, patchUid, patchInfo);
                        }
                        
                        if (multiblock) {
                            Log.e(TAG, "starting multiple blobk reads");
                            final int correct_reply_size = addressed ? 28 : 25;
                            for (int i = 0; i <= 43; i = i + 3) {
                                final byte[] cmd;
                                if (addressed) {
                                    cmd = new byte[]{0x60, 0x23, 0, 0, 0, 0, 0, 0, 0, 0, (byte) i, 0x02};
                                    System.arraycopy(uid, 0, cmd, 2, 8);
                                } else {
                                    cmd = new byte[]{0x02, 0x23, (byte) i, 0x02};
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
                                    Log.e(TAG, "Incorrect block size: " + replyBlock.length + " vs " + correct_reply_size);
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
                                    for (int j = 0; j < 3; j++) {
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
                                if(addressed) {
                                    cmd = new byte[]{0x60, 0x20, 0, 0, 0, 0, 0, 0, 0, 0, (byte) i, 0};
                                    System.arraycopy(uid, 0, cmd, 2, 8);
                                    correct_reply_size = 10;
                                    startBlock = 2;
                                } else {
                                    cmd = new byte[]{(byte)0x02, (byte) 0x23, 0, (byte) 0x0};
                                    correct_reply_size = 9;
                                    startBlock = 1;
                                }
                                cmd[2] = (byte)i;

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
                        Log.e(TAG, "Got exception reading nfc in background: ",e);
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

    public static ReadingData parseData(int attempt, String tagId, byte[] data, Long CaptureDateTime) {

        int indexTrend = data[26] & 0xFF;

        int indexHistory = data[27] & 0xFF; // double check this bitmask? should be lower?

        final int sensorTime = 256 * (data[317] & 0xFF) + (data[316] & 0xFF);

        long sensorStartTime = CaptureDateTime - sensorTime * MINUTE;

        // option to use 13 bit mask
        //final boolean thirteen_bit_mask = Pref.getBooleanDefaultFalse("testing_use_thirteen_bit_mask");
        final boolean thirteen_bit_mask = true;

        ArrayList<GlucoseData> historyList = new ArrayList<>();


        // loads history values (ring buffer, starting at index_trent. byte 124-315)
        for (int index = 0; index < 32; index++) {
            int i = indexHistory - index - 1;
            if (i < 0) i += 32;
            GlucoseData glucoseData = new GlucoseData();
            // glucoseData.glucoseLevel =
            //       getGlucose(new byte[]{data[(i * 6 + 125)], data[(i * 6 + 124)]});

            glucoseData.glucoseLevelRaw =
                    getGlucoseRaw(new byte[]{data[(i * 6 + 125)], data[(i * 6 + 124)]}, thirteen_bit_mask);

            int time = Math.max(0, Math.abs((sensorTime - 3) / 15) * 15 - index * 15);

            glucoseData.realDate = sensorStartTime + time * MINUTE;
            glucoseData.sensorId = tagId;
            glucoseData.sensorTime = time;
            historyList.add(glucoseData);
        }


        ArrayList<GlucoseData> trendList = new ArrayList<>();

        // loads trend values (ring buffer, starting at index_trent. byte 28-123)
        for (int index = 0; index < 16; index++) {
            int i = indexTrend - index - 1;
            if (i < 0) i += 16;
            GlucoseData glucoseData = new GlucoseData();
            // glucoseData.glucoseLevel =
            //         getGlucose(new byte[]{data[(i * 6 + 29)], data[(i * 6 + 28)]});

            glucoseData.glucoseLevelRaw =
                    getGlucoseRaw(new byte[]{data[(i * 6 + 29)], data[(i * 6 + 28)]}, thirteen_bit_mask);
            int time = Math.max(0, sensorTime - index);

            glucoseData.realDate = sensorStartTime + time * MINUTE;
            glucoseData.sensorId = tagId;
            glucoseData.sensorTime = time;
            trendList.add(glucoseData);
        }


        final ReadingData readingData = new ReadingData(null, trendList, historyList);
        readingData.raw_data = data;
        return readingData;
    }


    private static int getGlucoseRaw(byte[] bytes, boolean thirteen) {
        if (thirteen) {
            return ((256 * (bytes[0] & 0xFF) + (bytes[1] & 0xFF)) & 0x1FFF);
        } else {
            return ((256 * (bytes[0] & 0xFF) + (bytes[1] & 0xFF)) & 0x0FFF);
        }
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
    
    static public ReadingData getTrend(LibreBlock libreBlock) {
        if(libreBlock.byte_start != 0 || libreBlock.byte_end < 344) {
            Log.i(TAG, "libreBlock exists but does not have enough data " + libreBlock.timestamp);
            return null;
        }
        ReadingData result = parseData(0, "", libreBlock.blockbytes, JoH.tsl());
        if(result.trend.size() == 0 || result.trend.get(0).glucoseLevelRaw == 0) {
            Log.i(TAG, "libreBlock exists but no trend data exists, or first value is zero " + libreBlock.timestamp);
            return null;
        }
        
        // TODO: verify checksum
        return result;
    }
    
}
