package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;


public class SystemStatus extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private String menu_name = "System Status";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    public TextView collection_method;
    public TextView current_device;
    public TextView connection_status;
    public TextView notes;
    public Button restart_collection_service;
    public Button forget_device;
    public SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_status);

        collection_method = (TextView)findViewById(R.id.collection_method);
        connection_status = (TextView)findViewById(R.id.connection_status);
        current_device = (TextView)findViewById(R.id.remembered_device);

        notes = (TextView)findViewById(R.id.other_notes);

        restart_collection_service = (Button)findViewById(R.id.restart_collection_service);
        forget_device = (Button)findViewById(R.id.forget_device);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
        set_current_values();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        mNavigationDrawerFragment.swapContext(position);
    }

    private void set_current_values() {
        collection_method.setText(prefs.getString("collection_method", "BluetoothWixel"));
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if(activeBluetoothDevice != null) {
            current_device.setText(activeBluetoothDevice.name);
            if(activeBluetoothDevice.connected) {
                connection_status.setText("Connected");
            } else {
                connection_status.setText("Not Connected");
            }
        } else {
            current_device.setText("None Set");
            connection_status.setText("Not Connected");
        }
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
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

}
