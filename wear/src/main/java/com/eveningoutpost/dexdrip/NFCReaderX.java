package com.eveningoutpost.dexdrip;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.View;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.GlucoseData;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.LibreBlock;
import com.eveningoutpost.dexdrip.Models.LibreOOPAlgorithm;
import com.eveningoutpost.dexdrip.Models.ReadingData;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.LibreUtils;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

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
    public static boolean HandleGoodReading(String tagId, byte[] data1, final long CaptureDateTime) {
        return HandleGoodReading(tagId, data1, CaptureDateTime, false, null, null);
    }

    // returns true if checksum passed.
    public static boolean HandleGoodReading(final String tagId, byte[] data1, final long CaptureDateTime, final boolean allowUpload, byte []patchUid,  byte []patchInfo ) {


        if (Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            // If oop is used, there is no need to  do the checksum It will be done by the oop.
            // (or actually we don't know how to do it, for us 14/de sensors).
            // Save raw block record (we start from block 0)
            LibreBlock.createAndSave(tagId, CaptureDateTime, data1, 0, allowUpload, patchUid, patchInfo);
            LibreOOPAlgorithm.SendData(data1, CaptureDateTime, patchUid, patchInfo);
        } else {
            final boolean checksum_ok = LibreUtils.verify(data1);
            if (!checksum_ok) {
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
                        mResult.CalculateSmothedData();
                        LibreAlarmReceiver.processReadingDataTransferObject(new ReadingData.TransferObject(1, mResult), CaptureDateTime, tagId, allowUpload, patchUid, patchInfo );
                        Home.staticRefreshBGCharts();
                    } finally {
                        JoH.releaseWakeLock(wl);
                    }
                }
            }.start();
        }
        return true; // Checksum tests have passed.
    }

    private static class NfcVReaderTask extends AsyncTask<Tag, Void, Tag> {

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
                    boolean checksum_ok = HandleGoodReading(SensorSn, data, now, false, tag.getId(), patchInfo);
                    if(checksum_ok == false) {
                        Log.e(TAG, "Read data but checksum is wrong");
                    }
                    PersistentStore.setString("LibreSN", SensorSn);
                } else {
                    Log.d(TAG, "Scan did not succeed so ignoring buffer");
                }
                Home.startHomeWithExtra(context, null, null);

            } catch (IllegalStateException e) {
                Log.e(TAG, "Illegal state exception in postExecute: " + e);

            } finally {
                tag_discovered = false; // right place?
                Home.staticBlockUI(context, false);
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
                        final boolean addressed = !Pref.getBoolean("use_nfc_any_tag", true);
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
                        
                        if (multiblock) {
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
                            final int correct_reply_size = 10;
                            for (int i = 0; i < 43; i++) {
                                final byte[] cmd = new byte[]{0x60, 0x20, 0, 0, 0, 0, 0, 0, 0, 0, (byte) i, 0};
                                System.arraycopy(uid, 0, cmd, 2, 8);
                                byte[] oneBlock;
                                Long time = System.currentTimeMillis();
                                while (true) {
                                    try {
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
                                    Log.d(TAG, HexDump.dumpHexString(oneBlock, 0, oneBlock.length));
                                if (oneBlock.length != correct_reply_size) {
                                    Log.e(TAG, "Incorrect block size: " + oneBlock.length + " vs " + correct_reply_size);
                                    JoH.static_toast_short(gs(R.string.nfc_invalid_data));
                                    vibrate(context, 3);
                                    return null;
                                }
                                System.arraycopy(oneBlock, 2, data, i * 8, 8);
                            }
                        }
                        JoH.benchmark("Tag read");
                        Log.d(TAG, "GOT TAG DATA!");
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
                        Log.i(TAG, "Got exception reading nfc in background: " + e.toString());
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


        final ReadingData readingData = new ReadingData(trendList, historyList);
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
