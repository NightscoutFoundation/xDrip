package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;

import java.lang.reflect.Method;


public class SystemStatus extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private String menu_name = "System Status";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    public TextView collection_method;
    public TextView current_device;
    public TextView connection_status;
    public TextView notes;
    public Button restart_collection_service;
    public Button forget_device;
    public ImageButton refresh;
    public SharedPreferences prefs;
    public BluetoothManager mBluetoothManager;
    public ActiveBluetoothDevice activeBluetoothDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_status);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);

        collection_method = (TextView)findViewById(R.id.collection_method);
        connection_status = (TextView)findViewById(R.id.connection_status);
        current_device = (TextView)findViewById(R.id.remembered_device);

        notes = (TextView)findViewById(R.id.other_notes);

        restart_collection_service = (Button)findViewById(R.id.restart_collection_service);
        forget_device = (Button)findViewById(R.id.forget_device);
        refresh = (ImageButton)findViewById(R.id.refresh_current_values);

        set_current_values();
        restartButtonListener();
        forgetDeviceListener();
        refreshButtonListener();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        mNavigationDrawerFragment.swapContext(position);
    }

    private void set_current_values() {
        activeBluetoothDevice = ActiveBluetoothDevice.first();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        setCollectionMethod();
        setCurrentDevice();
        setConnectionStatus();
        setNotes();
    }

    public void setCollectionMethod() {
        collection_method.setText(prefs.getString("dex_collection_method", "BluetoothWixel"));
    }

    public void setCurrentDevice() {
        if(activeBluetoothDevice != null) {
            current_device.setText(activeBluetoothDevice.name);
        } else {
            current_device.setText("None Set");
        }
    }

    public void setConnectionStatus() {
        boolean connected = false;
        if (mBluetoothManager != null && activeBluetoothDevice != null) {
            for (BluetoothDevice bluetoothDevice : mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
                if (bluetoothDevice.getAddress().compareTo(activeBluetoothDevice.address) == 0) {
                    connected = true;
                }
            }
        }
        if(connected) {
            connection_status.setText("Connected");
        } else {
            connection_status.setText("Not Connected");
        }
    }

    public void setNotes() {
        if(mBluetoothManager == null) {
            notes.append("\n- This device does not seem to support bluetooth");
        } else {
            if(!mBluetoothManager.getAdapter().isEnabled()) {
                notes.append("\n- Bluetooth seems to be turned off");
            } else {
                if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){
                    notes.append("\n- The android version of this device is not compatible with Bluetooth Low Energy");
                }
            }
        }
    }

    public void restartButtonListener() {
        restart_collection_service.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CollectionServiceStarter.restartCollectionService(getApplicationContext());
                set_current_values();
            }
        });
    }

    public void forgetDeviceListener() {
        forget_device.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mBluetoothManager != null && ActiveBluetoothDevice.first() != null) {
                    BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
                    if(bluetoothAdapter != null) {
                        for( BluetoothDevice bluetoothDevice : bluetoothAdapter.getBondedDevices()) {
                            if(bluetoothDevice.getAddress().compareTo(ActiveBluetoothDevice.first().address) == 0) {
                                try {
                                    Method m = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
                                    m.invoke(bluetoothDevice, (Object[]) null);
                                    notes.append("\n- Bluetooth unbonded, if using share tell it to forget your device.");
                                    notes.append("\n- Scan for devices again to set connection back up!");
                                } catch (Exception e) { Log.e("SystemStatus", e.getMessage()); }
                            }
                        }

                        ActiveBluetoothDevice.forget();
                        bluetoothAdapter.disable();
                        bluetoothAdapter.enable();
                        try {
                            wait(1000);
                        } catch(Exception e) {
                            Log.e("SystemStatus", "Error stalling");
                        }
                    }
                }
                CollectionServiceStarter.restartCollectionService(getApplicationContext());
                set_current_values();
            }
        });
    }

    public void refreshButtonListener() {
        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                set_current_values();
            }
        });
    }
}
