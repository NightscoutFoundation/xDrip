package com.eveningoutpost.dexdrip.importedlibraries.dexcom;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.CalRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.EGVRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.GlucoseDataSet;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.MeterRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.SensorRecord;
import com.eveningoutpost.dexdrip.importedlibraries.usbserial.driver.CdcAcmSerialDriver;
import com.eveningoutpost.dexdrip.importedlibraries.usbserial.driver.ProbeTable;
import com.eveningoutpost.dexdrip.importedlibraries.usbserial.driver.UsbSerialDriver;
import com.eveningoutpost.dexdrip.importedlibraries.usbserial.driver.UsbSerialProber;
import com.eveningoutpost.dexdrip.models.Calibration;

import org.json.JSONArray;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

// This code and this particular library are from the NightScout android uploader
// Check them out here: https://github.com/nightscout/android-uploader
// Some of this code may have been modified for use in this project


/**
 * An {@link IntentService} subclass for handling asynchronous CGM Receiver downloads and cloud uploads
 * requests in a service on a separate handler thread.
 */
public class SyncingService extends IntentService {

    // Action for intent
    private static final String ACTION_SYNC = "com.eveningoutpost.dexdrip.importedlibraries.dexcom.action.SYNC";
    private static final String ACTION_CALIBRATION_CHECKIN = "com.eveningoutpost.dexdrip.CalibrationCheckInActivity";

    // Parameters for intent
    private static final String SYNC_PERIOD = "com.eveningoutpost.dexdrip.importedlibraries.dexcom.extra.SYNC_PERIOD";

    // Response to broadcast to activity
    public static final String RESPONSE_SGV = "mySGV";
    public static final String RESPONSE_TREND = "myTrend";
    public static final String RESPONSE_TIMESTAMP = "myTimestamp";
    public static final String RESPONSE_NEXT_UPLOAD_TIME = "myUploadTime";
    public static final String RESPONSE_UPLOAD_STATUS = "myUploadStatus";
    public static final String RESPONSE_DISPLAY_TIME = "myDisplayTime";
    public static final String RESPONSE_JSON = "myJSON";
    public static final String RESPONSE_BAT = "myBatLvl";

    private final String TAG = SyncingService.class.getSimpleName();
    private Context mContext;
    private UsbManager mUsbManager;
    private UsbSerialDriver mSerialDevice;
    private UsbDevice dexcom;
    private UsbDeviceConnection mConnection;

    // Constants
    private final int TIME_SYNC_OFFSET = 10000;
    public static final int MIN_SYNC_PAGES = 2;
    public static final int GAP_SYNC_PAGES = 20;


