package com.eveningoutpost.dexdrip.utils.bt;


import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

// jamorham
// interface for providing bluetooth status callbacks

public interface BtCallBack3 {

    void btCallback3(String mac, String status, String name, Bundle bundle, BluetoothDevice device);

}