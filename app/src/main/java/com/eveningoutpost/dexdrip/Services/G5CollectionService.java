package com.eveningoutpost.dexdrip.Services;

/**
 * Created by jcostik1 on 3/15/16.
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.G5Model.AuthChallengeRxMessage;
import com.eveningoutpost.dexdrip.G5Model.AuthChallengeTxMessage;

import com.eveningoutpost.dexdrip.G5Model.AuthRequestTxMessage;
import com.eveningoutpost.dexdrip.G5Model.AuthStatusRxMessage;
import com.eveningoutpost.dexdrip.G5Model.BluetoothServices;
import com.eveningoutpost.dexdrip.G5Model.BondRequestTxMessage;
import com.eveningoutpost.dexdrip.G5Model.Extensions;
import com.eveningoutpost.dexdrip.G5Model.KeepAliveTxMessage;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.G5Model.Transmitter;

import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.utils.BgToSpeech;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class G5CollectionService extends Service{

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

    private ScanSettings settings;
    private List<ScanFilter> filters;

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
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
//        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DexShareCollectionStart");
//        wakeLock.acquire(40000);
        Log.d(TAG, "onG5StartCommand");

        defaultTransmitter = new Transmitter("4023Q2");

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

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void setupBluetooth() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //First time using the app or bluetooth was turned off?
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        }
        else {
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
            }
            else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    public void startScan() {
        if (Build.VERSION.SDK_INT < 21) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
        else {
            Log.d(TAG, "startScan");
            mLEScanner.startScan(filters, settings, mScanCallback);
        }
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
                    //They match, connect to the device.
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
                                    //They match, connect to the device.
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
            BluetoothGattService cgmService = gatt.getService(UUID.fromString(BluetoothServices.CGMService));
            android.util.Log.i("onServiceDiscovered", cgmService.getUuid().toString());

            if (authStatus != null && authStatus.authenticated == 1) {
                BluetoothGattCharacteristic controlCharacteristic = cgmService.getCharacteristic(UUID.fromString(BluetoothServices.Control));
                if (!mGatt.readCharacteristic(controlCharacteristic)) {
                    android.util.Log.e("onCharacteristicRead", "ReadCharacteristicError");
                }
            }
            else {
                BluetoothGattCharacteristic authCharacteristic = cgmService.getCharacteristic(UUID.fromString(BluetoothServices.Authentication));
                if (!mGatt.readCharacteristic(authCharacteristic)) {
                    android.util.Log.e("onCharacteristicRead", "ReadCharacteristicError");
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (authStatus != null) {
                    if (authStatus.authenticated != 1) {
                        BluetoothGattCharacteristic authCharacteristic = mGatt.getService(UUID.fromString(BluetoothServices.CGMService)).getCharacteristic(UUID.fromString(BluetoothServices.Authentication));

                        AuthChallengeRxMessage authChallenge = new AuthChallengeRxMessage(authCharacteristic.getValue());
                        Log.i("AuthChallenge", Arrays.toString(authChallenge.challenge));
                        Log.i("AuthChallenge", Arrays.toString(authChallenge.tokenHash));
                    }
                    else if (authStatus.bonded == 5) {
                        Log.i("CharBytes", Arrays.toString(characteristic.getValue()));
                        Log.i("CharHex", Extensions.bytesToHex(characteristic.getValue()));


                    }
                }
            }

            mGatt.setCharacteristicNotification(characteristic, false);
        }

//        authTxMessage.data: <0167422e c566804b f802>
//        AuthChallengeRxMessage Token: <d6d91514 d8271345>
//        AuthChallengeRxMessage challenge: <2bde4389 f215584c>
//        challengeHash: <65a53e82 ab481f57>
//        AuthChallengeTxMessage: <0465a53e 82ab481f 57>

//        authTxMessage.data: <01af6ffb 8ba89047 0102>
//        AuthChallengeRxMessage Token: <066d66d6 2f3500ee>
//        AuthChallengeRxMessage challenge: <acfb39af d5b08ab1>
//        challengeHash: <4198dc37 e5e1ec6a>
//        AuthChallengeTxMessage: <044198dc 37e5e1ec 6a>


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                android.util.Log.i("CharBytes", Arrays.toString(characteristic.getValue()));
                android.util.Log.i("CharHex", Extensions.bytesToHex(characteristic.getValue()));

                // Request authentication.
                authStatus = new AuthStatusRxMessage(characteristic.getValue());
                if (authStatus.authenticated == 1 && authStatus.bonded == 1) {
                    android.util.Log.i("Auth", "Transmitter already authenticated");
                }
                else {
                    if (authRequest == null) {
                        mGatt.setCharacteristicNotification(characteristic, true);

                        authRequest = new AuthRequestTxMessage();

                        characteristic.setValue(authRequest.byteSequence);
                        mGatt.writeCharacteristic(characteristic);

                        //Extensions.doSleep(200);

                        mGatt.readCharacteristic(characteristic);

                        return;
                    }

                    // Auth challenge and token have been retrieved.
                    if (characteristic.getValue()[0] == 0x3) {
                        AuthChallengeRxMessage authChallenge = new AuthChallengeRxMessage(characteristic.getValue());

                        if (!Arrays.equals(authChallenge.tokenHash, calculateHash(authRequest.singleUseToken))) {
                            android.util.Log.e("Auth", "Transmitter failed auth challenge");
                            return;
                        }

                        byte[] challengeHash = calculateHash(authChallenge.challenge);
                        if (challengeHash != null) {
                            AuthChallengeTxMessage authChallengeTx = new AuthChallengeTxMessage(challengeHash);

                            characteristic.setValue(authChallengeTx.byteSequence);
                            mGatt.writeCharacteristic(characteristic);

                            //Extensions.doSleep(200);

                            mGatt.readCharacteristic(characteristic);
                        }
                    }
                    // New auth after completing challenge. We should see pairing here.
                    else if (characteristic.getValue()[0] == 0x5) {
                        authStatus = new AuthStatusRxMessage(characteristic.getValue());

                        if (authStatus.authenticated != 1) {
                            android.util.Log.e("Auth", "Transmitter rejected auth challenge");
                        }

                        if (authStatus.bonded != 1) {
                            KeepAliveTxMessage keepAlive = new KeepAliveTxMessage(25);

                            characteristic.setValue(keepAlive.byteSequence);
                            mGatt.writeCharacteristic(characteristic);

                            mGatt.readCharacteristic(characteristic);

                            BondRequestTxMessage bondRequest = new BondRequestTxMessage();

                            characteristic.setValue(bondRequest.byteSequence);
                            mGatt.writeCharacteristic(characteristic);

                            //Extensions.doSleep(200);

                            mGatt.readCharacteristic(characteristic);

                            mGatt.setCharacteristicNotification(characteristic, false);
                        }
                    }
                }
            }
        }
    };

    @SuppressLint("GetInstance")
    private byte[] calculateHash(byte[] data) {
        if (data.length != 8) {
            android.util.Log.e("Decrypt", "Data length should be exactly 8.");
            return  null;
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
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    private byte[] cryptKey() {
        try {
            return ("00" + defaultTransmitter.transmitterId + "00" + defaultTransmitter.transmitterId).getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}