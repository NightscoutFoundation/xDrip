package com.eveningoutpost.dexdrip.Services;

/**
 * Created by jcostik1 on 3/15/16.
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.eveningoutpost.dexdrip.G5Model.AuthChallengeRxMessage;
import com.eveningoutpost.dexdrip.G5Model.AuthChallengeTxMessage;

import com.eveningoutpost.dexdrip.G5Model.AuthRequestTxMessage;
import com.eveningoutpost.dexdrip.G5Model.AuthStatusRxMessage;
import com.eveningoutpost.dexdrip.G5Model.BluetoothServices;
import com.eveningoutpost.dexdrip.G5Model.BondRequestTxMessage;
import com.eveningoutpost.dexdrip.G5Model.DisconnectTxMessage;
import com.eveningoutpost.dexdrip.G5Model.Extensions;
import com.eveningoutpost.dexdrip.G5Model.SensorRxMessage;
import com.eveningoutpost.dexdrip.G5Model.SensorTxMessage;
import com.eveningoutpost.dexdrip.G5Model.TransmitterStatus;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.G5Model.Transmitter;

//KS not needed
/*
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.utils.BgToSpeech;
import com.eveningoutpost.dexdrip.xdrip;
*/

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class G5CollectionService extends Service {

    private final static String TAG = "wear" + G5CollectionService.class.getSimpleName();

    private static final Object short_lock = new Object();
    private static boolean cycling_bt = false;
    private static boolean service_running = false;
    private static boolean scan_scheduled = false;

    //KS private ForegroundServiceStarter foregroundServiceStarter;

    public Service service;
    //KS private BgToSpeech bgToSpeech;
    private static PendingIntent pendingIntent;

    private android.bluetooth.BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private BluetoothGatt mGatt;
    private Transmitter defaultTransmitter;
    public AuthStatusRxMessage authStatus = null;
    public AuthRequestTxMessage authRequest = null;

    private BluetoothGattService cgmService;// = gatt.getService(UUID.fromString(BluetoothServices.CGMService));
    private BluetoothGattCharacteristic authCharacteristic;// = cgmService.getCharacteristic(UUID.fromString(BluetoothServices.Authentication));
    private BluetoothGattCharacteristic controlCharacteristic;//
    private BluetoothGattCharacteristic commCharacteristic;//

    private BluetoothDevice device;
    private Boolean isBondedOrBonding = false;
    public static boolean keep_running = true;

    private ScanSettings settings;
    private List<ScanFilter> filters;
    private SharedPreferences prefs;

    private static boolean isScanning = false;
    private boolean isConnected = false;
    private boolean encountered133 = false;
    private Handler handler;
    public int max133Retries = 5;
    public int max133RetryCounter = 0;
    private static int disconnected133 = 0;
    private static int disconnected59 = 0;
    public boolean isIntialScan = true;
    public static Timer scan_interval_timer = new Timer();
    public ArrayList<Long> advertiseTimeMS = new ArrayList<Long>();
    public long timeInMillisecondsOfLastSuccessfulSensorRead = new Date().getTime();
    private int maxScanIntervalInMilliseconds = 5 * 1000; //seconds *1k
    private int maxScanCycles = 24;
    private int scanCycleCount = 0;
    public Context mContext;//KS

    StringBuilder log = new StringBuilder();


    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initScanCallback();
        }
        advertiseTimeMS.add((long)0);
        service = this;
        mContext = getApplicationContext();//KS
