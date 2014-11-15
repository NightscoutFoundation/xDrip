package com.eveningoutpost.dexdrip;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Created by stephenblack on 11/3/14.
 */
@Table(name = "ActiveBluetoothDevice", id = BaseColumns._ID)
public class ActiveBluetoothDevice extends Model {
    @Column(name = "name")
    public String name;

    @Column(name = "address")
    public String address;
}