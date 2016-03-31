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
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.G5Model.AuthChallengeRxMessage;
import com.eveningoutpost.dexdrip.G5Model.AuthChallengeTxMessage;

import com.eveningoutpost.dexdrip.G5Model.AuthRequestTxMessage;
import com.eveningoutpost.dexdrip.G5Model.AuthStatusRxMessage;
import com.eveningoutpost.dexdrip.G5Model.BluetoothServices;
import com.eveningoutpost.dexdrip.G5Model.BondRequestTxMessage;
import com.eveningoutpost.dexdrip.G5Model.DisconnectTxMessage;
import com.eveningoutpost.dexdrip.G5Model.Extensions;
import com.eveningoutpost.dexdrip.G5Model.KeepAliveTxMessage;
import com.eveningoutpost.dexdrip.G5Model.SensorRxMessage;
import com.eveningoutpost.dexdrip.G5Model.SensorTxMessage;
import com.eveningoutpost.dexdrip.G5Model.TransmitterStatus;
import com.eveningoutpost.dexdrip.G5Model.TransmitterTimeRxMessage;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.G5Model.Transmitter;

import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.utils.BgToSpeech;
import com.squareup.okhttp.OkHttpClient;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class G5CollectionService extends Service {

    private final static String TAG = G5CollectionService.class.getSimpleName();
    private ForegroundServiceStarter foregroundServiceStarter;

    public Service service;
    private BgToSpeech bgToSpeech;
    private PendingIntent pendingIntent;

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
    private long startTimeInterval = -1;
    private int lastBattery = 216;
    private Boolean isBondedOrBonding = false;
    private Boolean isFirstTry = true;

    private AlarmManager alarm;// = (AlarmManager) getSystemService(ALARM_SERVICE);

    private ScanSettings settings;
    private List<ScanFilter> filters;
    private SharedPreferences prefs;

    private boolean isScanning = false;

    private Handler handler;

    StringBuilder log = new StringBuilder();


    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initScanCallback();
        }

//        readData = new ReadDataShare(this);
        service = this;
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), service);
        foregroundServiceStarter.start();
