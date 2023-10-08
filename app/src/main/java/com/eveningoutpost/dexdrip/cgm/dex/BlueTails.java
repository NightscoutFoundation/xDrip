package com.eveningoutpost.dexdrip.cgm.dex;

import static com.eveningoutpost.dexdrip.g5model.BluetoothServices.Control;
import static com.eveningoutpost.dexdrip.g5model.BluetoothServices.ProbablyBackfill;
import static com.eveningoutpost.dexdrip.cgm.dex.ClassifierAction.BACKFILL;
import static com.eveningoutpost.dexdrip.cgm.dex.ClassifierAction.CONNECT;
import static com.eveningoutpost.dexdrip.cgm.dex.ClassifierAction.CONTROL;
import static com.eveningoutpost.dexdrip.cgm.dex.ClassifierAction.action;
import static com.eveningoutpost.dexdrip.cgm.dex.ClassifierAction.lastReadingTimestamp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.bt.BtCallBack3;
import com.eveningoutpost.dexdrip.utils.bt.ConnectReceiver;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.HashMap;
import java.util.UUID;

import lombok.Getter;
import lombok.val;

/**
 * JamOrHam
 */

public class BlueTails extends BluetoothGattCallback implements BtCallBack3 {

    private static final String TAG = BlueTails.class.getSimpleName();
    private static final String PREF = "bluetails_enabled";
    @Getter
    private static final BlueTails instance = new BlueTails();
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private final byte[] EMPTY_PAYLOAD = new byte[1];
    private final HashMap<UUID, ClassifierSignpost> characteristics = new HashMap<>();

    {
        characteristics.put(Control, new ClassifierSignpost(Control, CONTROL));
        characteristics.put(ProbablyBackfill, new ClassifierSignpost(ProbablyBackfill, BACKFILL));
    }

    public static void immortality() {
        if (enabled()) {
            getInstance().hello();
        } else {
            lastReadingTimestamp = 0;
        }
    }

    public static boolean enabled() {
        return Pref.getBooleanDefaultFalse(PREF);
    }

    void hello() {
        if (mBluetoothManager == null) {
            mBluetoothManager= (BluetoothManager) xdrip.getAppContext().getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }
        if (mBluetoothAdapter != null) {
            ConnectReceiver.addCallBack(this, TAG);
        } else {
            UserError.Log.e(TAG, "Cannot find bluetooth adapter");
        }
    }

    @Override
    public void btCallback3(final String mac, final String status, final String name, final Bundle bundle, final BluetoothDevice device) {
        if (enabled()) {
            UserError.Log.d(TAG, "Got connection for: " + mac + " " + status + " " + name + " " + " device: " + device);
            val pairedDevices = mBluetoothAdapter.getBondedDevices();
            if ((pairedDevices != null) && (pairedDevices.size() > 0)) {
                for (val d : pairedDevices) {
                    if (d.getAddress().equals(mac)) {
                        if (d.getName().startsWith("DXC") || d.getName().startsWith("Dex")) {
                            val gatt = device.connectGatt(xdrip.getAppContext(), false, getInstance());
                        }
                        break;
                    }
                }
            }
        } else {
            UserError.Log.d(TAG, "Is not enabled on callback");
        }
    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        UserError.Log.d(TAG, "Connection state changed: " + status + " new state: " + newState);

        if (newState == STATE_CONNECTED) {
            gatt.discoverServices();
            action(CONNECT, EMPTY_PAYLOAD);
        }
        if (newState == STATE_DISCONNECTED) {
            gatt.close();
        }
    }

    private void enableNotifications(final BluetoothGatt gatt) {
        for (val v : characteristics.values()) {
            if (v.characteristic != null) {
                gatt.setCharacteristicNotification(v.characteristic, true);
                UserError.Log.d(TAG, "Enable notification: " + v.action);
            }
        }
    }

    @Override
    public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
        UserError.Log.d(TAG, "Services discovered start");
        for (val service : gatt.getServices()) {
            for (val cha : service.getCharacteristics()) {
                val c = characteristics.get(cha.getUuid());
                if (c != null) {
                    c.characteristic = cha;
                    UserError.Log.d(TAG, "Found " + c.action);
                }
            }
        }
        enableNotifications(gatt);
    }

    @Override
    public synchronized void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        val bytes = characteristic.getValue();
        UserError.Log.d(TAG, "Characteristic changed: " + characteristic.getUuid() + " " + HexDump.dumpHexString(bytes));

        val achar = characteristics.get(characteristic.getUuid());
        if (achar != null) {
            action(achar.action, bytes);
        } else {
            UserError.Log.wtf(TAG, "Got onCharacteristicChanged for something we don't know about: " + characteristic.getUuid());
        }
    }

}
