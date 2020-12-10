package com.eveningoutpost.dexdrip.Models;

import android.content.SharedPreferences;
import android.provider.BaseColumns;
import android.support.v7.preference.PreferenceManager;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.UtilityModels.Blukon;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * Created by Emma Black on 11/3/14.
 */
@Table(name = "ActiveBluetoothDevice", id = BaseColumns._ID)
public class ActiveBluetoothDevice extends Model {
    @Column(name = "name")
    public String name;

    @Column(name = "address")
    public String address;

    @Column(name = "connected")
    public boolean connected;


    public static final Object table_lock = new Object();

    public static synchronized ActiveBluetoothDevice first() {
        return new Select()
                .from(ActiveBluetoothDevice.class)
                .orderBy("_ID asc")
                .executeSingle();
    }

    public static synchronized  void forget() {
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if (activeBluetoothDevice != null) {
            activeBluetoothDevice.delete();
        }
    }

    public static synchronized  void connected() {
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if(activeBluetoothDevice != null) {
            activeBluetoothDevice.connected = true;
            activeBluetoothDevice.save();
        }
    }

    public static synchronized  void disconnected() {
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if(activeBluetoothDevice != null) {
            activeBluetoothDevice.connected = false;
            activeBluetoothDevice.save();
        }
    }

    public static synchronized boolean is_connected() {
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        return (activeBluetoothDevice != null && activeBluetoothDevice.connected);
    }

    public static synchronized void setDevice(String name, String address) {
        ActiveBluetoothDevice btDevice;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        synchronized (ActiveBluetoothDevice.table_lock) {
             btDevice = new Select().from(ActiveBluetoothDevice.class)
                    .orderBy("_ID desc")
                    .executeSingle();
        }
        prefs.edit().putString("last_connected_device_address", address).apply();
        Blukon.clearPin();
        if (btDevice == null) {
            ActiveBluetoothDevice newBtDevice = new ActiveBluetoothDevice();
            newBtDevice.name = name;
            newBtDevice.address = address;
            newBtDevice.save();
        } else {
            btDevice.name = name;
            btDevice.address = address;
            btDevice.save();
        }
    }
}
