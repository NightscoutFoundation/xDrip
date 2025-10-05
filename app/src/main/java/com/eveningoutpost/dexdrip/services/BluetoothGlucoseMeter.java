package com.eveningoutpost.dexdrip.services;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.PowerManager;

import android.util.Log;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.glucosemeter.CurrentTimeRx;
import com.eveningoutpost.dexdrip.glucosemeter.GlucoseReadingRx;
import com.eveningoutpost.dexdrip.glucosemeter.RecordsCmdTx;
import com.eveningoutpost.dexdrip.glucosemeter.VerioHelper;
import com.eveningoutpost.dexdrip.glucosemeter.caresens.ContextRx;
import com.eveningoutpost.dexdrip.glucosemeter.caresens.TimeTx;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.eveningoutpost.dexdrip.glucosemeter.VerioHelper.VERIO_F7A1_SERVICE;
import static com.eveningoutpost.dexdrip.glucosemeter.VerioHelper.VERIO_F7A2_WRITE;
import static com.eveningoutpost.dexdrip.glucosemeter.VerioHelper.VERIO_F7A3_NOTIFICATION;
import static com.eveningoutpost.dexdrip.models.CalibrationRequest.isSlopeFlatEnough;
import static com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder.unitized_string_with_units_static;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Created by jamorham on 09/12/2016.
 * based on code by LadyViktoria
 * <p>
 * Should support any bluetooth standard compliant meter
 * offering the Glucose Service, like the excellent
 * Contour Next One
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public class BluetoothGlucoseMeter extends Service {

    public final static String ACTION_BLUETOOTH_GLUCOSE_METER_SERVICE_UPDATE
            = "com.eveningoutpost.dexdrip.BLUETOOTH_GLUCOSE_METER_SERVICE_UPDATE";
    public final static String ACTION_BLUETOOTH_GLUCOSE_METER_NEW_SCAN_DEVICE
            = "com.eveningoutpost.dexdrip.BLUETOOTH_GLUCOSE_METER_NEW_SCAN_DEVICE";
    public final static String BLUETOOTH_GLUCOSE_METER_TAG = "Bluetooth Glucose Meter";

    private static final String GLUCOSE_READING_MARKER = "Glucose Reading From: ";
    private static final String TAG = BluetoothGlucoseMeter.class.getSimpleName();

    private static final UUID GLUCOSE_SERVICE = UUID.fromString("00001808-0000-1000-8000-00805f9b34fb");
    private static final UUID CURRENT_TIME_SERVICE = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_INFO_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    private static final UUID CONTOUR_SERVICE = UUID.fromString("00000000-0002-11e2-9e96-0800200c9a66");

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID GLUCOSE_CHARACTERISTIC = UUID.fromString("00002a18-0000-1000-8000-00805f9b34fb");
    private static final UUID CONTEXT_CHARACTERISTIC = UUID.fromString("00002a34-0000-1000-8000-00805f9b34fb");
    private static final UUID RECORDS_CHARACTERISTIC = UUID.fromString("00002a52-0000-1000-8000-00805f9b34fb");
    private static final UUID TIME_CHARACTERISTIC = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");
    private static final UUID DATE_TIME_CHARACTERISTIC = UUID.fromString("00002a08-0000-1000-8000-00805f9b34fb");

    private static final UUID CONTOUR_1022 = UUID.fromString("00001022-0002-11e2-9e96-0800200c9a66");
    private static final UUID CONTOUR_1025 = UUID.fromString("00001025-0002-11e2-9e96-0800200c9a66");
    private static final UUID CONTOUR_1026 = UUID.fromString("00001026-0002-11e2-9e96-0800200c9a66");

    private static final UUID ISENS_TIME_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static final UUID ISENS_TIME_CHARACTERISTIC = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");

    private static final UUID MANUFACTURER_NAME = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");


    private static final ConcurrentLinkedQueue<Bluetooth_CMD> queue = new ConcurrentLinkedQueue<>();
    private static final Object mLock = new Object(); // ok static?

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private static final long NO_CLOCK_THRESHOLD = Constants.MINUTE_IN_MS * 70; // +- minutes

    private static final boolean d = false;
    private static final boolean ignore_control_solution_tests = true;

    private static final long SCAN_PERIOD = 10000;

    private static boolean await_acks = false;
    public static boolean awaiting_ack = false;
    public static boolean awaiting_data = false;

    private static int bondingstate = -1;
    private static long started_at = -1;

    private static BluetoothAdapter mBluetoothAdapter;
    public static String mBluetoothDeviceAddress;
    private static String mLastConnectedDeviceAddress;
    private static String mLastManufacturer = "";
    private static BluetoothGatt mBluetoothGatt;

    private static int mConnectionState = STATE_DISCONNECTED;
    private static int service_discovery_count = 0;
    private static boolean services_discovered = false;
    private static Bluetooth_CMD last_queue_command;

    private static CurrentTimeRx ct;
    private static int highestSequenceStore = 0;
    private BloodTest lastBloodTest;
    private GlucoseReadingRx awaitingContext;

    // bluetooth gatt callback
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {

                if (mConnectionState != STATE_CONNECTED) {
                    // TODO sane release
                    PowerManager.WakeLock wl = JoH.getWakeLock("bluetooth-meter-connected", 60000);
                    mConnectionState = STATE_CONNECTED;
                    mLastConnectedDeviceAddress = gatt.getDevice().getAddress();

                    statusUpdate("Connected to device: " + mLastConnectedDeviceAddress);
                    if ((playSounds() && (JoH.ratelimit("bt_meter_connect_sound", 3)))) {
                        JoH.playResourceAudio(R.raw.bt_meter_connect);
                    }

                    Log.d(TAG, "Delay for settling");
                    waitFor(600);
                    statusUpdate("Discovering services");
                    service_discovery_count = 0; // reset as new non retried connnection
                    discover_services();
                    // Bluetooth_CMD.poll_queue(); // do we poll here or on service discovery - should we clear here?
                } else {
                    // TODO timeout
                    Log.e(TAG, "Apparently already connected - ignoring");
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                final int old_connection_state = mConnectionState;
                mConnectionState = STATE_DISCONNECTED;
                statusUpdate("Disconnected");
                if ((old_connection_state == STATE_CONNECTED) && (playSounds() && (JoH.ratelimit("bt_meter_disconnect_sound", 3)))) {
                    JoH.playResourceAudio(R.raw.bt_meter_disconnect);
                }
                close();
                refreshDeviceCache(mBluetoothGatt);
                Bluetooth_CMD.poll_queue();
                // attempt reconnect
                reconnect();
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                services_discovered = true;
                statusUpdate("Services discovered");

                bondingstate = mBluetoothGatt.getDevice().getBondState();
                if (bondingstate != BluetoothDevice.BOND_BONDED) {
                    statusUpdate("Attempting to create pairing bond - device must be in pairing mode!");
                    sendDeviceUpdate(gatt.getDevice());
                    mBluetoothGatt.getDevice().createBond();
                    waitFor(1000);
                    bondingstate = mBluetoothGatt.getDevice().getBondState();
                    if (bondingstate != BluetoothDevice.BOND_BONDED) {
                        statusUpdate("Pairing appeared to fail");
                    } else {
                        sendDeviceUpdate(gatt.getDevice());
                    }
                } else {
                    Log.d(TAG, "Device is already bonded - good");
                }

                if (d) {
                    List<BluetoothGattService> gatts = getSupportedGattServices();
                    for (BluetoothGattService bgs : gatts) {
                        Log.d(TAG, "DEBUG: " + bgs.getUuid());
                    }
                }

                if (queue.isEmpty()) {
                    statusUpdate("Requesting data from meter");
                    Bluetooth_CMD.read(DEVICE_INFO_SERVICE, MANUFACTURER_NAME, "get device manufacturer");
                    Bluetooth_CMD.read(CURRENT_TIME_SERVICE, TIME_CHARACTERISTIC, "get device time");

                    Bluetooth_CMD.notify(GLUCOSE_SERVICE, GLUCOSE_CHARACTERISTIC, "notify new glucose record");
                    Bluetooth_CMD.enable_notification_value(GLUCOSE_SERVICE, GLUCOSE_CHARACTERISTIC, "notify new glucose value");

                    if (hasContextCharacteristic(gatt)) {
                        Bluetooth_CMD.enable_notification_value(GLUCOSE_SERVICE, CONTEXT_CHARACTERISTIC, "notify new context value");
                        Bluetooth_CMD.notify(GLUCOSE_SERVICE, CONTEXT_CHARACTERISTIC, "notify new glucose context");
                    } else {
                        if (d) {
                            Log.d(TAG, "Device has no context characteristic. Skipping");
                        }
                    }

                    Bluetooth_CMD.enable_indications(GLUCOSE_SERVICE, RECORDS_CHARACTERISTIC, "readings indication request");
                    Bluetooth_CMD.notify(GLUCOSE_SERVICE, RECORDS_CHARACTERISTIC, "notify glucose record");
                    Bluetooth_CMD.write(GLUCOSE_SERVICE, RECORDS_CHARACTERISTIC, RecordsCmdTx.getAllRecords(), "request all readings");
                    Bluetooth_CMD.notify(GLUCOSE_SERVICE, GLUCOSE_CHARACTERISTIC, "notify new glucose record again"); // dummy

                    Bluetooth_CMD.poll_queue();

                } else {
                    Log.e(TAG, "Queue is not empty so not scheduling anything..");
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        private boolean hasContextCharacteristic(BluetoothGatt gatt) {
            BluetoothGattService glucoseService = gatt.getService(GLUCOSE_SERVICE);
            if (glucoseService != null) {
                BluetoothGattCharacteristic contextCharacteristic = glucoseService.getCharacteristic(CONTEXT_CHARACTERISTIC);
                return contextCharacteristic != null;
            }
            return false;
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor,
                                      int status) {
            Log.d(TAG, "Descriptor written to: " + descriptor.getUuid() + " getvalue: " + JoH.bytesToHex(descriptor.getValue()) + " status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Bluetooth_CMD.poll_queue();
            } else {
                Log.e(TAG, "Got gatt descriptor write failure: " + status);
                Bluetooth_CMD.retry_last_command(status);
            }
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            Log.d(TAG, "Written to: " + characteristic.getUuid() + " getvalue: " + JoH.bytesToHex(characteristic.getValue()) + " status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (ack_blocking()) {
                    if (d)
                        Log.d(TAG, "Awaiting ACK before next command: " + awaiting_ack + ":" + awaiting_data);
                } else {
                    Bluetooth_CMD.poll_queue();
                }
            } else {
                Log.e(TAG, "Got gatt write failure: " + status);
                Bluetooth_CMD.retry_last_command(status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                if (characteristic.getUuid().equals(TIME_CHARACTERISTIC)) {
                    UserError.Log.d(TAG, "Got time characteristic read data");
                    ct = new CurrentTimeRx(characteristic.getValue());
                    statusUpdate("Device time: " + ct.toNiceString());
                } else if (characteristic.getUuid().equals(DATE_TIME_CHARACTERISTIC)) {
                    UserError.Log.d(TAG, "Got date time characteristic read data");
                    ct = new CurrentTimeRx(characteristic.getValue());
                    statusUpdate("Device time: " + ct.toNiceString());
                } else if (characteristic.getUuid().equals(MANUFACTURER_NAME)) {
                    mLastManufacturer = characteristic.getStringValue(0);
                    UserError.Log.d(TAG, "Manufacturer Name: " + mLastManufacturer);
                    statusUpdate("Device from: " + mLastManufacturer);

                    await_acks = false; // reset

                    // Roche Aviva Connect uses a DateTime characteristic instead
                    if (mLastManufacturer.startsWith("Roche")) {
                        Bluetooth_CMD.transmute_command(CURRENT_TIME_SERVICE, TIME_CHARACTERISTIC,
                                GLUCOSE_SERVICE, DATE_TIME_CHARACTERISTIC);
                    }

                    // Diamond Mobile Mini DM30b firmware v1.2.4
                    // v1.2.4 has reversed sequence numbers and first item is last item and no clock access
                    if (mLastManufacturer.startsWith("TaiDoc")) {
                        // no time service!
                        Bluetooth_CMD.delete_command(CURRENT_TIME_SERVICE, TIME_CHARACTERISTIC);
                        ct = new CurrentTimeRx(); // implicitly trust meter time stamps!! beware daylight saving time changes
                        ct.noClockAccess = true;
                        ct.sequenceNotReliable = true;

                        // no glucose context!
                        Bluetooth_CMD.delete_command(GLUCOSE_SERVICE, CONTEXT_CHARACTERISTIC);
                        Bluetooth_CMD.delete_command(GLUCOSE_SERVICE, CONTEXT_CHARACTERISTIC);

                        // only request last reading - diamond mini seems to make sequence 0 be the most recent record
                        Bluetooth_CMD.replace_command(GLUCOSE_SERVICE, RECORDS_CHARACTERISTIC, "W",
                                new Bluetooth_CMD("W", GLUCOSE_SERVICE, RECORDS_CHARACTERISTIC, RecordsCmdTx.getFirstRecord(), "request newest reading"));

                    }

                    // Caresens Dual
                    if (mLastManufacturer.startsWith("i-SENS")) {
                        Bluetooth_CMD.delete_command(CURRENT_TIME_SERVICE, TIME_CHARACTERISTIC);
                        ct = new CurrentTimeRx(); // implicitly trust meter time stamps!! beware daylight saving time changes
                        ct.noClockAccess = true;
                        Bluetooth_CMD.notify(ISENS_TIME_SERVICE, ISENS_TIME_CHARACTERISTIC, "notify isens clock");
                        Bluetooth_CMD.write(ISENS_TIME_SERVICE, ISENS_TIME_CHARACTERISTIC, new TimeTx(JoH.tsl()).getByteSequence(), "set isens clock");
                        Bluetooth_CMD.write(ISENS_TIME_SERVICE, ISENS_TIME_CHARACTERISTIC, new TimeTx(JoH.tsl()).getByteSequence(), "set isens clock");

                        Bluetooth_CMD.replace_command(GLUCOSE_SERVICE, RECORDS_CHARACTERISTIC, "W",
                                new Bluetooth_CMD("W", GLUCOSE_SERVICE, RECORDS_CHARACTERISTIC, RecordsCmdTx.getNewerThanSequence(getHighestSequence()), "request reading newer than " + getHighestSequence()));

                    }

                    // LifeScan Verio Flex
                    if (mLastManufacturer.startsWith("LifeScan")) {

                        await_acks = true;

                        Bluetooth_CMD.empty_queue(); // Verio Flex isn't standards compliant

                        Bluetooth_CMD.notify(VERIO_F7A1_SERVICE, VERIO_F7A3_NOTIFICATION, "verio general notification");
                        Bluetooth_CMD.enable_notification_value(VERIO_F7A1_SERVICE, VERIO_F7A3_NOTIFICATION, "verio general notify value");
                        Bluetooth_CMD.write(VERIO_F7A1_SERVICE, VERIO_F7A2_WRITE, VerioHelper.getTimeCMD(), "verio ask time");
                        Bluetooth_CMD.write(VERIO_F7A1_SERVICE, VERIO_F7A2_WRITE, VerioHelper.getTcounterCMD(), "verio T data query"); // don't change order with R
                        Bluetooth_CMD.write(VERIO_F7A1_SERVICE, VERIO_F7A2_WRITE, VerioHelper.getRcounterCMD(), "verio R data query"); // don't change order with T

                    }

                } else {
                    Log.d(TAG, "Got a different charactersitic! " + characteristic.getUuid().toString());

                }
                Bluetooth_CMD.poll_queue();
            } else {
                Log.e(TAG, "Got gatt read failure: " + status);
                Bluetooth_CMD.retry_last_command(status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            final PowerManager.WakeLock wl = JoH.getWakeLock("bt-meter-characterstic-change", 30000);
            try {
                processCharacteristicChange(gatt, characteristic);
                Bluetooth_CMD.poll_queue();
            } finally {
                JoH.releaseWakeLock(wl);
            }
        }
    };

    // end callback

    private BluetoothManager mBluetoothManager;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private ScanCallback mScanCallback;

    private String lastScannedDeviceAddress = "";
    // old api
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    JoH.runOnUiThreadDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (d)
                                Log.i(TAG, "old: onLeScan " + device.toString() + " c:" + device.getBluetoothClass().toString());

                            if (d) Log.i(TAG, "" + device.getName());

                            if ((lastScannedDeviceAddress.equals(device.getAddress())) && (!JoH.ratelimit("bt-scan-repeated-address", 2))) {
                                if (d)
                                    Log.d(TAG, "Ignoring repeated address: " + device.getAddress());
                            } else {
                                lastScannedDeviceAddress = device.getAddress();
                                sendDeviceUpdate(device);
                            }
                        }
                    }, 0);
                }
            };

    private static void sendDeviceUpdate(BluetoothDevice device) {
        sendDeviceUpdate(device, false);
    }

    private static void sendDeviceUpdate(BluetoothDevice device, boolean force) {
        if (device == null) return;
        broadcastUpdate(ACTION_BLUETOOTH_GLUCOSE_METER_NEW_SCAN_DEVICE, device.getAddress()
                + "^" + device.getBondState()
                + "^" + ((device.getName() != null) ? device.getName().replace("^", "") : "") + (force ? " " : ""));
    }

    private static boolean isBonded() {
        return (bondingstate == BluetoothDevice.BOND_BONDED);
    }

    private static boolean playSounds() {
        return Pref.getBoolean("bluetooth_meter_play_sounds", true);
    }

    private synchronized static void forgetDevice(String address) {
        Log.d(TAG, "forgetDevice() start");
        try {
            if ((mBluetoothAdapter == null) || (address == null)) return;
            final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName() != null) {
                        if (device.getAddress().equals(address)) {
                            Log.e(TAG, "Unpairing.. " + address);
                            JoH.static_toast_long("Unpairing: " + address);
                            try {
                                Method m = device.getClass().getMethod("removeBond", (Class[]) null);
                                m.invoke(device, (Object[]) null);

                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage(), e);
                            }
                        }
                    }

                }
            }
            Log.d(TAG, "forgetDevice() finished");
        } catch (Exception e) {
            Log.wtf(TAG, "Exception forgetting: " + address + " " + e);
        }
    }

    /**
     * /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    private static synchronized void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.d(TAG, "Closing gatt");
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }


    protected static void waitFor(final int millis) {
        synchronized (mLock) {
            try {
                Log.e(TAG, "waiting " + millis + "ms");
                mLock.wait(millis);
            } catch (final InterruptedException e) {
                Log.e(TAG, "Sleeping interrupted", e);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT < 26) {
            final IntentFilter pairingRequestFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
            pairingRequestFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
            registerReceiver(mPairingRequestRecevier, pairingRequestFilter);
        } else {
            UserError.Log.d(TAG, "Not registering pairing receiver on Android 8+");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        started_at = JoH.tsl();

        initialize();
        final String service_action = intent.getStringExtra("service_action");
        if (service_action != null) {
            if (service_action.equals("connect")) {
                // some reset method neeeded - as we don't appear to have shutdown
                close();
                mLastConnectedDeviceAddress = "";
                bondingstate = -1;
                mConnectionState = STATE_DISCONNECTED;
                scanLeDevice(false); // stop scanning
                String connect_address = intent.getStringExtra("connect_address");
                connect(connect_address);
            } else if (service_action.equals("scan")) {
                beginScan();
            } else if (service_action.equals("forget")) {
                final String forget_address = intent.getStringExtra("forget_address");
                forgetDevice(forget_address);
                beginScan();
            }
        } else {
            // default action here?
        }
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
        try {
            unregisterReceiver(mPairingRequestRecevier);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering pairing receiver: " + e);
        }
        started_at = -1;
    }

    public void startup() {
        UserError.Log.d(TAG, "startup()");
        initScanCallback();
    }

    // robustly discover device services
    private synchronized void discover_services() {
        UserError.Log.d(TAG, "discover_services()");
        awaiting_data = false; // reset
        awaiting_ack = false;
        services_discovered = false;
        service_discovery_count++;
        if (mBluetoothGatt != null) {
            if (mConnectionState == STATE_CONNECTED) {

                mBluetoothGatt.discoverServices();
                JoH.runOnUiThreadDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if ((!services_discovered) && (service_discovery_count < 10)) {
                            Log.d(TAG, "Timeout discovering services - retrying...");
                            discover_services();
                        }
                    }
                }, (5000 + (500 * service_discovery_count)));
            } else {
                Log.e(TAG, "Cannot discover services as we are not connected");
            }
        } else {
            Log.e(TAG, "mBluetoothGatt is null!");
        }
    }

    private void reconnect() {
        statusUpdate("Attempting reconnection: " + mBluetoothDeviceAddress);
        connect(mBluetoothDeviceAddress);
    }

    // seriously..
    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        if (gatt == null) return false;
        try {
            Method localMethod = gatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                return (Boolean) localMethod.invoke(gatt, new Object[0]);
            }
        } catch (Exception localException) {
            Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }

    public static void statusUpdate(String status) {
        broadcastUpdate(ACTION_BLUETOOTH_GLUCOSE_METER_SERVICE_UPDATE, status);
        UserError.Log.d(TAG, "StatusUpdate: " + status);
    }

    private static void broadcastUpdate(final String action, final String data) {
        final Intent intent = new Intent(action);
        intent.putExtra("data", data);
        LocalBroadcastManager.getInstance(xdrip.getAppContext()).sendBroadcast(intent);
    }

    private static boolean ack_blocking() {
        final boolean result = await_acks && (awaiting_ack || awaiting_data);
        if (result) {
            if (d) Log.d(TAG, "Ack blocking: " + awaiting_ack + ":" + awaiting_data);
        }
        return result;
    }

    private synchronized void markDeviceAsSuccessful(BluetoothGatt gatt) {
        if (!Pref.getStringDefaultBlank("selected_bluetooth_meter_address").equals(mLastConnectedDeviceAddress)) {
            Pref.setString("selected_bluetooth_meter_address", mLastConnectedDeviceAddress);
            Pref.setString("selected_bluetooth_meter_info", mLastManufacturer + "   " + mLastConnectedDeviceAddress);
            Pref.setBoolean("bluetooth_meter_enabled", true); // auto-enable the setting
            JoH.static_toast_long("Success with: " + mLastConnectedDeviceAddress + "  Enabling auto-start");
            if (gatt != null) sendDeviceUpdate(gatt.getDevice(), true); // force update
        }
    }

    private void processGlucoseReadingRx(GlucoseReadingRx gtb) {
        if ((!ignore_control_solution_tests) || (gtb.sampleType != 10)) {

            if (ct.sequenceNotReliable) {
                // sequence numbers on some meters are reversed so instead invent one using timestamp with 5 second resolution
                gtb.sequence = (int) ((gtb.time / 5000) - (1496755620 / 5));
            } else {
                setHighestSequence(gtb.sequence);
            }

            if (ct.noClockAccess && Pref.getBooleanDefaultFalse("meter_recent_reading_as_now")) {
                // for diamond mini we don't know if the clock is correct but if it is within the threshold period then we treat it as if the reading
                // has just happened. We only do this when we have received at least one synced reading so the first one after pairing isn't munged.
                if (JoH.absMsSince(gtb.time) < NO_CLOCK_THRESHOLD
                        && PersistentStore.getBoolean(GLUCOSE_READING_MARKER + mLastConnectedDeviceAddress)) {
                    final long saved_time = gtb.time;
                    gtb.time = JoH.tsl() - Constants.SECOND_IN_MS * 30; // when the reading was most likely taken
                    UserError.Log.e(TAG, "Munged meter reading time from: " + JoH.dateTimeText(saved_time) + " to " + JoH.dateTimeText(gtb.time));
                }
                if (JoH.quietratelimit(GLUCOSE_READING_MARKER, 10)) {
                    PersistentStore.setBoolean(GLUCOSE_READING_MARKER + mLastConnectedDeviceAddress, true);
                }
            }


            final BloodTest bt = BloodTest.create((gtb.time - ct.timediff) + gtb.offsetMs(), gtb.mgdl, BLUETOOTH_GLUCOSE_METER_TAG + ":\n" + mLastManufacturer + "   " + mLastConnectedDeviceAddress, gtb.getUuid().toString());
            if (bt != null) {
                UserError.Log.d(TAG, "Successfully created new BloodTest: " + bt.toS());
                bt.glucoseReadingRx = gtb; // add reference
                lastBloodTest = bt;
                UserError.Log.uel(TAG, "New blood test data: " + BgGraphBuilder.unitized_string_static(bt.mgdl) + " @ " + JoH.dateTimeText(bt.timestamp) + " " + bt.source);

                Inevitable.task("evaluate-meter-records", 2000, this::evaluateLastRecords);

            } else {
                if (d) UserError.Log.d(TAG, "Failed to create BloodTest record");
            }
        } else {
            UserError.Log.d(TAG, "Ignoring control solution test");
        }
    }

    private synchronized void processCharacteristicChange(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {

        // extra debug
        if (d) {
            UserError.Log.d(TAG, "charactersiticChanged: " + characteristic.getUuid().toString() + " " + JoH.bytesToHex(characteristic.getValue()));
        }

        if (GLUCOSE_CHARACTERISTIC.equals(characteristic.getUuid())) {

            final GlucoseReadingRx gtb = new GlucoseReadingRx(characteristic.getValue(), gatt.getDevice().getAddress());
            UserError.Log.d(TAG, "Result: " + gtb.toString());
            if (ct == null) {
                statusUpdate("Cannot process glucose record as we do not know device time!");
            } else {
                if (JoH.quietratelimit("mark-meter-device-success", 10)) {
                    markDeviceAsSuccessful(gatt);
                }
                statusUpdate("Glucose Record: " + JoH.dateTimeText((gtb.time - ct.timediff) + gtb.offsetMs()) + "\n" + unitized_string_with_units_static(gtb.mgdl));

                if (playSounds() && JoH.ratelimit("bt_meter_data_in", 1))
                    JoH.playResourceAudio(R.raw.bt_meter_data_in);

                if (!gtb.contextInfoFollows) {
                    processGlucoseReadingRx(gtb);
                } else {
                    UserError.Log.d(TAG, "Record has context information so delaying processing");
                    awaitingContext = gtb;
                }
            }

        } else if (RECORDS_CHARACTERISTIC.equals(characteristic.getUuid())) {
            UserError.Log.d(TAG, "Change notification for RECORDS: " + JoH.bytesToHex(characteristic.getValue()));
        } else if (CONTEXT_CHARACTERISTIC.equals(characteristic.getUuid())) {
            UserError.Log.d(TAG, "Change notification for CONTEXT: " + JoH.bytesToHex(characteristic.getValue()));
            processContextData(characteristic.getValue());
        } else if (VERIO_F7A3_NOTIFICATION.equals(characteristic.getUuid())) {
            UserError.Log.d(TAG, "Change notification for VERIO: " + JoH.bytesToHex(characteristic.getValue()));
            try {
                final GlucoseReadingRx gtb = VerioHelper.parseMessage(characteristic.getValue());
                if (gtb != null) {
                    // if this was a BG reading we could process (offset already pre-calculated in time) - not robust against meter clock changes
                    markDeviceAsSuccessful(gatt);
                    statusUpdate("Glucose Record: " + JoH.dateTimeText((gtb.time + gtb.offsetMs())) + "\n" + unitized_string_with_units_static(gtb.mgdl));

                    if (playSounds() && JoH.ratelimit("bt_meter_data_in", 1))
                        JoH.playResourceAudio(R.raw.bt_meter_data_in);
                    final BloodTest bt = BloodTest.create((gtb.time) + gtb.offsetMs(), gtb.mgdl, BLUETOOTH_GLUCOSE_METER_TAG + ":\n" + mLastManufacturer + "   " + mLastConnectedDeviceAddress);
                    if (bt != null) {
                        UserError.Log.d(TAG, "Successfully created new BloodTest: " + bt.toS());
                        bt.glucoseReadingRx = gtb; // add reference
                        lastBloodTest = bt;
                        UserError.Log.uel(TAG, "New verio blood test data: " + BgGraphBuilder.unitized_string_static(bt.mgdl) + " @ " + JoH.dateTimeText(bt.timestamp) + " " + bt.source);

                        final long record_time = lastBloodTest.timestamp;
                        // TODO better replaced with Inevitable Task
                        JoH.runOnUiThreadDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (lastBloodTest.timestamp == record_time) {
                                    ct = new CurrentTimeRx(); // zero hack
                                    evaluateLastRecords();
                                }
                            }
                        }, 1000);

                    } else {
                        if (d) UserError.Log.d(TAG, "Failed to create BloodTest record");
                    }
                }
            } catch (Exception e) {
                UserError.Log.wtf(TAG, "Got exception processing Verio data " + e);
            }
        } else {
            UserError.Log.e(TAG, "Unknown characteristic change: " + characteristic.getUuid().toString() + " " + JoH.bytesToHex(characteristic.getValue()));
        }
    }

    private synchronized void processContextData(byte[] context) {
        final ContextRx crx = new ContextRx(context);
        if (awaitingContext != null) {

            if (awaitingContext.sequence == crx.sequence) {
                if (crx.ketone()) {
                    UserError.Log.e(TAG, "Received Ketone data: " + awaitingContext.asKetone());
                    awaitingContext = null;
                } else {
                    if (crx.normalRecord()) {
                        processGlucoseReadingRx(awaitingContext);
                        awaitingContext = null;
                    } else {
                        UserError.Log.e(TAG, "Received context packet but we're not sure what its for: " + crx.toString());
                    }
                }
            } else {
                UserError.Log.e(TAG, "Received out of sequence context: " + awaitingContext.sequence + " vs " + crx.toString());
            }

        } else {
            UserError.Log.d(TAG, "Received context but nothing awaiting context: " + crx.toString());
        }
    }

    private static final BroadcastReceiver mPairingRequestRecevier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            JoH.doPairingRequest(context, this, intent, mBluetoothDeviceAddress);
        }
    };

    public static void verioScheduleRequestBg(int r) {
        Bluetooth_CMD.write(VERIO_F7A1_SERVICE, VERIO_F7A2_WRITE, VerioHelper.getRecordCMD(r), "verio get record " + r); // wording used to update request counter
    }

    // decide what to do with newest data
    private synchronized void evaluateLastRecords() {
        if (lastBloodTest != null) {
            GcmActivity.syncBloodTests();

            final boolean delay_calibration = true;
            final GlucoseReadingRx lastGlucoseRecord = lastBloodTest.glucoseReadingRx;
            if ((lastGlucoseRecord != null) && (lastGlucoseRecord.device != null) && (ct != null)) {
                final String sequence_id = "last-btm-sequence-" + lastGlucoseRecord.device;
                final String timestamp_id = "last-btm-timestamp" + lastGlucoseRecord.device;
                // sequence numbers start from 0 so we add 1
                if ((lastGlucoseRecord.sequence + 1) > PersistentStore.getLong(sequence_id)) {
                    PersistentStore.setLong(sequence_id, lastGlucoseRecord.sequence + 1);
                    // get adjusted timestamp
                    if (lastBloodTest.timestamp > PersistentStore.getLong(timestamp_id)) {
                        PersistentStore.setLong(timestamp_id, lastBloodTest.timestamp);
                        Log.d(TAG, "evaluateLastRecords: appears to be a new record: sequence:" + lastGlucoseRecord.sequence);
                        JoH.runOnUiThreadDelayed(Home::staticRefreshBGCharts, 300);
                        if (Pref.getBooleanDefaultFalse("bluetooth_meter_for_calibrations")
                                || Pref.getBooleanDefaultFalse("bluetooth_meter_for_calibrations_auto")) {
                            final long time_since = JoH.msSince(lastBloodTest.timestamp);
                            if (time_since >= 0) {
                                if (time_since < (12 * Constants.HOUR_IN_MS)) {
                                    final Calibration calibration = Calibration.lastValid();
                                    // check must also be younger than most recent calibration
                                    if ((calibration == null) || (lastBloodTest.timestamp > calibration.timestamp)) {
                                        UserError.Log.ueh(TAG, "Prompting for calibration for: " + BgGraphBuilder.unitized_string_with_units_static(lastBloodTest.mgdl) + " from: " + JoH.dateTimeText(lastBloodTest.timestamp));
                                        JoH.clearCache();
                                        Home.startHomeWithExtra(getApplicationContext(), Home.HOME_FULL_WAKEUP, "1");
                                        JoH.runOnUiThreadDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                Home.staticRefreshBGCharts();
                                                // requires offset in past

                                                if ((Pref.getBooleanDefaultFalse("bluetooth_meter_for_calibrations_auto") && isSlopeFlatEnough())) {
                                                    Log.d(TAG, "Slope flat enough for auto calibration");
                                                    if (!delay_calibration) {
                                                        Home.startHomeWithExtra(xdrip.getAppContext(),
                                                                Home.BLUETOOTH_METER_CALIBRATION,
                                                                BgGraphBuilder.unitized_string_static(lastBloodTest.mgdl),
                                                                Long.toString(time_since),
                                                                "auto");
                                                    } else {
                                                        Log.d(TAG, "Delaying calibration for later");
                                                        JoH.static_toast_long("Waiting for 15 minutes more sensor data for calibration");
                                                    }
                                                } else {
                                                    if (Pref.getBooleanDefaultFalse("bluetooth_meter_for_calibrations")) {
                                                        // manual calibration
                                                        Home.startHomeWithExtra(xdrip.getAppContext(),
                                                                Home.BLUETOOTH_METER_CALIBRATION,
                                                                BgGraphBuilder.unitized_string_static(lastBloodTest.mgdl),
                                                                Long.toString(time_since),
                                                                "manual");
                                                    } else {
                                                        Log.d(TAG, "Not flat enough slope for auto calibration and manual calibration not enabled");
                                                    }
                                                }
                                            }
                                        }, 500);
                                    } else {
                                        UserError.Log.e(TAG, "evaluateLastRecords: meter reading is at least as old as last calibration - ignoring");
                                    }
                                } else {
                                    UserError.Log.e(TAG, "evaluateLastRecords: meter reading is too far in the past: " + JoH.dateTimeText(lastBloodTest.timestamp));
                                }
                            } else {
                                UserError.Log.e(TAG, "evaluateLastRecords: time is in the future - ignoring");
                            }
                        }
                    }
                } else {
                    UserError.Log.d(TAG, "evaluateLastRecords: sequence isn't newer");
                }
            } else {
                UserError.Log.e(TAG, "evaluateLastRecords: Data missing for evaluation");
            }
        } else {
            UserError.Log.e(TAG, "evaluateLastRecords: lastBloodTest is Null!!");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        //return mBinder;
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    private boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                UserError.Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            UserError.Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        startup();
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    private synchronized boolean connect(final String address) {
        if ((address == null) || (address.equals("00:00:00:00:00:00"))) {
            if (d) Log.d(TAG, "ignoring connect with null address");
            return false;
        }
        Log.d(TAG, "connect() called with address: " + address);
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // if device address changes we should probably disconnect/close
        if ((mConnectionState == STATE_CONNECTED)
                && (mLastConnectedDeviceAddress.equals(address))
                && (JoH.ratelimit("bt-meter-connect-repeat", 7))) {
            Log.e(TAG, "We are already connected - not connecting");
            if (service_discovery_count == 0) discover_services();
            return false;
        }

        // Previously connected device.  Try to reconnect.
        statusUpdate("Trying to connect to: " + address);
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            if (d) Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            statusUpdate("Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
        refreshDeviceCache(mBluetoothGatt);
        if (d) Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    private void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }


    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    private List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

    // currently not used, will need updating if it is
    @TargetApi(21)
    private void initScanCallback() {
        Log.d(TAG, "init v21 ScanCallback()");

        // v21 version
        if (Build.VERSION.SDK_INT >= 21) {
            mScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    Log.i(TAG, "onScanResult result: " + result.toString());
                    final BluetoothDevice btDevice = result.getDevice();
                    scanLeDevice(false); // stop scanning
                    connect(btDevice.getAddress());
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    for (ScanResult sr : results) {
                        Log.i("ScanResult - Results", sr.toString());
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    Log.e(TAG, "Scan Failed Error Code: " + errorCode);
                    if (errorCode == 1) {
                        Log.e(TAG, "Already Scanning: "); // + isScanning);
                        //isScanning = true;
                    } else if (errorCode == 2) {
                        // reset bluetooth?
                    }
                }
            };
        }
    }

    private void scanLeDevice(final boolean enable) {
        final boolean force_old = true;
        statusUpdate(enable ? "Starting Scanning" + "\nMake sure meter is turned on - For pairing hold the meter power button until it flashes blue" : "Stopped Scanning");
        if (enable) {
            JoH.runOnUiThreadDelayed(new Runnable() {
                @Override
                public void run() {
                    if ((Build.VERSION.SDK_INT < 21) || (force_old)) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mLEScanner.stopScan(mScanCallback);

                    }
                    // check scan still running and we haven't been restarted in connect mode
                    // and stop the service if so
                }
            }, SCAN_PERIOD);
            if ((Build.VERSION.SDK_INT < 21) || (force_old)) {
                Log.d(TAG, "Starting old scan");
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
                Log.d(TAG, "Starting api21 scan");
            }
        } else {
            if ((Build.VERSION.SDK_INT < 21) || (force_old)) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    private void beginScan() {
        if (Build.VERSION.SDK_INT >= 21) {
            if (d) Log.d(TAG, "Preparing for scan...");

            // set up v21 scanner
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<>();

            filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(GLUCOSE_SERVICE)).build());
        }
        // all api versions
        scanLeDevice(true);
    }

    public static void immortality() {
        if (started_at == -1) {
            startIfEnabled();
        } else {
            startIfNoRecentData();
        }
    }

    public static void startIfNoRecentData() {
        if (JoH.quietratelimit("bluetooth-recent-check", 1800)) {
            if (Pref.getBoolean("bluetooth_meter_enabled", false)) {
                final List<BloodTest> btl = BloodTest.lastMatching(1, BLUETOOTH_GLUCOSE_METER_TAG + "%");
                if ((btl == null) || (btl.size() == 0) || (JoH.msSince(btl.get(0).created_timestamp) > Constants.HOUR_IN_MS * 6)) {
                    if (JoH.pratelimit("restart_bluetooth_service", 3600 * 5)) {
                        UserError.Log.uel(TAG, "Restarting Bluetooth Glucose meter service");
                        startIfEnabled();
                    }
                }
            }
        }
    }

    public static void startIfEnabled() {
        if (Pref.getBoolean("bluetooth_meter_enabled", false)) {
            final String meter_address = Pref.getStringDefaultBlank("selected_bluetooth_meter_address");
            if (meter_address.length() > 5) {
                if (JoH.pratelimit("bluetooth-glucose-immortality", 10)) {
                    UserError.Log.d(TAG, "Starting Service");
                    start_service(meter_address);
                } else {
                    UserError.Log.e(TAG, "Not starting due to rate limit");
                }
            }
        }
    }

    // remote api for stopping
    public static void stop_service() {
        final Intent stop_intent = new Intent(xdrip.getAppContext(), BluetoothGlucoseMeter.class);
        xdrip.getAppContext().stopService(stop_intent);
    }

    // remote api for starting
    public static void start_service(String connect_address) {
        stop_service(); // is this right?
        final Intent start_intent = new Intent(xdrip.getAppContext(), BluetoothGlucoseMeter.class);
        if ((connect_address != null) && (connect_address.length() > 0)) {
            if (connect_address.equals("auto")) {
                connect_address = Pref.getStringDefaultBlank("selected_bluetooth_meter_address");
            }
            start_intent.putExtra("service_action", "connect");
            start_intent.putExtra("connect_address", connect_address);
        } else {
            start_intent.putExtra("service_action", "scan");
        }
        xdrip.getAppContext().startService(start_intent);
    }

    // remote api for forgetting
    public static void start_forget(String forget_address) {
        stop_service(); // is this right?
        final Intent start_intent = new Intent(xdrip.getAppContext(), BluetoothGlucoseMeter.class);
        if ((forget_address != null) && (forget_address.length() > 0)) {
            start_intent.putExtra("service_action", "forget");
            start_intent.putExtra("forget_address", forget_address);
            xdrip.getAppContext().startService(start_intent);
        }
    }

    public static void sendImmediateData(UUID service, UUID characteristic, byte[] data, String notes) {
        Log.d(TAG, "Sending immediate data: " + notes);
        Bluetooth_CMD.process_queue_entry(Bluetooth_CMD.gen_write(service, characteristic, data, notes));
    }

    private static final String PREF_HIGHEST_SEQUENCE = "bt-glucose-sequence-max-";

    private static int getHighestSequence() {
        return (int) PersistentStore.getLong(PREF_HIGHEST_SEQUENCE + mBluetoothDeviceAddress);
    }

    private static void setHighestSequence(int sequence) {
        highestSequenceStore = sequence;
        Inevitable.task("set-bt-glucose-highest", 1000, new Runnable() {
            @Override
            public void run() {
                if (highestSequenceStore > 0) {
                    PersistentStore.setLong(PREF_HIGHEST_SEQUENCE + mBluetoothDeviceAddress, highestSequenceStore);
                }
            }
        });
    }


    // jamorham bluetooth queue methodology
    private static class Bluetooth_CMD {

        final static long QUEUE_TIMEOUT = 10000;
        private static final int MAX_RESEND = 3;
        static long queue_check_scheduled = 0;
        public long timestamp;
        public String cmd;
        public byte[] data;
        public String note;
        public UUID service;
        public UUID characteristic;
        public int resent;

        private Bluetooth_CMD(String cmd, UUID service, UUID characteristic, byte[] data, String note) {
            this.cmd = cmd;
            this.service = service;
            this.characteristic = characteristic;
            this.data = data;
            this.note = note;
            this.timestamp = System.currentTimeMillis();
            this.resent = 0;
        }

        private synchronized static void add_item(String cmd, UUID service, UUID characteristic, byte[] data, String note) {
            queue.add(gen_item(cmd, service, characteristic, data, note));
        }

        private static Bluetooth_CMD gen_item(String cmd, UUID service, UUID characteristic, byte[] data, String note) {
            final Bluetooth_CMD btc = new Bluetooth_CMD(cmd, service, characteristic, data, note);
            return btc;
        }

        private static Bluetooth_CMD gen_write(UUID service, UUID characteristic, byte[] data, String note) {
            return gen_item("W", service, characteristic, data, note);
        }

        private static void write(UUID service, UUID characteristic, byte[] data, String note) {
            add_item("W", service, characteristic, data, note);
        }

        private static void read(UUID service, UUID characteristic, String note) {
            add_item("R", service, characteristic, null, note);
        }

        private static void notify(UUID service, UUID characteristic, String note) {
            add_item("N", service, characteristic, new byte[]{0x01}, note);
        }

        private static Bluetooth_CMD gen_notify(UUID service, UUID characteristic, String note) {
            return gen_item("N", service, characteristic, new byte[]{0x01}, note);
        }

        private static void unnotify(UUID service, UUID characteristic, String note) {
            add_item("U", service, characteristic, new byte[]{0x00}, note);
        }

        private static void enable_indications(UUID service, UUID characteristic, String note) {
            add_item("D", service, characteristic, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, note);
        }

        private static void enable_notification_value(UUID service, UUID characteristic, String note) {
            add_item("D", service, characteristic, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, note);
        }

        private static Bluetooth_CMD gen_enable_notification_value(UUID service, UUID characteristic, String note) {
            return gen_item("D", service, characteristic, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, note);
        }

        private static synchronized void check_queue_age() {
            queue_check_scheduled = 0;
            if (!queue.isEmpty()) {
                final Bluetooth_CMD btc = queue.peek();
                if (btc != null) {
                    long queue_age = System.currentTimeMillis() - btc.timestamp;
                    if (d) Log.d(TAG, "check queue age.. " + queue_age + " on " + btc.note);
                    if (queue_age > QUEUE_TIMEOUT) {
                        statusUpdate("Timed out on: " + btc.note + (isBonded() ? "" : "\nYou may need to enable the meter's pairing mode by holding the power button when turning it on until it flashes blue"));
                        queue.clear();
                        last_queue_command = null;
                        close();
                        waitFor(3000);
                        //reconnect(); hmm we would like to reconnect here
                    }
                }
            } else {
                if (d) Log.d(TAG, "check queue age - queue is empty");
            }
        }

        private static synchronized void empty_queue() {
            queue.clear();
        }

        private static synchronized void delete_command(final UUID fromService, final UUID fromCharacteristic) {
            try {
                for (Bluetooth_CMD btc : queue) {
                    if (btc.service.equals(fromService) && btc.characteristic.equals(fromCharacteristic)) {
                        Log.d(TAG, "Removing: " + btc.note);
                        queue.remove(btc);
                        break; // currently we only ever need to do one so break for speed
                    }
                }
            } catch (Exception e) {
                Log.wtf("Got exception in delete: ", e);
            }
        }

        private static synchronized void transmute_command(final UUID fromService, final UUID fromCharacteristic,
                                                           final UUID toService, final UUID toCharacteristic) {
            try {
                for (Bluetooth_CMD btc : queue) {
                    if (btc.service.equals(fromService) && btc.characteristic.equals(fromCharacteristic)) {
                        btc.service = toService;
                        btc.characteristic = toCharacteristic;
                        Log.d(TAG, "Transmuted service: " + fromService + " -> " + toService);
                        Log.d(TAG, "Transmuted charact: " + fromCharacteristic + " -> " + toCharacteristic);
                        break; // currently we only ever need to do one so break for speed
                    }
                }
            } catch (Exception e) {
                Log.wtf("Got exception in transmute: ", e);
            }
        }

        private static synchronized void replace_command(final UUID fromService, final UUID fromCharacteristic, String type,
                                                         Bluetooth_CMD btc_replacement) {
            try {
                for (Bluetooth_CMD btc : queue) {
                    if (btc.service.equals(fromService)
                            && btc.characteristic.equals(fromCharacteristic)
                            && btc.cmd.equals(type)) {
                        btc.service = btc_replacement.service;
                        btc.characteristic = btc_replacement.characteristic;
                        btc.cmd = btc_replacement.cmd;
                        btc.data = btc_replacement.data;
                        btc.note = btc_replacement.note;
                        Log.d(TAG, "Replaced service: " + fromService + " -> " + btc_replacement.service);
                        Log.d(TAG, "Replaced charact: " + fromCharacteristic + " -> " + btc_replacement.characteristic);
                        Log.d(TAG, "Replaced     cmd: " + btc_replacement.cmd);
                        break; // currently we only ever need to do one so break for speed
                    }
                }
            } catch (Exception e) {
                Log.wtf("Got exception in replace: ", e);
            }
        }

        private static synchronized void insert_after_command(final UUID fromService, final UUID fromCharacteristic,
                                                              Bluetooth_CMD btc_replacement) {

            final ConcurrentLinkedQueue<Bluetooth_CMD> tmp_queue = new ConcurrentLinkedQueue<>();
            try {
                for (Bluetooth_CMD btc : queue) {
                    tmp_queue.add(btc);
                    if (btc.service.equals(fromService) && btc.characteristic.equals(fromCharacteristic)) {
                        if (btc_replacement != null) tmp_queue.add(btc_replacement);
                        btc_replacement = null; // first only item
                    }
                }

                queue.clear();
                queue.addAll(tmp_queue);

            } catch (Exception e) {
                Log.wtf("Got exception in insert_after: ", e);
            }
        }


        private synchronized static void poll_queue() {
            poll_queue(false);
        }

        private synchronized static void poll_queue(boolean startup) {

            if (mConnectionState == STATE_DISCONNECTED) {
                Log.e(TAG, "Connection is disconnecting, deleting queue");
                last_queue_command = null;
                queue.clear();
                return;
            }

            if (mBluetoothGatt == null) {
                Log.e(TAG, "mBluetoothGatt is null - connect and defer");
                // connect?
                // set timer?
                return;
            }
            if (startup && queue.size() > 1) {
                Log.d(TAG, "Queue busy deferring poll");
                // set timer??
                return;
            }

            final long time_now = System.currentTimeMillis();
            if ((time_now - queue_check_scheduled) > 10000) {
                JoH.runOnUiThreadDelayed(new Runnable() {
                    @Override
                    public void run() {
                        check_queue_age();
                    }
                }, QUEUE_TIMEOUT + 1000);
                queue_check_scheduled = time_now;
            } else {
                if (d) Log.d(TAG, "Queue check already scheduled");
            }

            if (ack_blocking()) {
                if (d) Log.d(TAG, "Queue blocked by awaiting ack");
                return;
            }

            Bluetooth_CMD btc = queue.poll();
            if (btc != null) {
                Log.d(TAG, "Processing queue " + btc.cmd + " :: " + btc.note + " :: " + btc.characteristic.toString() + " " + JoH.bytesToHex(btc.data));
                last_queue_command = btc;
                process_queue_entry(btc);
            } else {
                if (d) Log.d(TAG, "Queue empty");
            }
        }

        private static synchronized void retry_last_command(int status) {
            if (last_queue_command != null) {
                if (last_queue_command.resent <= MAX_RESEND) {
                    last_queue_command.resent++;
                    if (d) Log.d(TAG, "Delay before retry");
                    waitFor(200);
                    Log.d(TAG, "Retrying try:(" + last_queue_command.resent + ") last command: " + last_queue_command.note);
                    process_queue_entry(last_queue_command);
                } else {
                    Log.e(TAG, "Exceeded max resend for: " + last_queue_command.note);
                    last_queue_command = null;

                }
            } else {
                Log.d(TAG, "No last command to retry");
            }
        }


        private static void process_queue_entry(Bluetooth_CMD btc) {

            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }

            BluetoothGattService service = null;
            final BluetoothGattCharacteristic characteristic;
            if (btc.service != null) service = mBluetoothGatt.getService(btc.service);
            if ((service != null) || (btc.service == null)) {
                if ((service != null) && (btc.characteristic != null)) {
                    characteristic = service.getCharacteristic(btc.characteristic);
                } else {
                    characteristic = null;
                }
                if (characteristic != null) {
                    switch (btc.cmd) {
                        case "W":
                            characteristic.setValue(btc.data);
                            if (await_acks && (characteristic.getValue().length > 1)) {
                                awaiting_ack = true;
                                awaiting_data = true;
                                if (d) Log.d(TAG, "Setting await ack blocker 1");
                                if (btc.note.startsWith("verio get record")) { // notify which record we are processing
                                    VerioHelper.updateRequestedRecord(Integer.parseInt(btc.note.substring(17)));
                                }
                            }
                            JoH.runOnUiThreadDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (!mBluetoothGatt.writeCharacteristic(characteristic)) {
                                            Log.d(TAG, "Failed in write characteristic");
                                            waitFor(150);
                                            if (!mBluetoothGatt.writeCharacteristic(characteristic)) {
                                                Log.e(TAG, "Failed second time in write charactersitic");
                                            }
                                        }
                                    } catch (NullPointerException e) {
                                        UserError.Log.e(TAG, "Got null pointer exception writing characteristic - probably temporary failure");
                                    }
                                }
                            }, 0);
                            break;

                        case "R":
                            mBluetoothGatt.readCharacteristic(characteristic);
                            break;

                        case "N":
                            mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                            waitFor(100);
                            poll_queue(); // we don't get an event from this
                            break;

                        case "U":
                            mBluetoothGatt.setCharacteristicNotification(characteristic, false);
                            break;

                        case "D":
                            final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                    CLIENT_CHARACTERISTIC_CONFIG);
                            descriptor.setValue(btc.data);
                            mBluetoothGatt.writeDescriptor(descriptor);
                            break;

                        default:
                            Log.e(TAG, "Unknown queue cmd: " + btc.cmd);

                    } // end switch

                } else {
                    Log.e(TAG, "Characteristic was null!!!!");
                }

            } else {
                Log.e(TAG, "Got null service error on: " + btc.service);
            }
        }

    }
}