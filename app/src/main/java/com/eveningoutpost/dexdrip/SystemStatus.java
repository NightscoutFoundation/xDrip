package com.eveningoutpost.dexdrip;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SystemStatus extends ActivityWithMenu {
    private static final int SMALL_SCREEN_WIDTH = 300;
    public static final String menu_name = "System Status";
    private TextView version_name_view;
    private TextView collection_method;
    private TextView current_device;
    private TextView connection_status;
    private TextView sensor_status_view;
    private TextView notes;
    private Button restart_collection_service;
    private Button forget_device;
    private ImageButton refresh;
    private SharedPreferences prefs;
    private BluetoothManager mBluetoothManager;
    private ActiveBluetoothDevice activeBluetoothDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_status);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
       version_name_view = (TextView)findViewById(R.id.version_name);
        collection_method = (TextView)findViewById(R.id.collection_method);
        connection_status = (TextView)findViewById(R.id.connection_status);
        sensor_status_view = (TextView)findViewById(R.id.sensor_status);
        current_device = (TextView)findViewById(R.id.remembered_device);

        notes = (TextView)findViewById(R.id.other_notes);

        restart_collection_service = (Button)findViewById(R.id.restart_collection_service);
        forget_device = (Button)findViewById(R.id.forget_device);
        refresh = (ImageButton)findViewById(R.id.refresh_current_values);

        //check for small devices:
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        if(width<SMALL_SCREEN_WIDTH){
            //adapt to small screen
            LinearLayout layout = (LinearLayout)findViewById(R.id.layout_collectionmethod);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout)findViewById(R.id.layout_version);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout)findViewById(R.id.layout_status);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout)findViewById(R.id.layout_device);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout)findViewById(R.id.layout_sensor);
            layout.setOrientation(LinearLayout.VERTICAL);
        }

        set_current_values();
        restartButtonListener();
        forgetDeviceListener();
        refreshButtonListener();
    }

    @Override
    public String getMenuName() {
        return menu_name;
    }

    private void set_current_values() {
        activeBluetoothDevice = ActiveBluetoothDevice.first();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        setVersionName();
        setCollectionMethod();
        setCurrentDevice();
        setConnectionStatus();
        setSensorStatus();
        setNotes();
    }


    private void setSensorStatus(){
        StringBuilder sensor_status= new StringBuilder();
        if(Sensor.isActive()){
            Sensor sens = Sensor.currentSensor();
            Date date = new Date(sens.started_at);
            DateFormat df = new SimpleDateFormat();
            sensor_status.append(df.format(date));
            sensor_status.append(" (");
            sensor_status.append((int) (System.currentTimeMillis() - sens.started_at) / (1000 * 60 * 60 * 24));
            sensor_status.append("d ");
            sensor_status.append((int)((System.currentTimeMillis() - sens.started_at)%(1000 * 60 * 60 * 24))/(1000*60*60));
            sensor_status.append("h)");
        } else {
            sensor_status.append("not available");
        }
        sensor_status_view.setText(sensor_status.toString());

    }


    private void setVersionName(){
        String versionName;
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName;
            version_name_view.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            //e.printStackTrace();
            Log.e(this.getClass().getSimpleName(),"PackageManager.NameNotFoundException:" + e.getMessage());
        }
    }

    private void setCollectionMethod() {
        collection_method.setText(prefs.getString("dex_collection_method", "BluetoothWixel"));
    }

    public void setCurrentDevice() {
        if(activeBluetoothDevice != null) {
            current_device.setText(activeBluetoothDevice.name);
        } else {
            current_device.setText("None Set");
        }
    }

    private void setConnectionStatus() {
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

    private void setNotes() {
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

    private void restartButtonListener() {
        restart_collection_service.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CollectionServiceStarter.restartCollectionService(getApplicationContext());
                set_current_values();
            }
        });
    }

    private void forgetDeviceListener() {
        forget_device.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mBluetoothManager != null && ActiveBluetoothDevice.first() != null) {
                    final BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
                    if(bluetoothAdapter != null) {
                        for( BluetoothDevice bluetoothDevice : bluetoothAdapter.getBondedDevices()) {
                            if(bluetoothDevice.getAddress().compareTo(ActiveBluetoothDevice.first().address) == 0) {
                                try {
                                    Method m = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
                                    m.invoke(bluetoothDevice, (Object[]) null);
                                    notes.append("\n- Bluetooth unbonded, if using share tell it to forget your device.");
                                    notes.append("\n- Scan for devices again to set connection back up!");
                                } catch (Exception e) { Log.e("SystemStatus", e.getMessage(), e); }
                            }
                        }

                        ActiveBluetoothDevice.forget();
                        bluetoothAdapter.disable();


                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                bluetoothAdapter.enable();
                                set_current_values();
                                mHandler2.postDelayed(new Runnable() {
                                    public void run() {
                                        CollectionServiceStarter.restartCollectionService(getApplicationContext());
                                        set_current_values();
                                    }
                                }, 5000);
                            }
                        }, 1000);
                    }
                }
            }
        });
    }

    private void refreshButtonListener() {
        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                set_current_values();
            }
        });
    }

    private Handler mHandler = new Handler();
    private Handler mHandler2 = new Handler();

}
