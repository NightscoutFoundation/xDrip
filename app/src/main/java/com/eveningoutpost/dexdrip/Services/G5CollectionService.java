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
import com.eveningoutpost.dexdrip.G5Model.TransmitterTimeTxMessage;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.G5Model.Transmitter;

import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.utils.BgToSpeech;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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

    protected static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    private final static String TAG = G5CollectionService.class.getSimpleName();
    private ForegroundServiceStarter foregroundServiceStarter;

    public Service service;
    private BgToSpeech bgToSpeech;
    private PendingIntent pendingIntent;
    private final static int REQUEST_ENABLE_BT = 1;

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
    private long lastRead = new Date().getTime() - (5 * 60 *1000);

    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();

    private AlarmManager alarm;// = (AlarmManager) getSystemService(ALARM_SERVICE);

    private ScanSettings settings;
    private List<ScanFilter> filters;
    private SharedPreferences prefs;

    private Handler handler;


    @Override
    public void onCreate() {
        super.onCreate();
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

//        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
//        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
//        wakeLock.acquire(4 * 60 * 1000);

        Log.d(TAG, "onG5StartCommand");
        Log.d(TAG, "SDK: " + Build.VERSION.SDK_INT);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
        setMissedBgTimer();

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        setupBluetooth();
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

    public void setMissedBgTimer() {
        Log.d(TAG, "Missed BG - CYCLE G5 Service");

        Calendar calendar = Calendar.getInstance();
        alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (pendingIntent != null)
            alarm.cancel(pendingIntent);
        long wakeTime = calendar.getTimeInMillis() + (4 * 1000 * 60);
        pendingIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
        } else
            alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
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
                filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUID.fromString(BluetoothServices.Advertisement))).build());
            }
            startScan();
        }
    }

    public void stopScan() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                Log.d(TAG, "stopScan");
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    public void startScan() {
        if (Build.VERSION.SDK_INT < 21) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            Log.d(TAG, "startScan");
            mLEScanner.startScan(filters, settings, mScanCallback);
        }
    }

    void scanAfterDelay() {

        Runnable task = new Runnable() {
            public void run() {
                startScan();
            }
        };
        worker.schedule(task, 12, TimeUnit.SECONDS);

    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            android.util.Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            // Check if the device has a name, the Dexcom transmitter always should. Match it with the transmitter id that was entered.
            // We get the last 2 characters to connect to the correct transmitter if there is more than 1 active or in the room.
            // If they match, connect to the device.
            if (btDevice.getName() != null) {
                String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(btDevice.getName());

                if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                    device = btDevice;
                    connectToDevice(btDevice);
                } else {
                    startScan();
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                android.util.Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            android.util.Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private void runOnUiThread(Runnable r) {
        handler.post(r);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // Check if the device has a name, the Dexcom transmitter always should. Match it with the transmitter id that was entered.
                            // We get the last 2 characters to connect to the correct transmitter if there is more than 1 active or in the room.
                            // If they match, connect to the device.
                            if (device.getName() != null) {
                                String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                                String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());

                                if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                                    connectToDevice(device);
                                }
                            }
                        }
                    });
                }
            };

    private void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(getApplicationContext(), false, gattCallback);
            stopScan();
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    android.util.Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    android.util.Log.e("gattCallback", "STATE_DISCONNECTED");
                    if (mGatt == null) {
                        scanAfterDelay();
                        return;
                    }
                    mGatt.close();
                    mGatt = null;
                    scanAfterDelay();
                    break;
                default:
                    android.util.Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            cgmService = gatt.getService(UUID.fromString(BluetoothServices.CGMService));
            authCharacteristic = cgmService.getCharacteristic(UUID.fromString(BluetoothServices.Authentication));
            controlCharacteristic = cgmService.getCharacteristic(BluetoothServices.Control);
            commCharacteristic = cgmService.getCharacteristic(UUID.fromString(BluetoothServices.Communication));

            if (!mGatt.readCharacteristic(authCharacteristic)) {
                android.util.Log.e("onCharacteristicRead", "ReadCharacteristicError");
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.writeCharacteristic(descriptor.getCharacteristic());
            } else {
                Log.e(TAG, "Unknown error writing descriptor");
            }
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            android.util.Log.i("Success Write", String.valueOf(status));
            android.util.Log.i("Characteristic", String.valueOf(characteristic.getUuid()));

            if (String.valueOf(characteristic.getUuid()) != String.valueOf(controlCharacteristic.getUuid())) {
                gatt.readCharacteristic(characteristic);
            } else {
                android.util.Log.i("control?", String.valueOf(characteristic.getUuid()));
            }

//            if (status == BluetoothGatt.GATT_SUCCESS) {
//            }

