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
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.G5Model.Extensions;
import com.eveningoutpost.dexdrip.G5Model.Transmitter;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.Dex_Constants;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import static com.eveningoutpost.dexdrip.xdrip.gs;

public class SystemStatus extends ActivityWithMenu {
    private static final int SMALL_SCREEN_WIDTH = 300;
    //public static final String menu_name = "System Status";
    private TextView version_name_view;
    private TextView collection_method;
    private TextView current_device;
    private TextView connection_status;
    private TextView sensor_status_view;
    private TextView transmitter_status_view;
    private TextView notes;
    private Button restart_collection_service;
    private Button forget_device;
    private Button futureDataDeleteButton;
    private ImageButton refresh;
    private SharedPreferences prefs;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private ActiveBluetoothDevice activeBluetoothDevice;
    private static final String TAG = "SystemStatus";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_status);
        JoH.fixActionBar(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        version_name_view = (TextView)findViewById(R.id.version_name);
        collection_method = (TextView)findViewById(R.id.collection_method);
        connection_status = (TextView)findViewById(R.id.connection_status);
        sensor_status_view = (TextView)findViewById(R.id.sensor_status);
        transmitter_status_view = (TextView)findViewById(R.id.transmitter_status);
        current_device = (TextView)findViewById(R.id.remembered_device);

        notes = (TextView)findViewById(R.id.other_notes);

        restart_collection_service = (Button)findViewById(R.id.restart_collection_service);
        forget_device = (Button)findViewById(R.id.forget_device);
        refresh = (ImageButton)findViewById(R.id.refresh_current_values);
        futureDataDeleteButton = (Button)findViewById(R.id.delete_future_data);

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
            layout = (LinearLayout)findViewById(R.id.layout_transmitter);
            layout.setOrientation(LinearLayout.VERTICAL);
        }

        set_current_values();
        restartButtonListener();
        forgetDeviceListener();
        refreshButtonListener();
    }

    @Override
    public String getMenuName() {
        return getString(R.string.system_status);
    }

    private void set_current_values() {
        activeBluetoothDevice = ActiveBluetoothDevice.first();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        setVersionName();
        setCollectionMethod();
        setCurrentDevice();
        if (Home.get_follower()) {
            setConnectionStatusFollower();
        } else if (prefs.getString("dex_collection_method", "bogus").equals("WifiWixel")) {
            setConnectionStatusWifiWixel();
        } else {
            setConnectionStatus();
        }
        setSensorStatus();
        setTransmitterStatus();
        setNotes();
        futureDataCheck();
    }

    private void setTransmitterStatus() {

        if(prefs.getString("dex_collection_method", "BluetoothWixel").equals("DexcomShare")){
            transmitter_status_view.setText("See Share Receiver");
            return;
        }

        TransmitterData td = TransmitterData.last();

        if (td== null || td.sensor_battery_level == 0){
            transmitter_status_view.setText("not available");
            GcmActivity.requestSensorBatteryUpdate();
        } else if((System.currentTimeMillis() - td.timestamp) > 1000*60*60*24){
            transmitter_status_view.setText("no data in 24 hours");
            GcmActivity.requestSensorBatteryUpdate();
        } else {
            transmitter_status_view.setText("" + td.sensor_battery_level);
            GcmActivity.requestSensorBatteryUpdate(); // always ask
            if (td.sensor_battery_level <= Dex_Constants.TRANSMITTER_BATTERY_EMPTY) {
                transmitter_status_view.append(" - very low");
            } else if (td.sensor_battery_level <= Dex_Constants.TRANSMITTER_BATTERY_LOW) {
                transmitter_status_view.append(" - low");
                transmitter_status_view.append("\n(experimental interpretation)");
            } else {
                transmitter_status_view.append(" - ok");
            }
        }

    }


    private void setSensorStatus(){
        StringBuilder sensor_status= new StringBuilder();
        if(Sensor.isActive()){
            Sensor sens = Sensor.currentSensor();
            Date date = new Date(sens.started_at);
            DateFormat df = new SimpleDateFormat();
            sensor_status.append(df.format(date));
            sensor_status.append(" (");
            sensor_status.append((System.currentTimeMillis() - sens.started_at) / (1000 * 60 * 60 * 24));
            sensor_status.append("d ");
            sensor_status.append(((System.currentTimeMillis() - sens.started_at) % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
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
            int versionNumber = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionCode;
            versionName += "\nCode: "+BuildConfig.buildVersion + "\nDowngradable to: "+versionNumber;
            version_name_view.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            //e.printStackTrace();
            Log.e(this.getClass().getSimpleName(),"PackageManager.NameNotFoundException:" + e.getMessage());
        }
    }

    private void setCollectionMethod() {
        collection_method.setText(prefs.getString("dex_collection_method", "BluetoothWixel").replace("Dexbridge", "xBridge"));
    }

    public void setCurrentDevice() {
        if(activeBluetoothDevice != null) {
            current_device.setText(activeBluetoothDevice.name);
        } else {
            current_device.setText("None Set");
        }

        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("DexcomG5") == 0) {
            Transmitter defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter != null) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if ((pairedDevices != null) && (pairedDevices.size() > 0)) {
                    for (BluetoothDevice device : pairedDevices) {
                        if (device.getName() != null) {

                            String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                            String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());

                            if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                                current_device.setText(defaultTransmitter.transmitterId);
                            }

                        }
                    }
                }
            } else {
                current_device.setText("No Bluetooth");
            }
        }
    }

    private void setConnectionStatusFollower() {
        if (GcmListenerSvc.lastMessageReceived == 0) {
            connection_status.setText(getApplicationContext().getString(R.string.no_data));
        } else {
            connection_status.setText((JoH.qs((JoH.ts() - GcmListenerSvc.lastMessageReceived) / 60000, 0)) + " mins ago");
        }
    }
    private void setConnectionStatusWifiWixel() {
        if (ParakeetHelper.isParakeetCheckingIn())
        {
            connection_status.setText(ParakeetHelper.parakeetStatusString());
        } else {
            connection_status.setText(getApplicationContext().getString(R.string.no_data));
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
            connection_status.setText(getApplicationContext().getString(R.string.connected));
        } else {
            connection_status.setText(getApplicationContext().getString(R.string.not_connected));
        }

        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if(collection_method.compareTo("DexcomG5") == 0) {
            Transmitter defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter != null) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        if (device.getName() != null) {

                            String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                            String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());

                            if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                                final String fw = G5CollectionService.getFirmwareVersionString(defaultTransmitter.transmitterId);
                                connection_status.setText(device.getName() + " Authed" + ((fw != null) ? ("\n" + fw) : ""));
                                break;
                            }

                        }
                    }
                }
            } else {
                connection_status.setText(getApplicationContext().getString(R.string.no_bluetooth));
            }
        }
    }

    private void setNotes() {
        try {
            if ((mBluetoothManager == null) || ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) && (mBluetoothManager.getAdapter() == null))) {
                notes.append("\n- This device does not seem to support bluetooth");
            } else {
                if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)
                        && !mBluetoothManager.getAdapter().isEnabled()) {
                    notes.append("\n- Bluetooth seems to be turned off");
                } else {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        notes.append("\n- The android version of this device is not compatible with Bluetooth Low Energy");
                    }
                }
            }
        } catch (NullPointerException e)
        {
            Log.e(TAG,"Got nullpointer exception in setNotes ",e);
        }
    }

    private void futureDataCheck() {
        futureDataDeleteButton.setVisibility(View.GONE);
        final List<BgReading> futureReadings = BgReading.futureReadings();
        final List<Calibration> futureCalibrations = Calibration.futureCalibrations();
        if((futureReadings != null && futureReadings.size() > 0) || (futureCalibrations != null && futureCalibrations.size() > 0)) {
            notes.append("\n- Your device has future data on it, Please double check the time and timezone on this phone.");
            futureDataDeleteButton.setVisibility(View.VISIBLE);
        }
        futureDataDeleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(futureReadings != null && futureReadings.size() > 0) {
                    for (BgReading bgReading : futureReadings) {
                        bgReading.calculated_value = 0;
                        bgReading.raw_data = 0;
                        bgReading.timestamp = 0;
                        bgReading.save();
                    }
                }
                if(futureCalibrations != null && futureCalibrations.size() > 0) {
                    for (Calibration calibration : futureCalibrations) {
                        calibration.slope_confidence = 0;
                        calibration.sensor_confidence = 0;
                        calibration.timestamp = 0;
                        calibration.save();
                    }
                }
            }
        });
    }

    private void restartButtonListener() {
        restart_collection_service.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                v.setEnabled(false);
                JoH.static_toast_short(gs(R.string.restarting_collector));
                v.setAlpha(0.2f);
                CollectionServiceStarter.restartCollectionService(getApplicationContext());
                set_current_values();
                JoH.runOnUiThreadDelayed(new Runnable() {
                    @Override
                    public void run() {
                        v.setEnabled(true);
                        v.setAlpha(1.0f);
                        set_current_values();
                    }
                }, 2000);
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

                String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
                if(collection_method.compareTo("DexcomG5") == 0) {
                    Transmitter defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
                    mBluetoothAdapter = mBluetoothManager.getAdapter();

                    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                    if ((pairedDevices != null) && (pairedDevices.size() > 0)) {
                        for (BluetoothDevice device : pairedDevices) {
                            if (device.getName() != null) {

                                String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                                String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());

                                if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                                    try {
                                        Method m = device.getClass().getMethod("removeBond", (Class[]) null);
                                        m.invoke(device, (Object[]) null);
                                        notes.append("\nG5 Transmitter unbonded, switch device mode to prevent re-pairing to G5.");
                                    } catch (Exception e) { Log.e("SystemStatus", e.getMessage(), e); }
                                }

                            }
                        }
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
