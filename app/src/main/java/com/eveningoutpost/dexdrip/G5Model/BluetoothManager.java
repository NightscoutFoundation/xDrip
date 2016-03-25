//package com.eveningoutpost.dexdrip.G5Model;
//
//import android.annotation.TargetApi;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothGatt;
//import android.bluetooth.BluetoothGattCallback;
//import android.bluetooth.BluetoothGattCharacteristic;
//import android.bluetooth.BluetoothGattService;
//import android.bluetooth.BluetoothProfile;
//import android.bluetooth.le.BluetoothLeScanner;
//import android.bluetooth.le.ScanCallback;
//import android.bluetooth.le.ScanFilter;
//import android.bluetooth.le.ScanResult;
//import android.bluetooth.le.ScanSettings;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Build;
//import android.os.ParcelUuid;
//import android.util.Log;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.UUID;
//
///**
// * Created by joeginley on 3/16/16.
// */
//@TargetApi(Build.VERSION_CODES.LOLLIPOP)
//@SuppressWarnings("deprecation")
//public class BluetoothManager {
//
//
//    private final static int REQUEST_ENABLE_BT = 1;
//
//    private android.bluetooth.BluetoothManager mBluetoothManager;
//    private BluetoothAdapter mBluetoothAdapter;
//    private BluetoothLeScanner mLEScanner;
//    private BluetoothGatt mGatt;
//
//    private ScanSettings settings;
//    private List<ScanFilter> filters;
//
//    public BluetoothManager() {
//
//        mBluetoothManager = (android.bluetooth.BluetoothManager)this.getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = mBluetoothManager.getAdapter();
//
//        setupBluetooth();
//    }
//
//    public void setupBluetooth() {
//        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
//            //First time using the app or bluetooth was turned off?
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//        }
//        else {
//            if (Build.VERSION.SDK_INT >= 21) {
//                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
//                settings = new ScanSettings.Builder()
//                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                        .build();
//                filters = new ArrayList<>();
//                //Only look for CGM.
//                filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUID.fromString(BluetoothServices.Advertisement))).build());
//            }
//            startScan();
//        }
//    }
//
//    public void stopScan() {
//        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
//            if (Build.VERSION.SDK_INT < 21) {
//                mBluetoothAdapter.stopLeScan(mLeScanCallback);
//            }
//            else {
//                mLEScanner.stopScan(mScanCallback);
//            }
//        }
//    }
//
//    public void startScan() {
//        if (Build.VERSION.SDK_INT < 21) {
//            mBluetoothAdapter.startLeScan(mLeScanCallback);
//        }
//        else {
//            mLEScanner.startScan(filters, settings, mScanCallback);
//        }
//    }
//
//    private ScanCallback mScanCallback = new ScanCallback() {
//        @Override
//        public void onScanResult(int callbackType, ScanResult result) {
//            Log.i("result", result.toString());
//            BluetoothDevice btDevice = result.getDevice();
//
//            // Check if the device has a name, the Dexcom transmitter always should. Match it with the transmitter id that was entered.
//            // We get the last 2 characters to connect to the correct transmitter if there is more than 1 active or in the room.
//            // If they match, connect to the device.
//            if (btDevice.getName() != null) {
//                String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(gincoseWrap.defaultTransmitter.transmitterId);
//                String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(btDevice.getName());
//
//                if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
//                    //They match, connect to the device.
//                    connectToDevice(btDevice);
//                }
//            }
//        }
//
//        @Override
//        public void onBatchScanResults(List<ScanResult> results) {
//            for (ScanResult sr : results) {
//                Log.i("ScanResult - Results", sr.toString());
//            }
//        }
//
//        @Override
//        public void onScanFailed(int errorCode) {
//            Log.e("Scan Failed", "Error Code: " + errorCode);
//        }
//    };
//
//    private BluetoothAdapter.LeScanCallback mLeScanCallback =
//            new BluetoothAdapter.LeScanCallback() {
//                @Override
//                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
//                    gincoseWrap.currentAct.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            // Check if the device has a name, the Dexcom transmitter always should. Match it with the transmitter id that was entered.
//                            // We get the last 2 characters to connect to the correct transmitter if there is more than 1 active or in the room.
//                            // If they match, connect to the device.
//                            if (device.getName() != null) {
//                                String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(gincoseWrap.defaultTransmitter.transmitterId);
//                                String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());
//
//                                if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
//                                    //They match, connect to the device.
//                                    connectToDevice(device);
//                                }
//                            }
//                        }
//                    });
//                }
//            };
//
//    private void connectToDevice(BluetoothDevice device) {
//        if (mGatt == null) {
//            mGatt = device.connectGatt(gincoseWrap.currentAct, false, gattCallback);
//            stopScan();
//        }
//    }
//
//    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            switch (newState) {
//                case BluetoothProfile.STATE_CONNECTED:
//                    Log.i("gattCallback", "STATE_CONNECTED");
//                    gatt.discoverServices();
//                    break;
//                case BluetoothProfile.STATE_DISCONNECTED:
//                    Log.e("gattCallback", "STATE_DISCONNECTED");
//                    break;
//                default:
//                    Log.e("gattCallback", "STATE_OTHER");
//            }
//        }
//
//        @Override
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            BluetoothGattService cgmService = gatt.getService(UUID.fromString(BluetoothServices.CGMService));
//            Log.i("onServiceDiscovered", cgmService.getUuid().toString());
//
//            if (gincoseWrap.authStatus != null && gincoseWrap.authStatus.authenticated == 1) {
//                BluetoothGattCharacteristic controlCharacteristic = cgmService.getCharacteristic(UUID.fromString(BluetoothServices.Control));
//                if (!mGatt.readCharacteristic(controlCharacteristic)) {
//                    Log.e("onCharacteristicRead", "ReadCharacteristicError");
//                }
//            }
//            else {
//                BluetoothGattCharacteristic authCharacteristic = cgmService.getCharacteristic(UUID.fromString(BluetoothServices.Authentication));
//                if (!mGatt.readCharacteristic(authCharacteristic)) {
//                    Log.e("onCharacteristicRead", "ReadCharacteristicError");
//                }
//            }
//        }
//
//        @Override
//        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                /*if (gincoseWrap.authStatus != null) {
//                    if (gincoseWrap.authStatus.authenticated != 1) {
//                        BluetoothGattCharacteristic authCharacteristic = mGatt.getService(UUID.fromString(BluetoothServices.CGMService)).getCharacteristic(UUID.fromString(BluetoothServices.Authentication));
//
//                        AuthChallengeRxMessage authChallenge = new AuthChallengeRxMessage(authCharacteristic.getValue());
//                        Log.i("AuthChallenge", Arrays.toString(authChallenge.challenge));
//                        Log.i("AuthChallenge", Arrays.toString(authChallenge.tokenHash));
//                    }
//                    else if (gincoseWrap.authStatus.bonded == 5) {
//                        Log.i("CharBytes", Arrays.toString(characteristic.getValue()));
//                        Log.i("CharHex", Extensions.bytesToHex(characteristic.getValue()));
//
//
//                    }
//                }*/
//            }
//
//            mGatt.setCharacteristicNotification(characteristic, false);
//        }
//
//        @Override
//        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.i("CharBytes", Arrays.toString(characteristic.getValue()));
//                Log.i("CharHex", Extensions.bytesToHex(characteristic.getValue()));
//
//                // Request authentication.
//                AuthStatusRxMessage authStatus = new AuthStatusRxMessage(characteristic.getValue());
//                if (authStatus.authenticated == 1 && authStatus.bonded == 1) {
//                    Log.i("Auth", "Transmitter already authenticated");
//                }
//                else {
//                    mGatt.setCharacteristicNotification(characteristic, true);
//
//                    AuthRequestTxMessage authRequest = new AuthRequestTxMessage();
//                    characteristic.setValue(authRequest.data.array());
//                    mGatt.writeCharacteristic(characteristic);
//                }
//            }
//        }
//    };
//}