    /**
     * Starts this service to perform action Single Sync with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionSingleSync(Context context, int numOfPages) {
        Intent intent = new Intent(context, SyncingService.class);
        intent.setAction(ACTION_SYNC);
        intent.putExtra(SYNC_PERIOD, numOfPages);
        context.startService(intent);
    }
    public static void startActionCalibrationCheckin(Context context) {
        Intent intent = new Intent(context, SyncingService.class);
        intent.setAction(ACTION_CALIBRATION_CHECKIN);
        context.startService(intent);
    }
    public SyncingService() {
        super("SyncingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mContext = getApplicationContext();
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SYNC.equals(action)) {
                final int param1 = intent.getIntExtra(SYNC_PERIOD, 1);
                handleActionSync(param1);
            } else if (ACTION_CALIBRATION_CHECKIN.equals(action)) {
                Log.i("CALIBRATION-CHECK-IN: ", "Beginning check in process");
                performCalibrationCheckin();
            }
        }
    }

    /**
     * Handle action Sync in the provided background thread with the provided
     * parameters.
     */
    private void performCalibrationCheckin(){
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NSDownload");
        wl.acquire();
        try {
            Log.i("CALIBRATION-CHECK-IN: ", "Wake Lock Acquired");
            if (acquireSerialDevice()) {
                try {
                    ReadData readData = new ReadData(mSerialDevice, mConnection, dexcom);

//                ReadData readData = new ReadData(mSerialDevice);
                    CalRecord[] calRecords = readData.getRecentCalRecords();
                    Log.i("CALIBRATION-CHECK-IN: ", "Found " + calRecords.length + " Records!");
                    save_most_recent_cal_record(calRecords);

                } catch (Exception e) {
                    Log.wtf("Unhandled exception caught", e);
                } finally {
                    // Close serial
                    try {
                        mSerialDevice.getPorts().get(0).close();
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to close", e);
                    }

                }
            } else {
                Log.w("CALIBRATION-CHECK-IN: ", "Failed to acquire serial device");
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private void handleActionSync(int numOfPages) {
        boolean broadcastSent = false;

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NSDownload");
        wl.acquire();

        try {
            sync(numOfPages);
        } finally {
            wl.release();
        }
    }

    private void sync(int numOfPages) {
        boolean broadcastSent;
        if (acquireSerialDevice()) {
            try {

                ReadData readData = new ReadData(mSerialDevice);
                // TODO: need to check if numOfPages if valid on ReadData side
                EGVRecord[] recentRecords = readData.getRecentEGVsPages(numOfPages);
                MeterRecord[] meterRecords = readData.getRecentMeterRecords();
                // TODO: need to check if numOfPages if valid on ReadData side
                SensorRecord[] sensorRecords = readData.getRecentSensorRecords(numOfPages);
                GlucoseDataSet[] glucoseDataSets = Utils.mergeGlucoseDataRecords(recentRecords, sensorRecords);

                // FIXME: This is a workaround for the new Dexcom AP which seems to have a new format
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                CalRecord[] calRecords = new CalRecord[1];
                if (prefs.getBoolean("cloud_cal_data", false)) {
                    calRecords = readData.getRecentCalRecords();
                }

                long timeSinceLastRecord = readData.getTimeSinceEGVRecord(recentRecords[recentRecords.length - 1]);
                // TODO: determine if the logic here is correct. I suspect it assumes the last record was less than 5
                // minutes ago. If a reading is skipped and the device is plugged in then nextUploadTime will be
                // set to a negative number. This situation will eventually correct itself.
                long nextUploadTime = (1000 * 60 * 5) - (timeSinceLastRecord * (1000));
                long displayTime = readData.readDisplayTime().getTime();
                // FIXME: Device seems to flake out on battery level reads. Removing for now.
//                int batLevel = readData.readBatteryLevel();
                int batLevel = 100;

                // convert into json for d3 plot
                JSONArray array = new JSONArray();
                for (int i = 0; i < recentRecords.length; i++) array.put(recentRecords[i].toJSON());

                EGVRecord recentEGV = recentRecords[recentRecords.length - 1];
//                broadcastSGVToUI(recentEGV, uploadStatus, nextUploadTime + TIME_SYNC_OFFSET,
//                                 displayTime, array ,batLevel);
                broadcastSent=true;
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.wtf("Unable to read from the dexcom, maybe it will work next time", e);
            } catch (NegativeArraySizeException e) {
                Log.wtf("Negative array exception from receiver", e);
            } catch (IndexOutOfBoundsException e) {
                Log.wtf("IndexOutOfBounds exception from receiver", e);
            } catch (CRCFailRuntimeException e){
                // FIXME: may consider localizing this catch at a lower level (like ReadData) so that
                // if the CRC check fails on one type of record we can capture the values if it
                // doesn't fail on other types of records. This means we'd need to broadcast back
                // partial results to the UI. Adding it to a lower level could make the ReadData class
                // more difficult to maintain - needs discussion.
                Log.wtf("CRC failed", e);
            } catch (Exception e) {
                Log.wtf("Unhandled exception caught", e);
            } finally {
                // Close serial
                try {
                    mSerialDevice.getPorts().get(0).close();
                } catch (IOException e) {

                    Log.e(TAG, "Unable to close", e);
                }

            }
        }
        //        if (!broadcastSent) broadcastSGVToUI();
    }

    private void save_most_recent_cal_record(CalRecord[] calRecords) {
        int size = calRecords.length;
        Calibration.create(calRecords,getApplicationContext(), false, 0);
    }

    private boolean acquireSerialDevice() {
        UsbDevice found_device = findDexcom();

        if (mUsbManager == null) {
            Log.w("CALIBRATION-CHECK-IN: ", "USB manager is null");
            return false;
        }

        if (dexcom == null) {
            Log.e(TAG, "dex device == null");
            return false;
        }

        if( mUsbManager.hasPermission(dexcom)) {                                           // the system is allowing us to poke around this device

            ProbeTable customTable = new ProbeTable();                                           // From the USB library...
            customTable.addProduct(0x22A3, 0x0047, CdcAcmSerialDriver.class);       // ...Specify the Vendor ID and Product ID

            UsbSerialProber prober = new UsbSerialProber(customTable);                      // Probe the device with the custom values
            List<UsbSerialDriver> drivers = prober.findAllDrivers(mUsbManager);            // let's go through the list
            Iterator<UsbSerialDriver> foo = drivers.iterator();                                                                                                  // Invalid Return code
            while (foo.hasNext()) {                                                         // let's loop through
                UsbSerialDriver driver = foo.next();                                        // set fooDriver to the next available driver
                if (driver != null) {
                    UsbDeviceConnection connection = mUsbManager.openDevice(driver.getDevice());
                    if (connection != null) {
                        mSerialDevice = driver;

                        mConnection = connection;
                        Log.i("CALIBRATION-CHECK-IN: ", "CONNECTEDDDD!!");
                        return true;
                    }
                } else {
                    Log.w("CALIBRATION-CHECK-IN: ", "Driver was no good");
                }
            }
            Log.w("CALIBRATION-CHECK-IN: ", "No usable drivers found");
        } else {
            Log.w("CALIBRATION-CHECK-IN: ", "You dont have permissions for that dexcom!!");
        }
        return false;
    }

    static public boolean isG4Connected(Context c){
        UsbManager manager = (UsbManager) c.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Log.i("USB DEVICES = ", deviceList.toString());
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        Log.i("USB DEVICES = ", String.valueOf(deviceList.size()));

        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            if (device.getVendorId() == 8867 && device.getProductId() == 71
                    && device.getDeviceClass() == 2 && device.getDeviceSubclass() ==0
                    && device.getDeviceProtocol() == 0){
                Log.i("CALIBRATION-CHECK-IN: ", "Dexcom Found!");
                return true;
            }
        }
        return false;
    }

    public UsbDevice findDexcom() {
        Log.i("CALIBRATION-CHECK-IN: ", "Searching for dexcom");
        mUsbManager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
        Log.i("USB MANAGER = ", mUsbManager.toString());
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Log.i("USB DEVICES = ", deviceList.toString());
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        Log.i("USB DEVICES = ", String.valueOf(deviceList.size()));

        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            if (device.getVendorId() == 8867 && device.getProductId() == 71
                    && device.getDeviceClass() == 2 && device.getDeviceSubclass() ==0
                    && device.getDeviceProtocol() == 0){
                dexcom = device;
                Log.i("CALIBRATION-CHECK-IN: ", "Dexcom Found!");
                return device;
            } else {
                Log.w("CALIBRATION-CHECK-IN: ", "that was not a dexcom (I dont think)");
            }
        }
        return null;
    }

    private void broadcastSGVToUI(EGVRecord egvRecord, boolean uploadStatus,
                                  long nextUploadTime, long displayTime,
                                  JSONArray json, int batLvl) {
        Log.d(TAG, "Current EGV: " + egvRecord.getBGValue());
        Intent broadcastIntent = new Intent();
//        broadcastIntent.setAction(MainActivity.CGMStatusReceiver.PROCESS_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(RESPONSE_SGV, egvRecord.getBGValue());
        broadcastIntent.putExtra(RESPONSE_TREND, egvRecord.getTrend().getID());
        broadcastIntent.putExtra(RESPONSE_TIMESTAMP, egvRecord.getDisplayTime().getTime());
        broadcastIntent.putExtra(RESPONSE_NEXT_UPLOAD_TIME, nextUploadTime);
        broadcastIntent.putExtra(RESPONSE_UPLOAD_STATUS, uploadStatus);
        broadcastIntent.putExtra(RESPONSE_DISPLAY_TIME, displayTime);
        if (json!=null)
            broadcastIntent.putExtra(RESPONSE_JSON, json.toString());
        broadcastIntent.putExtra(RESPONSE_BAT, batLvl);
        sendBroadcast(broadcastIntent);
    }

    private void broadcastSGVToUI() {
        EGVRecord record=new EGVRecord(-1, Dex_Constants.TREND_ARROW_VALUES.NONE,new Date(),new Date());
        broadcastSGVToUI(record,false, (long) (1000 * 60 * 5) + TIME_SYNC_OFFSET, new Date().getTime(), null, 0);
    }

}