//            mGatt.setCharacteristicNotification(characteristic, false);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                android.util.Log.i("CharBytes-or", Arrays.toString(characteristic.getValue()));
                android.util.Log.i("CharHex-or", Extensions.bytesToHex(characteristic.getValue()));

                if (characteristic.getValue()[0] == 5 || characteristic.getValue()[0] <= 0) {
                    authStatus = new AuthStatusRxMessage(characteristic.getValue());
                    if (authStatus.authenticated == 1 && authStatus.bonded == 1) {
                        mGatt.setCharacteristicNotification(controlCharacteristic, true);
                        BluetoothGattDescriptor descriptor = controlCharacteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        TransmitterTimeTxMessage timeMessage = new TransmitterTimeTxMessage();
                        android.util.Log.i("timeMessage", Arrays.toString(timeMessage.byteSequence));
                        controlCharacteristic.setValue(timeMessage.byteSequence);
                        mGatt.writeDescriptor(descriptor);
                    } else if (authStatus.authenticated == 1) {
                        android.util.Log.i("Auth", "Let's Bond!");
                        KeepAliveTxMessage keepAlive = new KeepAliveTxMessage(30);
                        characteristic.setValue(keepAlive.byteSequence);
                        mGatt.writeCharacteristic(characteristic);
                        mGatt.readCharacteristic(characteristic);
                        BondRequestTxMessage bondRequest = new BondRequestTxMessage();
                        characteristic.setValue(bondRequest.byteSequence);
                        mGatt.writeCharacteristic(characteristic);
                        device.createBond();
                    } else {
                        android.util.Log.i("Auth", "Transmitter NOT already authenticated");
                        //mGatt.setCharacteristicNotification(characteristic, true);
                        authRequest = new AuthRequestTxMessage();
                        characteristic.setValue(authRequest.byteSequence);
                        android.util.Log.i("AuthReq", authRequest.byteSequence.toString());
                        mGatt.writeCharacteristic(characteristic);
                    }
                }

                if (characteristic.getValue()[0] == 8) {
                    android.util.Log.i("Auth", "Transmitter NOT already authenticated");
                    authRequest = new AuthRequestTxMessage();
                    characteristic.setValue(authRequest.byteSequence);
                    android.util.Log.i("AuthReq", authRequest.byteSequence.toString());
                    mGatt.writeCharacteristic(characteristic);
                }

//                 Auth challenge and token have been retrieved.
                if (characteristic.getValue()[0] == 0x3) {
                    AuthChallengeRxMessage authChallenge = new AuthChallengeRxMessage(characteristic.getValue());
                    if (authRequest == null) {
                        android.util.Log.d("new auth", "hmmmm");
                        authRequest = new AuthRequestTxMessage();
                    }
                    android.util.Log.i("tokenHash", Arrays.toString(authChallenge.tokenHash));
                    android.util.Log.i("singleUSe", Arrays.toString(calculateHash(authRequest.singleUseToken)));

                    byte[] challengeHash = calculateHash(authChallenge.challenge);
                    android.util.Log.d("challenge hash", Arrays.toString(challengeHash));
                    if (challengeHash != null) {
                        android.util.Log.d("Auth", "Transmitter try auth challenge");
                        AuthChallengeTxMessage authChallengeTx = new AuthChallengeTxMessage(challengeHash);
                        android.util.Log.i("AuthChallenge", Arrays.toString(authChallengeTx.byteSequence));
                        characteristic.setValue(authChallengeTx.byteSequence);
                        mGatt.writeCharacteristic(characteristic);
                    }
                }
            }

        }

        @Override
        // Characteristic notification
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            android.util.Log.i("CharBytes-nfy", Arrays.toString(characteristic.getValue()));
            android.util.Log.i("CharHex-nfy", Extensions.bytesToHex(characteristic.getValue()));

            byte[] buffer = characteristic.getValue();
            byte firstByte = buffer[0];

            if (firstByte == 0x2f) {
                SensorRxMessage sensorRx = new SensorRxMessage(characteristic.getValue());
                if (pendingIntent != null)
                    alarm.cancel(pendingIntent);
                long timeSince = new Date().getTime() - lastRead;
                android.util.Log.i("ms since", Long.toString(timeSince));
                if (timeSince > 3 * 60 * 1000) {
                    TransmitterData txData = new TransmitterData();
                    ByteBuffer sensorData = ByteBuffer.allocate(buffer.length);
                    sensorData.order(ByteOrder.LITTLE_ENDIAN);
                    sensorData.put(buffer, 0, buffer.length);
                    txData.raw_data = sensorRx.unfiltered;
                    txData.filtered_data = sensorRx.filtered;

                    if (sensorRx.status == TransmitterStatus.BRICKED) {
                        //TODO Handle this in UI/Notification
                    } else if (sensorRx.status == TransmitterStatus.LOW) {
                        txData.sensor_battery_level = 206;
                    } else {
                        txData.sensor_battery_level = 216;
                    }

                    txData.uuid = UUID.randomUUID().toString();
                    //txData.timestamp = new Date().getTime();
                    lastRead = startTimeInterval + sensorRx.timestamp;
                    txData.timestamp = lastRead;
                    android.util.Log.i("timestamp", Long.toString(txData.timestamp));

                    processNewTransmitterData(txData, txData.timestamp);
                }
                setMissedBgTimer();
                doDisconnectMessage(gatt, characteristic);
            }
            // Transmitter Time
            else if (firstByte == 0x25) {
                TransmitterTimeRxMessage transmitterTime = new TransmitterTimeRxMessage(characteristic.getValue());

                startTimeInterval = new Date().getTime() - transmitterTime.currentTime;

                SensorTxMessage sensorTx = new SensorTxMessage();
                characteristic.setValue(sensorTx.byteSequence);
                gatt.writeCharacteristic(characteristic);

            } else {
                doDisconnectMessage(gatt, characteristic);
            }

        }
    };


    private void processNewTransmitterData(TransmitterData transmitterData, long timestamp) {
        if (transmitterData == null) {
            return;
        }

        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.i(TAG, "setSerialDataToTransmitterRawData: No Active Sensor, Data only stored in Transmitter Data");
            return;
        }

        Sensor.updateBatteryLevel(sensor, transmitterData.sensor_battery_level);
        android.util.Log.i("timestamp create", Long.toString(transmitterData.timestamp));

        BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, this, transmitterData.timestamp);
    }

    // Sends the disconnect tx message to our bt device.
    private void doDisconnectMessage(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        gatt.setCharacteristicNotification(controlCharacteristic, false);

        DisconnectTxMessage disconnectTx = new DisconnectTxMessage();
        characteristic.setValue(disconnectTx.byteSequence);
        gatt.writeCharacteristic(characteristic);
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

}