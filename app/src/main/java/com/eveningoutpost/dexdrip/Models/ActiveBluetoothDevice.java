package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

/**
 * Created by stephenblack on 11/3/14.
 */
@Table(name = "ActiveBluetoothDevice", id = BaseColumns._ID)
public class ActiveBluetoothDevice extends Model {
    @Column(name = "name")
    public String name;

    @Column(name = "address")
    public String address;

    @Column(name = "connected")
    public boolean connected;

    public static ActiveBluetoothDevice first() {
        return new Select()
                .from(ActiveBluetoothDevice.class)
                .orderBy("_ID asc")
                .executeSingle();
    }

    public static void forget() {
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if (activeBluetoothDevice != null) {
            activeBluetoothDevice.delete();
        }
    }

    public static void connected() {
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if(activeBluetoothDevice != null) {
            activeBluetoothDevice.connected = true;
            activeBluetoothDevice.save();
        }
    }

    public static void disconnected() {
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if(activeBluetoothDevice != null) {
            activeBluetoothDevice.connected = false;
            activeBluetoothDevice.save();
        }
    }

    public static boolean is_connected() {
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if(activeBluetoothDevice != null) {
            return activeBluetoothDevice.connected;
        }
        return false;
    }

}
