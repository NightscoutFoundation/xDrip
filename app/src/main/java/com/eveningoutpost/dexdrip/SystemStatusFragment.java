package com.eveningoutpost.dexdrip;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.g5model.Extensions;
import com.eveningoutpost.dexdrip.g5model.Transmitter;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.Dex_Constants;
import com.eveningoutpost.dexdrip.models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.TransmitterData;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.services.DexCollectionService;
import com.eveningoutpost.dexdrip.services.G5CollectionService;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.SensorStatus;
import com.eveningoutpost.dexdrip.databinding.ActivitySystemStatusBinding;
import com.eveningoutpost.dexdrip.ui.MicroStatus;
import com.eveningoutpost.dexdrip.ui.MicroStatusImpl;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.google.android.gms.wearable.DataMap;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.DexcomG5;
import static com.eveningoutpost.dexdrip.xdrip.gs;

public class SystemStatusFragment extends Fragment {
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
    private BroadcastReceiver serviceDataReceiver;

    //@Inject
    MicroStatus microStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        //Injectors.getMicroStatusComponent().inject(this);
        requestWearCollectorStatus();
        serviceDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                final String action = intent.getAction();
                //final String msg = intent.getStringExtra("data");
                Bundle bundle = intent.getBundleExtra("data");
                if (bundle != null) {
                    DataMap dataMap = DataMap.fromBundle(bundle);
                    String lastState = dataMap.getString("lastState", "");
                    long last_timestamp = dataMap.getLong("timestamp", 0);
                    UserError.Log.d(TAG, "serviceDataReceiver onReceive:" + action + " :: " + lastState + " last_timestamp :: " + last_timestamp);
                    switch (action) {
                        case WatchUpdaterService.ACTION_BLUETOOTH_COLLECTION_SERVICE_UPDATE:
                            switch (DexCollectionType.getDexCollectionType()) {
                                case DexcomG5:
                                    G5CollectionService.setWatchStatus(dataMap);//msg, last_timestamp
                                    break;
                                case DexcomShare:
                                    if (lastState != null && !lastState.isEmpty()) {
                                        setConnectionStatus(lastState);//TODO getLastState() in non-G5 Services
                                    }
                                    break;
                                default:
                                    DexCollectionService.setWatchStatus(dataMap);//msg, last_timestamp
                                    if (lastState != null && !lastState.isEmpty()) {
                                        setConnectionStatus(lastState);
                                    }
                                    break;
                            }
                            break;
                    }
                }
            }
        };
        final ActivitySystemStatusBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.activity_system_status, container, false);
        microStatus = new MicroStatusImpl();
        binding.setMs(microStatus);
        return binding.getRoot();
    }

    private void requestWearCollectorStatus() {
        final PowerManager.WakeLock wl = JoH.getWakeLock("ACTION_STATUS_COLLECTOR",120000);
        if (Home.get_enable_wear()) {
            if (DexCollectionType.getDexCollectionType().equals(DexcomG5)) {
                startWatchUpdaterService(safeGetContext(), WatchUpdaterService.ACTION_STATUS_COLLECTOR, TAG, "getBatteryStatusNow", G5CollectionService.getBatteryStatusNow);
            }
            else {
                startWatchUpdaterService(safeGetContext(), WatchUpdaterService.ACTION_STATUS_COLLECTOR, TAG);
            }
        }
        JoH.releaseWakeLock(wl);
    }

    @Override
    public void onPause() {
        if (serviceDataReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(safeGetContext()).unregisterReceiver(serviceDataReceiver);
            } catch (IllegalArgumentException e) {
                UserError.Log.e(TAG, "broadcast receiver not registered", e);
            }
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WatchUpdaterService.ACTION_BLUETOOTH_COLLECTION_SERVICE_UPDATE);
        LocalBroadcastManager.getInstance(safeGetContext()).registerReceiver(serviceDataReceiver, intentFilter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // setContentView(R.layout.activity_system_status);
        // JoH.fixActionBar(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        final View v = getView();
        version_name_view = (TextView) v.findViewById(R.id.version_name);
        collection_method = (TextView) v.findViewById(R.id.collection_method);
        connection_status = (TextView) v.findViewById(R.id.connection_status);
        sensor_status_view = (TextView) v.findViewById(R.id.sensor_status);
        transmitter_status_view = (TextView) v.findViewById(R.id.transmitter_status);
        current_device = (TextView) v.findViewById(R.id.remembered_device);

        notes = (TextView) v.findViewById(R.id.other_notes);


        restart_collection_service = (Button) v.findViewById(R.id.restart_collection_service);
        forget_device = (Button) v.findViewById(R.id.forget_device);
        refresh = (ImageButton) v.findViewById(R.id.refresh_current_values);
        futureDataDeleteButton = (Button) v.findViewById(R.id.delete_future_data);

        //check for small devices:
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        if (width < SMALL_SCREEN_WIDTH) {
            //adapt to small screen
            LinearLayout layout = (LinearLayout) v.findViewById(R.id.layout_collectionmethod);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout) v.findViewById(R.id.layout_version);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout) v.findViewById(R.id.layout_status);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout) v.findViewById(R.id.layout_device);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout) v.findViewById(R.id.layout_sensor);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout = (LinearLayout) v.findViewById(R.id.layout_transmitter);
            layout.setOrientation(LinearLayout.VERTICAL);
        }

        set_current_values();
        restartButtonListener();
        forgetDeviceListener();
        refreshButtonListener();
    }

    //@Override
    //public String getMenuName() {
    //   return getString(R.string.system_status);
    //}

    private void set_current_values() {
        notes.setText("");
        activeBluetoothDevice = ActiveBluetoothDevice.first();
        if (Build.VERSION.SDK_INT >= 18) {
            mBluetoothManager = (BluetoothManager) safeGetContext().getSystemService(Context.BLUETOOTH_SERVICE);
        }
        setVersionName();
        //setCollectionMethod();
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

       /* if (notes.getText().length()==0) {
            notes.setText("Swipe for more status pages!");
        }*/
    }

    private void setTransmitterStatus() {

        if (prefs.getString("dex_collection_method", "BluetoothWixel").equals("DexcomShare")) {
            transmitter_status_view.setText("See Share Receiver");
            return;
        }

        TransmitterData td = TransmitterData.last();

        if (td == null || td.sensor_battery_level == 0) {
            transmitter_status_view.setText("not available");
            GcmActivity.requestSensorBatteryUpdate();
        } else if ((System.currentTimeMillis() - td.timestamp) > 1000 * 60 * 60 * 24) {
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


    private void setSensorStatus() {
        sensor_status_view.setText(SensorStatus.status());
    }


    private void setVersionName() {
        String versionName;
        try {
            versionName = safeGetContext().getPackageManager().getPackageInfo(safeGetContext().getPackageName(), PackageManager.GET_META_DATA).versionName;
            int versionNumber = safeGetContext().getPackageManager().getPackageInfo(safeGetContext().getPackageName(), PackageManager.GET_META_DATA).versionCode;
            versionName += "\nCode: " + BuildConfig.buildVersion + "\nDowngradable to: " + versionNumber;
            version_name_view.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            //e.printStackTrace();
            Log.e(this.getClass().getSimpleName(), "PackageManager.NameNotFoundException:" + e.getMessage());
        }
    }

    private void setCollectionMethod() {
        collection_method.setText(prefs.getString("dex_collection_method", "BluetoothWixel").replace("Dexbridge", "xBridge"));
    }

    public void setCurrentDevice() {
        if (activeBluetoothDevice != null) {
            current_device.setText(activeBluetoothDevice.name);
        } else {
            current_device.setText("None Set");
        }

        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("DexcomG5") == 0) {
            Transmitter defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
            if (Build.VERSION.SDK_INT >= 18) {
                mBluetoothAdapter = mBluetoothManager.getAdapter();
            }
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
            connection_status.setText(safeGetContext().getString(R.string.no_data));
        } else {
            connection_status.setText((JoH.qs((JoH.ts() - GcmListenerSvc.lastMessageReceived) / 60000, 0)) + " mins ago");
        }
    }

    private void setConnectionStatusWifiWixel() {
        if (ParakeetHelper.isParakeetCheckingIn()) {
            connection_status.setText(ParakeetHelper.parakeetStatusString());
        } else {
            connection_status.setText(safeGetContext().getString(R.string.no_data));
        }
    }

    public void setConnectionStatus(String msg) {
        if (msg != null && !msg.isEmpty()) {
            connection_status.setText(msg);
        }
    }

    private void setConnectionStatus() {
        boolean connected = false;
        if (mBluetoothManager != null && activeBluetoothDevice != null && (Build.VERSION.SDK_INT >= 18)) {
            for (BluetoothDevice bluetoothDevice : mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
                if (bluetoothDevice.getAddress().compareTo(activeBluetoothDevice.address) == 0) {
                    connected = true;
                }
            }
        }
        if (connected) {
            connection_status.setText(safeGetContext().getString(R.string.connected));
        } else {
            connection_status.setText(safeGetContext().getString(R.string.not_connected));
        }

        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("DexcomG5") == 0) {
            Transmitter defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
            if (Build.VERSION.SDK_INT >= 18) mBluetoothAdapter = mBluetoothManager.getAdapter();
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
                connection_status.setText(safeGetContext().getString(R.string.no_bluetooth)); 
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
        } catch (NullPointerException e) {
            Log.e(TAG, "Got nullpointer exception in setNotes ", e);
        }
    }

    private void futureDataCheck() {
        futureDataDeleteButton.setVisibility(View.GONE);
        final List<BgReading> futureReadings = BgReading.futureReadings();
        final List<Calibration> futureCalibrations = Calibration.futureCalibrations();
        if ((futureReadings != null && futureReadings.size() > 0) || (futureCalibrations != null && futureCalibrations.size() > 0)) {
            notes.append("\n- Your device has future data on it, Please double check the time and timezone on this phone.");
            futureDataDeleteButton.setVisibility(View.VISIBLE);
        }
        futureDataDeleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (futureReadings != null && futureReadings.size() > 0) {
                    for (BgReading bgReading : futureReadings) {
                        bgReading.calculated_value = 0;
                        bgReading.raw_data = 0;
                        bgReading.timestamp = 0;
                        bgReading.save();
                    }
                }
                if (futureCalibrations != null && futureCalibrations.size() > 0) {
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
                startWatchUpdaterService(safeGetContext(), WatchUpdaterService.ACTION_START_COLLECTOR, TAG);
                CollectionServiceStarter.restartCollectionService(safeGetContext());
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
                if (mBluetoothManager != null && ActiveBluetoothDevice.first() != null) {
                    final BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
                    if (bluetoothAdapter != null) {
                        for (BluetoothDevice bluetoothDevice : bluetoothAdapter.getBondedDevices()) {
                            if (bluetoothDevice.getAddress().compareTo(ActiveBluetoothDevice.first().address) == 0) {
                                try {
                                    Method m = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
                                    m.invoke(bluetoothDevice, (Object[]) null);
                                    notes.append("\n- Bluetooth unbonded, if using share tell it to forget your device.");
                                    notes.append("\n- Scan for devices again to set connection back up!");
                                } catch (Exception e) {
                                    Log.e("SystemStatus", e.getMessage(), e);
                                }
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
                                        CollectionServiceStarter.restartCollectionService(safeGetContext());
                                        set_current_values();
                                    }
                                }, 5000);
                            }
                        }, 1000);
                    }
                }

                String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
                if (collection_method.compareTo("DexcomG5") == 0) {
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
                                    } catch (Exception e) {
                                        Log.e("SystemStatus", e.getMessage(), e);
                                    }
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
                requestWearCollectorStatus();
            }
        });
    }

    private Handler mHandler = new Handler();
    private Handler mHandler2 = new Handler();

    private Context safeGetContext() {
        if (isAdded()) {
            return getActivity();
        } else {
            return xdrip.getAppContext();
        }
    }

}