//        final IntentFilter bondintent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
//        registerReceiver(mPairReceiver, bondintent);
//        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        listenForChangeInSettings();
        bgToSpeech = BgToSpeech.setupTTS(getApplicationContext()); //keep reference to not being garbage collected
        handler = new Handler(getApplicationContext().getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onG5StartCommand");
        Log.d(TAG, "SDK: " + Build.VERSION.SDK_INT);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
        keepAlive();

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        isBondedOrBonding = false;
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName() != null) {

                    String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                    String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());

                    if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                        isBondedOrBonding = true;
                    }

                }
            }
        }

        if (mGatt != null) {
            mGatt.close();
            mGatt = null;
        }

        Log.d(TAG, "Bonded? " + isBondedOrBonding.toString());
        if (Sensor.isActive()){
            setupBluetooth();
            Log.d(TAG, "Active Sensor");

        } else {
            stopScan();
            Log.d(TAG, "No Active Sensor");
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        close();
//        setRetryTimer();
//        foregroundServiceStarter.stop();
//        unregisterReceiver(mPairReceiver);
//        BgToSpeech.tearDownTTS();
        Log.i(TAG, "SERVICE STOPPED");
    }

    public void keepAlive() {
        Log.d(TAG, "Wake Lock & Wake Time");

        isFirstTry = true;

        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire(20 * 1000);
        alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (pendingIntent != null)
            alarm.cancel(pendingIntent);
        long wakeTime = (long) (SystemClock.elapsedRealtime() + (4.5 * 1000 * 60));
        pendingIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, wakeTime, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarm.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, wakeTime, pendingIntent);
        } else
            alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, wakeTime, pendingIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void setupBluetooth() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //First time using the app or bluetooth was turned off?
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<>();
                //Only look for CGM.
                filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(BluetoothServices.Advertisement)).build());
            }
            if (isScanning){
                stopScan();
                Log.d(TAG, "Refresh Scanning");
            }
            startScan();
        }
    }

    public void stopScan() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                Log.d(TAG, "stopScan");
                try {
                    mLEScanner.stopScan(mScanCallback);
                } catch (NullPointerException e) {
                    //Known bug in Samsung API 21 stack
                    System.out.print("Caught the NullPointerException");
                }
            }
        }

        isScanning = false;
    }

    public void startScan() {
        if (isScanning) {
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setupLeScanCallback();

            mBluetoothAdapter.startLeScan(new UUID[]{ BluetoothServices.Advertisement }, mLeScanCallback);
        } else {
            Log.d(TAG, "startScan");

            mLEScanner.startScan(filters, settings, mScanCallback);
        }

        isScanning = true;
    }
    
    void scanAfterDelay(int delay) {
        Log.d(TAG, "ScanDelay");
        handler.postDelayed(new Runnable() {
            public void run() {
                startScan();
            }
        }, delay);
    }

    // API 18 - 20
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
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

    @TargetApi(21)
    private void initScanCallback(){
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                android.util.Log.i(TAG, "result: " + result.toString());
                BluetoothDevice btDevice = result.getDevice();
                // Check if the device has a name, the Dexcom transmitter always should. Match it with the transmitter id that was entered.
                // We get the last 2 characters to connect to the correct transmitter if there is more than 1 active or in the room.
                // If they match, connect to the device.
                if (btDevice.getName() != null) {
                    String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                    String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(btDevice.getName());

                    if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                        if (isFirstTry) {
                            Log.d(TAG, "ReadDelay");
                            isFirstTry = false;
                            stopScan();
                            scanAfterDelay(50);
                        } else {
                            device = btDevice;
                            connectToDevice(btDevice);
                        }
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                android.util.Log.e(TAG, "Scan Failed Error Code: " + errorCode);
                if (errorCode == 1) {
                    android.util.Log.e(TAG, "Already Scanning");
                    isScanning = true;
                }
            }
        };
    }

    /*private void runOnUiThread(Runnable r) {
        handler.post(r);
    }*/

    private BluetoothAdapter.LeScanCallback mLeScanCallback = null;

    private void connectToDevice(BluetoothDevice device) {
        android.util.Log.i(TAG, "Request Connect");
        if (mGatt == null) {
            android.util.Log.i(TAG, "mGatt Null, connecting...");

            stopScan();
            mGatt = device.connectGatt(getApplicationContext(), false, gattCallback);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    android.util.Log.i("gattCallback", "STATE_CONNECTED");
                    mGatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    android.util.Log.e("gattCallback", "STATE_DISCONNECTED");
                    mGatt.close();
                    mGatt = null;
                    startScan();
                    break;
                default:
                    android.util.Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                cgmService = mGatt.getService(BluetoothServices.CGMService);
                authCharacteristic = cgmService.getCharacteristic(BluetoothServices.Authentication);
                controlCharacteristic = cgmService.getCharacteristic(BluetoothServices.Control);
                commCharacteristic = cgmService.getCharacteristic(BluetoothServices.Communication);

                mGatt.setCharacteristicNotification(authCharacteristic, true);

                if (!mGatt.readCharacteristic(authCharacteristic)) {
                    android.util.Log.e(TAG, "onCharacteristicRead : ReadCharacteristicError");
                }
            }
            else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mGatt.writeCharacteristic(descriptor.getCharacteristic());
            } else {
                Log.e(TAG, "Unknown error writing descriptor");
            }
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            android.util.Log.i(TAG, "Success Write " +  String.valueOf(status));
            android.util.Log.i(TAG, "Characteristic " + String.valueOf(characteristic.getUuid()));

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (String.valueOf(characteristic.getUuid()).equalsIgnoreCase(String.valueOf(authCharacteristic.getUuid()))) {
                    android.util.Log.i(TAG, "auth? " + String.valueOf(characteristic.getUuid()));
                    if (characteristic.getValue() != null && characteristic.getValue()[0] != 0x7 && characteristic.getValue()[0] != 0x6) {
                        mGatt.readCharacteristic(characteristic);
                    }
                } else {
                    android.util.Log.i(TAG, "control?" + String.valueOf(characteristic.getUuid()));

                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("ReadStatus", String.valueOf(status));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                android.util.Log.i(TAG, "CharBytes-or " + Arrays.toString(characteristic.getValue()));
                android.util.Log.i(TAG, "CharHex-or " + Extensions.bytesToHex(characteristic.getValue()));

                byte [] buffer = characteristic.getValue();
                if (!isBondedOrBonding) {
                    buffer[0] = 8;
                }

                if (buffer[0] == 5 || buffer[0] <= 0) {
                    authStatus = new AuthStatusRxMessage(characteristic.getValue());
                    if (authStatus.authenticated == 1 && authStatus.bonded == 1) {
                        isBondedOrBonding = true;
                        mGatt.setCharacteristicNotification(authCharacteristic, false);
                        mGatt.setCharacteristicNotification(controlCharacteristic, true);
                        BluetoothGattDescriptor descriptor = controlCharacteristic.getDescriptor(BluetoothServices.CharacteristicUpdateNotification);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
//                        TransmitterTimeTxMessage timeMessage = new TransmitterTimeTxMessage();
//                        android.util.Log.i("timeMessage", Arrays.toString(timeMessage.byteSequence));
//                        controlCharacteristic.setValue(timeMessage.byteSequence);
                        SensorTxMessage sensorTx = new SensorTxMessage();
                        controlCharacteristic.setValue(sensorTx.byteSequence);
                        mGatt.writeDescriptor(descriptor);
                    } else if (authStatus.authenticated == 1) {
                        android.util.Log.i(TAG, "Let's Bond!");
                        KeepAliveTxMessage keepAlive = new KeepAliveTxMessage(25);
                        characteristic.setValue(keepAlive.byteSequence);
                        mGatt.writeCharacteristic(characteristic);
                        mGatt.readCharacteristic(characteristic);
                        BondRequestTxMessage bondRequest = new BondRequestTxMessage();
                        characteristic.setValue(bondRequest.byteSequence);
                        mGatt.writeCharacteristic(characteristic);
                        device.createBond();
                    } else {
                        android.util.Log.i(TAG, "Transmitter NOT already authenticated");
                        authRequest = new AuthRequestTxMessage();
                        characteristic.setValue(authRequest.byteSequence);
                        android.util.Log.i(TAG, authRequest.byteSequence.toString());
                        mGatt.writeCharacteristic(characteristic);
                    }
                }

                if (buffer[0] == 8) {
                    android.util.Log.i(TAG, "8 - Transmitter NOT already authenticated");
                    authRequest = new AuthRequestTxMessage();
                    characteristic.setValue(authRequest.byteSequence);
                    android.util.Log.i(TAG, authRequest.byteSequence.toString());
                    isBondedOrBonding = true;
                    mGatt.writeCharacteristic(characteristic);
                }

//                 Auth challenge and token have been retrieved.
                if (buffer[0] == 0x3) {
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
                }
            }

        }

        @Override
        // Characteristic notification
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            android.util.Log.i(TAG, "CharBytes-nfy" + Arrays.toString(characteristic.getValue()));
            android.util.Log.i(TAG, "CharHex-nfy" + Extensions.bytesToHex(characteristic.getValue()));

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

                android.util.Log.i(TAG, "filtered: " + sensorRx.filtered);
                android.util.Log.i(TAG, "unfiltered: " + sensorRx.unfiltered);

                processNewTransmitterData(sensorRx.unfiltered, sensorRx.filtered, sensor_battery_level, new Date().getTime());
                if (pendingIntent != null) {
                    alarm.cancel(pendingIntent);
                }
                keepAlive();

                doDisconnectMessage(gatt, characteristic);
            }
            // Transmitter Time
            else if (firstByte == 0x25) {
                TransmitterTimeRxMessage transmitterTime = new TransmitterTimeRxMessage(characteristic.getValue());
                startTimeInterval = new Date().getTime() - transmitterTime.currentTime;

                mGatt.setCharacteristicNotification(controlCharacteristic, true);
                BluetoothGattDescriptor descriptor = controlCharacteristic.getDescriptor(BluetoothServices.CharacteristicUpdateNotification);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

                SensorTxMessage sensorTx = new SensorTxMessage();
                controlCharacteristic.setValue(sensorTx.byteSequence);
                mGatt.writeDescriptor(descriptor);

            } else {
                doDisconnectMessage(gatt, characteristic);
            }

        }
    };


    private void processNewTransmitterData(int raw_data , int filtered_data,int sensor_battery_level, long CaptureTime) {

        TransmitterData transmitterData = TransmitterData.create(raw_data, sensor_battery_level, CaptureTime);
        if (transmitterData == null) {
            Log.i(TAG, "TransmitterData.create failed: Duplicate packet");
            return;
        }
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.i(TAG, "setSerialDataToTransmitterRawData: No Active Sensor, Data only stored in Transmitter Data");
            return;
        }

        Sensor.updateBatteryLevel(sensor, transmitterData.sensor_battery_level);
        android.util.Log.i("timestamp create", Long.toString(transmitterData.timestamp));

        BgReading.create(transmitterData.raw_data, filtered_data, this, transmitterData.timestamp);
    }

    // Sends the disconnect tx message to our bt device.
    private void doDisconnectMessage(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        mGatt.setCharacteristicNotification(controlCharacteristic, false);

        DisconnectTxMessage disconnectTx = new DisconnectTxMessage();
        characteristic.setValue(disconnectTx.byteSequence);
        mGatt.writeCharacteristic(characteristic);
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

    private void appendToStringBuilder(String toAppend) {
        log.append(toAppend + '\n');
    }

    private void uploadStringBuilder() {
        int SOCKET_TIMEOUT = 60000;
        int CONNECTION_TIMEOUT = 30000;
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        client.setWriteTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS);
        client.setReadTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS);
    }

}