//        final IntentFilter bondintent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
//        registerReceiver(mPairReceiver, bondintent);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        listenForChangeInSettings();
        handler = new Handler(getApplicationContext().getMainLooper());
    }

    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

            if(key.compareTo("run_ble_scan_constantly") == 0 || key.compareTo("always_unbond_G5") == 0
                    || key.compareTo("always_get_new_keys") == 0 || key.compareTo("run_G5_ble_tasks_on_uithread") == 0) {
                Log.i(TAG, "G5 Setting Change");
                cycleScan(0);
            }

        }
    };

    public void listenForChangeInSettings() {
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        // TODO do we need an unregister!?
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final PowerManager.WakeLock wl = JoH.getWakeLock(mContext, "g5-start-service", 120000);//KS add context
        try {
            if ((!service_running) && (keep_running)) {
                service_running = true;

                Log.d(TAG, "onG5StartCommand wakeup: "+JoH.dateTimeText(JoH.tsl()));

                scanCycleCount = 0;
                mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = mBluetoothManager.getAdapter();

                if (mGatt != null) {
                    mGatt.close();
                    mGatt = null;
                }

                if (Sensor.isActive()) {
                    setupBluetooth();
                    Log.d(TAG, "Active Sensor");

                } else {
                    stopScan();
                    Log.d(TAG, "No Active Sensor");
                }

                service_running=false;

                return START_STICKY;
            } else {
                Log.e(TAG,"jamorham service already active!");
                keepAlive();
                return START_NOT_STICKY;
            }

        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private synchronized void getTransmitterDetails() {
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Log.d(TAG, "Transmitter: " + prefs.getString("dex_txid", "ABCDEF"));
        defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
        isBondedOrBonding = false;
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if ((pairedDevices != null) && (pairedDevices.size() > 0)) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName() != null) {

                    String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                    String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());

                    if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                        isBondedOrBonding = true;
                    } else {
                        isIntialScan = true;
                    }

                }
            }
        }
        Log.d(TAG, "Bonded? " + isBondedOrBonding.toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopScan();

        Log.d(TAG, "onDestroy");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        scan_interval_timer.cancel();
        if (pendingIntent != null) {
            Log.d(TAG, "onDestroy stop Alarm pendingIntent");
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarm.cancel(pendingIntent);
        }
        // close gatt
        if (mGatt != null) {
            try {
                mGatt.close();
            } catch (NullPointerException e) {
                Log.d(TAG, "concurrency related null pointer exception in close");
            }
        }

//        close();
//        setRetryTimer();
//        foregroundServiceStarter.stop();
//        unregisterReceiver(mPairReceiver);
//        BgToSpeech.tearDownTTS();
        Log.i(TAG, "onDestroy SERVICE STOPPED");
    }

    public synchronized void keepAlive() {
        keepAlive(0);
    }

    public synchronized void keepAlive(int wake_in_ms) {
        if (!keep_running) return;
        if (JoH.ratelimit("G5-keepalive", 5)) {
            long wakeTime;
            if (wake_in_ms==0) {
                wakeTime = getNextAdvertiseTime() - 60 * 1000;
            } else {
                wakeTime = Calendar.getInstance().getTimeInMillis() + wake_in_ms;
            }
            //Log.e(TAG, "Delay Time: " + minuteDelay);
            Log.e(TAG, "Scheduling Wake Time: in " +  JoH.qs((wakeTime-JoH.tsl())/1000,0)+ " secs "+ JoH.dateTimeText(wakeTime));
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (pendingIntent != null)
                alarm.cancel(pendingIntent);
            pendingIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
            } else
                alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
        } else {
            Log.e(TAG, "Ignoring keepalive call due to ratelimit");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void setupBluetooth() {
        getTransmitterDetails();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //First time using the app or bluetooth was turned off?
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            Timer single_timer = new Timer();
            single_timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mBluetoothAdapter.enable();
                }
            }, 1000);
            single_timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    setupBluetooth();
                }
            }, 10000);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<>();
                //Only look for CGM.
                //filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(BluetoothServices.Advertisement)).build());
                String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                filters.add(new ScanFilter.Builder().setDeviceName("Dexcom" + transmitterIdLastTwo).build());
            }

            cycleScan(0);
        }
    }

    public void stopScan(){
        if (!isScanning) {
            Log.d(TAG, "alreadyStoppedScanning");
            return;
        }
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {

                try {

                    if (enforceMainThread()){
                        Handler iHandler = new Handler(Looper.getMainLooper());
                        iHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                stopLogic();
                            }
                        });
                    } else {
                        stopLogic();;
                    }


                } catch (NullPointerException e) {
                    //Known bug in Samsung API 21 stack
                    System.out.print("Caught the NullPointerException");
                }
            }
        }
    }

    private synchronized void stopLogic() {
        try {
            Log.e(TAG, "stopScan");
            try {
                mLEScanner.stopScan(mScanCallback);
            } catch (NullPointerException | IllegalStateException e) {
                Log.e(TAG, "Exception in stopLogic: " + e);
            }
            isScanning = false;
        } catch (IllegalStateException is) {

        }
    }

    public synchronized void cycleScan(int delay) {

        if (!keep_running) return;
        if (JoH.ratelimit("G5-timeout",60) || !scan_scheduled) {
            scan_scheduled=true;
            //Log.e(TAG, "Scheduling cycle scan, delay: " + delay);
            final Timer single_timer = new Timer();
            single_timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (scanConstantly()) {
                        startScan();
                    } else {
                        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            } else {

                                try {
                                    if (enforceMainThread()) {
                                        Handler iHandler = new Handler(Looper.getMainLooper());
                                        iHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                scanLogic();
                                            }
                                        });

                                    } else {
                                        scanLogic();
                                    }
                                } catch
                                        (NullPointerException e) {
                                    //Known bug in Samsung API 21 stack
                                    System.out.print("Caught the NullPointerException");
                                } finally {
                                    scan_scheduled=false;
                                }
                            }
                        }
                    }
                    scan_scheduled=false;
                }
            }, delay);
        } else {
            Log.e(TAG,"jamorham blocked excessive scan schedule");
        }
    }

    private synchronized void scanLogic() {
        if (!keep_running) return;
        if (JoH.ratelimit("G5-scanlogic",2)) {
            try {
                mLEScanner.stopScan(mScanCallback);
                isScanning = false;
                if (!isConnected) {
                    mLEScanner.startScan(filters, settings, mScanCallback);
                    Log.w(TAG, "scan cycle start");
                }
                isScanning = true;
            } catch (IllegalStateException | NullPointerException is) {
                setupBluetooth();
            }


            scanCycleCount++;

            //Log.e(TAG, "MSSinceSensorRx: " + getMillisecondsSinceLastSuccesfulSensorRead());
            //if it isn't the initial scan, rescan for maxScanCycles
            if (!isIntialScan && scanCycleCount > maxScanCycles) {
                scan_interval_timer.cancel();
                scan_interval_timer = new Timer();
                scan_interval_timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //Log.e(TAG, "cycling scan to stop until expected advertisement");
                        if (isScanning) {
                            keepAlive();
                        }
                        stopScan();
                    }
                }, maxScanIntervalInMilliseconds);
            }
            //last ditch
            else if (!isIntialScan && getMillisecondsSinceLastSuccesfulSensorRead() > 11 * 60 * 1000) {
                Log.e(TAG, "MSSinceSensorRx: " + getMillisecondsSinceLastSuccesfulSensorRead());
                isIntialScan = true;
                cycleBT();
            }
            //startup or re-auth, sit around and wait for tx to advertise
            else {
                scan_interval_timer.cancel();
                scan_interval_timer = new Timer();
                scan_interval_timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //Log.e(TAG, "cycling scan");
                        cycleScan(0);
                    }
                }, maxScanIntervalInMilliseconds);
            }
        }
    }

    public synchronized void startScan() {
        android.util.Log.e(TAG, "Initial scan?" + isIntialScan);
        if (isScanning) {
            Log.d(TAG, "alreadyScanning");
            scan_interval_timer.cancel();
            return;
        }

        getTransmitterDetails();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            setupBluetooth();
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                setupLeScanCallback();
                mBluetoothAdapter.startLeScan(new UUID[]{BluetoothServices.Advertisement}, mLeScanCallback);
            } else {
                if (enforceMainThread()){
                    Handler iHandler = new Handler(Looper.getMainLooper());
                    iHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            startLogic();
                        }
                    });
                } else {
                    startLogic();
                }
                Log.e(TAG, "startScan normal");
            }
        }
    }

    private synchronized void startLogic() {
        try {
            isScanning = true;
            mLEScanner.startScan(filters, settings, mScanCallback);
        } catch (Exception e) {
            isScanning = false;
            setupBluetooth();
        }
    }

    private synchronized void cycleBT(boolean t) {
        Log.e(TAG, "cycleBT special: count:" + disconnected133 + " / "+ disconnected59);
        if ((disconnected133 < 2) && (disconnected59 < 2)) {
            cycleBT();
        } else {
            Log.e(TAG, "jamorham special restart");
            keepAlive(10000); // retry in 10 seconds

            // close gatt
            if (mGatt != null) {
                try {
                    mGatt.close();
                } catch (NullPointerException e) {
                    Log.d(TAG, "concurrency related null pointer exception in close");
                }
            }
            disconnected133 = 0;
            disconnected59 = 0;
            stopSelf();
        }
    }

    private synchronized void cycleBT(){
        synchronized (short_lock) {
            if (JoH.ratelimit("cyclebt",10)) {
                if (cycling_bt) {
                    Log.e(TAG, "jamorham Already concurrent BT cycle in progress!");
                    return;
                }
                encountered133 = false;
                stopScan();
                Log.e(TAG, "Cycling BT-gatt - disabling BT");
                mBluetoothAdapter.disable();
                Timer single_timer = new Timer();
                single_timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mBluetoothAdapter.enable();
                        Log.e(TAG, "Cycling BT-gatt - enableing BT");
                        cycling_bt = false;
                    }
                }, 3000);
            }
            keepAlive();
        }
    }

    void forgetDevice() {
        Transmitter defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName() != null) {

                    String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                    String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());
                    Log.e(TAG, "removeBond");
                    if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                        try {
                            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
                            m.invoke(device, (Object[]) null);
                            getTransmitterDetails();
                        } catch (Exception e) { Log.e("SystemStatus", e.getMessage(), e); }
                    }

                }
            }
        }
    }

    // API 18 - 20
    //@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setupLeScanCallback() {
        if (mLeScanCallback == null) {
            mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    // Check if the device has a name, the Dexcom transmitter always should. Match it with the transmitter id that was entered.
                    // We get the last 2 characters to connect to the correct transmitter if there is more than 1 active or in the room.
                    // If they match, connect to the device.
                    if (device.getName() != null) {
                        String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                        String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());

                        if (transmitterIdLastTwo.toUpperCase().equals(deviceNameLastTwo.toUpperCase())) {
                            connectToDevice(device);
                        }
                    }
                }
            };
        }
    }

    private ScanCallback mScanCallback;

    //@TargetApi(21)
    private void initScanCallback(){
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                android.util.Log.i(TAG, "result: " + result.toString());
                BluetoothDevice btDevice = result.getDevice();
//                // Check if the device has a name, the Dexcom transmitter always should. Match it with the transmitter id that was entered.
//                // We get the last 2 characters to connect to the correct transmitter if there is more than 1 active or in the room.
//                // If they match, connect to the device.
                if (btDevice.getName() != null) {
                    String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                    String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(btDevice.getName());

                    if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                        if (advertiseTimeMS.size() > 0)
                            if ((new Date().getTime() - advertiseTimeMS.get(advertiseTimeMS.size()-1)) > 2.5*60*1000)
                                advertiseTimeMS.clear();
                        advertiseTimeMS.add(new Date().getTime());
                        isIntialScan = false;
                        //device = btDevice;
                        device = mBluetoothAdapter.getRemoteDevice(btDevice.getAddress());
                        stopScan();
                        connectToDevice(btDevice);
                    } else {
                        //stopScan(10000);
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "Scan Failed Error Code: " + errorCode);
                if (errorCode == 1) {
                    android.util.Log.e(TAG, "Already Scanning: " + isScanning);
                    //isScanning = true;
                } else if (errorCode == 2){
                    cycleBT();
                }
            }
        };
    }

    public synchronized void fullAuthenticate() {

        if (alwaysUnbond()) {
            forgetDevice();
        }
        try {
            android.util.Log.i(TAG, "Start Auth Process(fullAuthenticate)");
            authRequest = new AuthRequestTxMessage();
            if (authCharacteristic != null) {
                authCharacteristic.setValue(authRequest.byteSequence);
                android.util.Log.i(TAG, authRequest.byteSequence.toString());
                mGatt.writeCharacteristic(authCharacteristic);
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Got null pointer in fullAuthenticate: " + e);
        }
    }

    public synchronized void authenticate() {
        try {
            mGatt.setCharacteristicNotification(authCharacteristic, true);
            if (!mGatt.readCharacteristic(authCharacteristic)) {
                android.util.Log.e(TAG, "onCharacteristicRead : ReadCharacteristicError");
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Got Nullpointer exception in authenticate(): " + e);
        }
    }

    public synchronized void getSensorData() {
        android.util.Log.i(TAG, "Request Sensor Data");
        try {
            if (mGatt != null) {
                mGatt.setCharacteristicNotification(controlCharacteristic, true);
                BluetoothGattDescriptor descriptor = controlCharacteristic.getDescriptor(BluetoothServices.CharacteristicUpdateNotification);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                SensorTxMessage sensorTx = new SensorTxMessage();
                controlCharacteristic.setValue(sensorTx.byteSequence);
                mGatt.writeDescriptor(descriptor);
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "Got null pointer in getSensorData() " + e);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = null;

    private synchronized void connectToDevice(BluetoothDevice device) {
        if (mGatt != null) {
            Log.i(TAG, "BGatt isnt null, Closing.");
            try {
                mGatt.close();
            } catch (NullPointerException e) {
                // concurrency related null pointer
            }
            mGatt = null;
        }
        android.util.Log.i(TAG, "Request Connect");
        if (enforceMainThread()){
            Handler iHandler = new Handler(Looper.getMainLooper());
            final BluetoothDevice mDevice = device;
            iHandler.post(new Runnable() {
                @Override
                public void run() {

                    android.util.Log.i(TAG, "mGatt Null, connecting...");
                    android.util.Log.i(TAG, "connectToDevice On Main Thread? " + isOnMainThread());
                    mGatt = mDevice.connectGatt(getApplicationContext(), false, gattCallback);

                }
            });
        } else {
            android.util.Log.i(TAG, "mGatt Null, connecting...");
            android.util.Log.i(TAG, "connectToDevice On Main Thread? " + isOnMainThread());
            mGatt = device.connectGatt(getApplicationContext(), false, gattCallback);
        }


    }

    // Sends the disconnect tx message to our bt device.
    private synchronized void doDisconnectMessage(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        mGatt.setCharacteristicNotification(controlCharacteristic, false);
        DisconnectTxMessage disconnectTx = new DisconnectTxMessage();
        characteristic.setValue(disconnectTx.byteSequence);
        mGatt.writeCharacteristic(characteristic);
        mGatt.disconnect();
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, final int status, final int newState) {
            if (enforceMainThread()) {
                Handler iHandler = new Handler(Looper.getMainLooper());
                iHandler.post(new Runnable() {
                                  @Override
                                  public void run() { //Log.e(TAG, "last disconnect status? " + lastGattStatus);
                                      android.util.Log.i(TAG, "onConnectionStateChange On Main Thread? " + isOnMainThread());
                                      switch (newState) {
                                          case BluetoothProfile.STATE_CONNECTED:
                                              Log.e(TAG, "STATE_CONNECTED");
                                              isConnected = true;

                                              if (enforceMainThread()){
                                                  Handler iHandler = new Handler(Looper.getMainLooper());
                                                  iHandler.post(new Runnable() {
                                                      @Override
                                                      public void run() {
                                                          android.util.Log.i(TAG, "discoverServices On Main Thread? " + isOnMainThread());
                                                          if (mGatt != null)
                                                              mGatt.discoverServices();
                                                      }
                                                  });
                                              } else {
                                                  android.util.Log.i(TAG, "discoverServices On Main Thread? " + isOnMainThread());
                                                  if (mGatt != null)
                                                      mGatt.discoverServices();
                                              }


                                              stopScan();
                                              scan_interval_timer.cancel();
                                              keepAlive();
                                              break;
                                          case BluetoothProfile.STATE_DISCONNECTED:
                                              isConnected = false;
                                              if (isScanning) {
                                                  stopScan();
                                              }
                                              Log.e(TAG, "STATE_DISCONNECTED: " + status);
                                              if (mGatt != null) {
                                                  try {
                                                      mGatt.close();
                                                  } catch (NullPointerException e) { //
                                                  }
                                              }
                                                  mGatt = null;
                                              if (status == 0 && !encountered133) {// || status == 59) {
                                                  android.util.Log.i(TAG, "clean disconnect");
                                                  max133RetryCounter = 0;
                                                  if (scanConstantly())
                                                      cycleScan(15000);
                                              } else if (status == 133 || max133RetryCounter >= max133Retries) {
                                                  Log.e(TAG, "max133RetryCounter? " + max133RetryCounter);
                                                  Log.e(TAG, "Encountered 133: " + encountered133);
                                                  max133RetryCounter = 0;
                                                  disconnected133++;
                                                  cycleBT(true);
                                              } else if (encountered133) {
                                                  Log.e(TAG, "max133RetryCounter? " + max133RetryCounter);
                                                  Log.e(TAG, "Encountered 133: " + encountered133);
                                                  if (scanConstantly())
                                                      startScan();
                                                  else
                                                      cycleScan(0);
                                                  max133RetryCounter++;
                                              } else if (status == 129) {
                                                  forgetDevice();
                                              } else {
                                                  if (status == 59) {
                                                      disconnected59++;
                                                  }
                                                  if (disconnected59 > 2) {
                                                      cycleBT(true);
                                                  } else {
                                                      if (scanConstantly())
                                                          startScan();
                                                      else
                                                          cycleScan(0);
                                                      max133RetryCounter = 0;
                                                  }
                                              }

                                              break;
                                          default:
                                              Log.e("gattCallback", "STATE_OTHER");
                                      }
                                  }
                              }
                );
            } else {
                android.util.Log.i(TAG, "onConnectionStateChange On Main Thread? " + isOnMainThread());
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        Log.e(TAG, "STATE_CONNECTED");
                        isConnected = true;

                        if (enforceMainThread()){
                            Handler iHandler = new Handler(Looper.getMainLooper());
                            iHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    android.util.Log.i(TAG, "discoverServices On Main Thread? " + isOnMainThread());
                                    if (mGatt != null)
                                        mGatt.discoverServices();
                                }
                            });
                        } else {
                            android.util.Log.i(TAG, "discoverServices On Main Thread? " + isOnMainThread());
                            if (mGatt != null)
                                mGatt.discoverServices();
                        }


                        stopScan();
                        scan_interval_timer.cancel();
                        keepAlive();
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        isConnected = false;
                        if (isScanning) {
                            stopScan();
                        }
                        Log.e(TAG, "STATE_DISCONNECTED: " + status);
                        if (mGatt != null)
                            mGatt.close();
                        mGatt = null;
                        if (status == 0 && !encountered133) {// || status == 59) {
                            android.util.Log.i(TAG, "clean disconnect");
                            max133RetryCounter = 0;
                            if (scanConstantly())
                                cycleScan(15000);
                        } else if (status == 133 || max133RetryCounter >= max133Retries) {
                            Log.e(TAG, "max133RetryCounter? " + max133RetryCounter);
                            Log.e(TAG, "Encountered 133: " + encountered133);
                            max133RetryCounter = 0;
                            disconnected133++;
                            cycleBT(true);
                        } else if (encountered133) {
                            Log.e(TAG, "max133RetryCounter? " + max133RetryCounter);
                            Log.e(TAG, "Encountered 133: " + encountered133);
                            if (scanConstantly())
                                startScan();
                            else
                                cycleScan(0);
                            max133RetryCounter++;
                        } else if (status == 129) {
                            forgetDevice();
                        } else {
                            if (status == 59) {
                                disconnected59++;
                            }
                            if (disconnected59 > 2) {
                                cycleBT(true);
                            } else {
                                if (scanConstantly())
                                    startScan();
                                else
                                    cycleScan(0);
                                max133RetryCounter = 0;
                            }
                        }

                        break;
                    default:
                        Log.e("gattCallback", "STATE_OTHER");
                }
            }


        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, final int status) {
            if (enforceMainThread()) {
                Handler iHandler = new Handler(Looper.getMainLooper());
                iHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        android.util.Log.i(TAG, "onServicesDiscovered On Main Thread? " + isOnMainThread());
                        Log.e(TAG, "onServicesDiscovered: " + status);
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            if (mGatt != null) {
                                try {
                                    cgmService = mGatt.getService(BluetoothServices.CGMService);
                                    if (cgmService != null) {
                                        authCharacteristic = cgmService.getCharacteristic(BluetoothServices.Authentication);
                                        controlCharacteristic = cgmService.getCharacteristic(BluetoothServices.Control);
                                        commCharacteristic = cgmService.getCharacteristic(BluetoothServices.Communication);
                                    }
                                } catch (NullPointerException e) {
                                    Log.e(TAG, "Got null point exception onService Discovered");
                                }
                                mBluetoothAdapter.cancelDiscovery();
                            }

                            //TODO : ADD option in settings!
                            if (alwaysAuthenticate() || alwaysUnbond()) {
                                fullAuthenticate();
                            } else {
                                authenticate();
                            }

                        } else {
                            Log.w(TAG, "onServicesDiscovered received: " + status);
                        }

                        if (status == 133) {
                            encountered133 = true;
                        }
                    }
                });
            } else {
                android.util.Log.i(TAG, "onServicesDiscovered On Main Thread? " + isOnMainThread());
                Log.e(TAG, "onServicesDiscovered: " + status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (mGatt != null) {
                        try {
                            cgmService = mGatt.getService(BluetoothServices.CGMService);
                            if (cgmService != null) {
                                authCharacteristic = cgmService.getCharacteristic(BluetoothServices.Authentication);
                                controlCharacteristic = cgmService.getCharacteristic(BluetoothServices.Control);
                                commCharacteristic = cgmService.getCharacteristic(BluetoothServices.Communication);
                            }
                        } catch (NullPointerException e) {
                            Log.e(TAG, "Got Null pointer in OnServices discovered 2");
                        }
                        mBluetoothAdapter.cancelDiscovery();
                    }

                    //TODO : ADD option in settings!
                    if (alwaysAuthenticate() || alwaysUnbond()) {
                        fullAuthenticate();
                    } else {
                        authenticate();
                    }

                } else {
                    Log.w(TAG, "onServicesDiscovered received: " + status);
                }

                if (status == 133) {
                    encountered133 = true;
                }
            }


        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            if (enforceMainThread()) {
                Handler iHandler = new Handler(Looper.getMainLooper());
                iHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        android.util.Log.i(TAG, "onDescriptorWrite On Main Thread? " + isOnMainThread());
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            mGatt.writeCharacteristic(descriptor.getCharacteristic());
                            Log.e(TAG, "Writing descriptor: " + status);
                        } else {
                            Log.e(TAG, "Unknown error writing descriptor");
                        }

                        if (status == 133) {
                            encountered133 = true;
                        }
                    }
                });
            } else {
                android.util.Log.i(TAG, "onDescriptorWrite On Main Thread? " + isOnMainThread());
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mGatt.writeCharacteristic(descriptor.getCharacteristic());
                    Log.e(TAG, "Writing descriptor: " + status);
                } else {
                    Log.e(TAG, "Unknown error writing descriptor");
                }

                if (status == 133) {
                    encountered133 = true;
                }
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            if (enforceMainThread()) {
                Handler iHandler = new Handler(Looper.getMainLooper());
                iHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "Success Write " + String.valueOf(status));
                        //Log.e(TAG, "Characteristic " + String.valueOf(characteristic.getUuid()));
                        android.util.Log.i(TAG, "onCharacteristicWrite On Main Thread? " + isOnMainThread());

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            if (String.valueOf(characteristic.getUuid()).equalsIgnoreCase(String.valueOf(authCharacteristic.getUuid()))) {
                                android.util.Log.i(TAG, "Char Value: " + Arrays.toString(characteristic.getValue()));
                                android.util.Log.i(TAG, "auth? " + String.valueOf(characteristic.getUuid()));
                                if (characteristic.getValue() != null && characteristic.getValue()[0] != 0x6) {
                                    mGatt.readCharacteristic(characteristic);
                                }
                            } else {
                                android.util.Log.i(TAG, "control? " + String.valueOf(characteristic.getUuid()));
                                android.util.Log.i(TAG, "status? " + status);
                            }
                        }

                        if (status == 133) {
                            encountered133 = true;
                        }
                    }
                });

            } else {
                Log.e(TAG, "Success Write " + String.valueOf(status));
                //Log.e(TAG, "Characteristic " + String.valueOf(characteristic.getUuid()));
                android.util.Log.i(TAG, "onCharacteristicWrite On Main Thread? " + isOnMainThread());

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (String.valueOf(characteristic.getUuid()).equalsIgnoreCase(String.valueOf(authCharacteristic.getUuid()))) {
                        android.util.Log.i(TAG, "Char Value: " + Arrays.toString(characteristic.getValue()));
                        android.util.Log.i(TAG, "auth? " + String.valueOf(characteristic.getUuid()));
                        if (characteristic.getValue() != null && characteristic.getValue()[0] != 0x6) {
                            mGatt.readCharacteristic(characteristic);
                        }
                    } else {
                        android.util.Log.i(TAG, "control? " + String.valueOf(characteristic.getUuid()));
                        android.util.Log.i(TAG, "status? " + status);
                    }
                }

                if (status == 133) {
                    encountered133 = true;
                }
            }


        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            if (enforceMainThread()) {
                Handler iHandler = new Handler(Looper.getMainLooper());
                iHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "ReadStatus: " + String.valueOf(status));
                        android.util.Log.i(TAG, "onCharacteristicRead On Main Thread? " + isOnMainThread());

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Log.e(TAG, "CharBytes-or " + Arrays.toString(characteristic.getValue()));
                            android.util.Log.i(TAG, "CharHex-or " + Extensions.bytesToHex(characteristic.getValue()));

                            byte[] buffer = characteristic.getValue();
                            byte code = buffer[0];
                            Transmitter defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
                            mBluetoothAdapter = mBluetoothManager.getAdapter();

                            switch (code) {
                                case 5:
                                    authStatus = new AuthStatusRxMessage(characteristic.getValue());
                                    if (authStatus.authenticated == 1 && authStatus.bonded == 1 && isBondedOrBonding == true) {
                                        isBondedOrBonding = true;
                                        getSensorData();
                                    } else if (authStatus.authenticated == 1 && authStatus.bonded == 2) {
                                        android.util.Log.i(TAG, "Let's Bond!");
                                        BondRequestTxMessage bondRequest = new BondRequestTxMessage();
                                        characteristic.setValue(bondRequest.byteSequence);
                                        mGatt.writeCharacteristic(characteristic);
                                        isBondedOrBonding = true;
                                        device.createBond();
                                    } else {
                                        android.util.Log.i(TAG, "Transmitter NOT already authenticated");
                                        authRequest = new AuthRequestTxMessage();
                                        characteristic.setValue(authRequest.byteSequence);
                                        android.util.Log.i(TAG, authRequest.byteSequence.toString());
                                        mGatt.writeCharacteristic(characteristic);
                                    }
                                    break;

                                case 3:
                                    AuthChallengeRxMessage authChallenge = new AuthChallengeRxMessage(characteristic.getValue());
                                    if (authRequest == null) {
                                        authRequest = new AuthRequestTxMessage();
                                    }
                                    android.util.Log.i(TAG, "tokenHash " + Arrays.toString(authChallenge.tokenHash));
                                    android.util.Log.i(TAG, "singleUSe " + Arrays.toString(calculateHash(authRequest.singleUseToken)));

                                    byte[] challengeHash = calculateHash(authChallenge.challenge);
                                    android.util.Log.d(TAG, "challenge hash" + Arrays.toString(challengeHash));
                                    if (challengeHash != null) {
                                        android.util.Log.d(TAG, "Transmitter try auth challenge");
                                        AuthChallengeTxMessage authChallengeTx = new AuthChallengeTxMessage(challengeHash);
                                        android.util.Log.i(TAG, "Auth Challenge: " + Arrays.toString(authChallengeTx.byteSequence));
                                        characteristic.setValue(authChallengeTx.byteSequence);
                                        mGatt.writeCharacteristic(characteristic);
                                    }
                                    break;

                                default:
                                    android.util.Log.i(TAG, code + " - Transmitter NOT already authenticated");
                                    authRequest = new AuthRequestTxMessage();
                                    characteristic.setValue(authRequest.byteSequence);
                                    android.util.Log.i(TAG, authRequest.byteSequence.toString());
                                    mGatt.writeCharacteristic(characteristic);
                                    break;
                            }

                        }

                        if (status == 133) {
                            encountered133 = true;
                        }
                    }
                });
            } else {
                Log.e(TAG, "ReadStatus: " + String.valueOf(status));
                android.util.Log.i(TAG, "onCharacteristicRead On Main Thread? " + isOnMainThread());

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "CharBytes-or " + Arrays.toString(characteristic.getValue()));
                    android.util.Log.i(TAG, "CharHex-or " + Extensions.bytesToHex(characteristic.getValue()));

                    byte[] buffer = characteristic.getValue();
                    byte code = buffer[0];
                    Transmitter defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
                    mBluetoothAdapter = mBluetoothManager.getAdapter();

                    switch (code) {
                        case 5:
                            authStatus = new AuthStatusRxMessage(characteristic.getValue());
                            if (authStatus.authenticated == 1 && authStatus.bonded == 1 && isBondedOrBonding == true) {
                                isBondedOrBonding = true;
                                getSensorData();
                            } else if (authStatus.authenticated == 1 && authStatus.bonded == 2) {
                                android.util.Log.i(TAG, "Let's Bond!");
                                BondRequestTxMessage bondRequest = new BondRequestTxMessage();
                                characteristic.setValue(bondRequest.byteSequence);
                                mGatt.writeCharacteristic(characteristic);
                                isBondedOrBonding = true;
                                device.createBond();
                            } else {
                                android.util.Log.i(TAG, "Transmitter NOT already authenticated");
                                authRequest = new AuthRequestTxMessage();
                                characteristic.setValue(authRequest.byteSequence);
                                android.util.Log.i(TAG, authRequest.byteSequence.toString());
                                mGatt.writeCharacteristic(characteristic);
                            }
                            break;

                        case 3:
                            AuthChallengeRxMessage authChallenge = new AuthChallengeRxMessage(characteristic.getValue());
                            if (authRequest == null) {
                                authRequest = new AuthRequestTxMessage();
                            }
                            android.util.Log.i(TAG, "tokenHash " + Arrays.toString(authChallenge.tokenHash));
                            android.util.Log.i(TAG, "singleUSe " + Arrays.toString(calculateHash(authRequest.singleUseToken)));

                            byte[] challengeHash = calculateHash(authChallenge.challenge);
                            android.util.Log.d(TAG, "challenge hash" + Arrays.toString(challengeHash));
                            if (challengeHash != null) {
                                android.util.Log.d(TAG, "Transmitter try auth challenge");
                                AuthChallengeTxMessage authChallengeTx = new AuthChallengeTxMessage(challengeHash);
                                android.util.Log.i(TAG, "Auth Challenge: " + Arrays.toString(authChallengeTx.byteSequence));
                                characteristic.setValue(authChallengeTx.byteSequence);
                                mGatt.writeCharacteristic(characteristic);
                            }
                            break;

                        default:
                            android.util.Log.i(TAG, code + " - Transmitter NOT already authenticated");
                            authRequest = new AuthRequestTxMessage();
                            characteristic.setValue(authRequest.byteSequence);
                            android.util.Log.i(TAG, authRequest.byteSequence.toString());
                            mGatt.writeCharacteristic(characteristic);
                            break;
                    }

                }

                if (status == 133) {
                    encountered133 = true;
                }
            }


        }

        @Override
        // Characteristic notification
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            if (enforceMainThread()) {
                Handler iHandler = new Handler(Looper.getMainLooper());
                iHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "CharBytes-nfy" + Arrays.toString(characteristic.getValue()));
                        android.util.Log.i(TAG, "CharHex-nfy" + Extensions.bytesToHex(characteristic.getValue()));

                        android.util.Log.i(TAG, "onCharacteristicChanged On Main Thread? " + isOnMainThread());

                        byte[] buffer = characteristic.getValue();
                        byte firstByte = buffer[0];
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && gatt != null) {
                            mGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                        }
                        if (firstByte == 0x2f) {
                            SensorRxMessage sensorRx = new SensorRxMessage(characteristic.getValue());

                            ByteBuffer sensorData = ByteBuffer.allocate(buffer.length);
                            sensorData.order(ByteOrder.LITTLE_ENDIAN);
                            sensorData.put(buffer, 0, buffer.length);

                            int sensor_battery_level = 0;
                            if (sensorRx.status == TransmitterStatus.BRICKED) {
                                //TODO Handle this in UI/Notification
                                sensor_battery_level = 206; //will give message "EMPTY"
                            } else if (sensorRx.status == TransmitterStatus.LOW) {
                                sensor_battery_level = 209; //will give message "LOW"
                            } else {
                                sensor_battery_level = 216; //no message, just system status "OK"
                            }

                            //Log.e(TAG, "filtered: " + sensorRx.filtered);
                            Log.e(TAG, "unfiltered: " + sensorRx.unfiltered);
                            disconnected133=0;
                            disconnected59=0;
                            doDisconnectMessage(gatt, characteristic);
                            processNewTransmitterData(sensorRx.unfiltered, sensorRx.filtered, sensor_battery_level, new Date().getTime());
                        }
                    }
                });
            } else {
                Log.e(TAG, "CharBytes-nfy" + Arrays.toString(characteristic.getValue()));
                android.util.Log.i(TAG, "CharHex-nfy" + Extensions.bytesToHex(characteristic.getValue()));

                android.util.Log.i(TAG, "onCharacteristicChanged On Main Thread? " + isOnMainThread());

                byte[] buffer = characteristic.getValue();
                byte firstByte = buffer[0];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && gatt != null) {
                    mGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                }
                if (firstByte == 0x2f) {
                    SensorRxMessage sensorRx = new SensorRxMessage(characteristic.getValue());

                    ByteBuffer sensorData = ByteBuffer.allocate(buffer.length);
                    sensorData.order(ByteOrder.LITTLE_ENDIAN);
                    sensorData.put(buffer, 0, buffer.length);

                    int sensor_battery_level = 0;
                    if (sensorRx.status == TransmitterStatus.BRICKED) {
                        //TODO Handle this in UI/Notification
                        sensor_battery_level = 206; //will give message "EMPTY"
                    } else if (sensorRx.status == TransmitterStatus.LOW) {
                        sensor_battery_level = 209; //will give message "LOW"
                    } else {
                        sensor_battery_level = 216; //no message, just system status "OK"
                    }

                    //Log.e(TAG, "filtered: " + sensorRx.filtered);
                    disconnected133=0;
                    disconnected59=0;
                    Log.e(TAG, "unfiltered: " + sensorRx.unfiltered);
                    doDisconnectMessage(gatt, characteristic);
                    processNewTransmitterData(sensorRx.unfiltered, sensorRx.filtered, sensor_battery_level, new Date().getTime());
                }
            }


        }
    };

    private synchronized void processNewTransmitterData(int raw_data , int filtered_data,int sensor_battery_level, long captureTime) {

        TransmitterData transmitterData = TransmitterData.create(raw_data, filtered_data, sensor_battery_level, captureTime);//KS
        if (transmitterData == null) {
            Log.e(TAG, "TransmitterData.create failed: Duplicate packet");
            return;
        } else {
            timeInMillisecondsOfLastSuccessfulSensorRead = captureTime;
        }
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.e(TAG, "setSerialDataToTransmitterRawData: No Active Sensor, Data only stored in Transmitter Data");
            return;
        }

        //TODO : LOG if unfiltered or filtered values are zero

        Sensor.updateBatteryLevel(sensor, transmitterData.sensor_battery_level);
        android.util.Log.i("timestamp create", Long.toString(transmitterData.timestamp));

        BgReading.create(transmitterData.raw_data, filtered_data, this, transmitterData.timestamp);

        Log.d(TAG + " Dex raw_data ", Double.toString(transmitterData.raw_data));//KS
        Log.d(TAG + " Dex filtered_data ", Double.toString(transmitterData.filtered_data));//KS
        Log.d(TAG + " Dex sensor_battery_level ", Double.toString(transmitterData.sensor_battery_level));//KS
        java.text.DateFormat df = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
        Date date = new Date();
        date.setTime(transmitterData.timestamp);
        Log.d(TAG, " Dex timestamp " + Double.toString(transmitterData.timestamp) + " " + df.format(date));//KS

    }

    @SuppressLint("GetInstance")
    private byte[] calculateHash(byte[] data) {
        if (data.length != 8) {
            android.util.Log.e("Decrypt", "Data length should be exactly 8.");
            return null;
        }

        byte[] key = cryptKey();
        if (key == null)
            return null;

        byte[] doubleData;
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.put(data);
        bb.put(data);

        doubleData = bb.array();

        Cipher aesCipher;
        try {
            aesCipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] aesBytes = aesCipher.doFinal(doubleData, 0, doubleData.length);

            bb = ByteBuffer.allocate(8);
            bb.put(aesBytes, 0, 8);

            return bb.array();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    private byte[] cryptKey() {
        try {
            return ("00" + defaultTransmitter.transmitterId + "00" + defaultTransmitter.transmitterId).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isOnMainThread()
    {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    private long getNextAdvertiseTime() {
        long millisecondsSinceTx = getMillisecondsSinceTxLastSeen();
        long timeToExpected  = (300*1000 - (millisecondsSinceTx%(300*1000)));
        long expectedTxTime = new Date().getTime() + timeToExpected - 3*1000;
        Log.e(TAG, "millisecondsSinceTxAd: " + millisecondsSinceTx );
        //Log.e(TAG, "timeToExpected: " + timeToExpected );
        //Log.e(TAG, "expectedTxTime: " + expectedTxTime );

        return expectedTxTime;
    }

    private long getMillisecondsSinceTxLastSeen() {
        return new Date().getTime() - advertiseTimeMS.get(0);
    }

    private long getMillisecondsSinceLastSuccesfulSensorRead() {
        return new Date().getTime() - timeInMillisecondsOfLastSuccessfulSensorRead;
    }

    public boolean scanConstantly() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return sharedPreferences.getBoolean("run_ble_scan_constantly", false);
    }

    public boolean alwaysUnbond() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return sharedPreferences.getBoolean("always_unbond_G5", false);
    }

    public boolean alwaysAuthenticate() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return sharedPreferences.getBoolean("always_get_new_keys", false);
    }

    public boolean enforceMainThread() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return sharedPreferences.getBoolean("run_G5_ble_tasks_on_uithread", false);
    }

}