package com.eveningoutpost.dexdrip;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.Blukon;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.cgm.medtrum.Medtrum;
import com.eveningoutpost.dexdrip.utils.AndroidBarcode;
import com.eveningoutpost.dexdrip.utils.ListActivityWithMenu;
import com.eveningoutpost.dexdrip.utils.LocationHelper;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lecho.lib.hellocharts.util.ChartUtils;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Medtrum.getDeviceInfoStringFromLegacy;
import static com.eveningoutpost.dexdrip.xdrip.gs;

@TargetApi(android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothScan extends ListActivityWithMenu {

    private final static String TAG = BluetoothScan.class.getSimpleName();
    private static final long SCAN_PERIOD = 30000;
    private boolean is_scanning;

    private Handler mHandler;
    private LeDeviceListAdapter mLeDeviceListAdapter;

    private ArrayList<BluetoothDevice> found_devices;
    private BluetoothAdapter bluetooth_adapter;
    private BluetoothLeScanner lollipopScanner;
    private Map<String, byte[]> adverts = new HashMap<>();
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (device.getName() != null && device.getName().length() > 0) {
                                mLeDeviceListAdapter.addDevice(device);
                                if (scanRecord != null)
                                    adverts.put(device.getAddress(), scanRecord);
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            };
    private ScanCallback mScanCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.OldAppTheme); // or null actionbar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_scan);
        final BluetoothManager bluetooth_manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        bluetooth_adapter = bluetooth_manager.getAdapter();
        mHandler = new Handler();


        if (bluetooth_adapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!bluetooth_manager.getAdapter().isEnabled()) {
            if (Pref.getBoolean("automatically_turn_bluetooth_on", true)) {
                JoH.setBluetoothEnabled(getApplicationContext(), true);
                Toast.makeText(this, "Trying to turn Bluetooth on", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Please turn Bluetooth on!", Toast.LENGTH_LONG).show();
            }
        } else {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Toast.makeText(this, "The android version of this device is not compatible with Bluetooth Low Energy", Toast.LENGTH_LONG).show();
            }
        }
        // Will request that GPS be enabled for devices running Marshmallow or newer.
        LocationHelper.requestLocationForBluetooth(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            initializeScannerCallback();

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
        mLeDeviceListAdapter.notifyDataSetChanged();
    }

    @Override
    public String getMenuName() {
        return getString(R.string.bluetooth_scan);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bluetooth_scan, menu);
        if (!is_scanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
        }
        return true;
    }

    private boolean doScan() {
        BluetoothManager bluetooth_manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        Toast.makeText(this, gs(R.string.scanning), Toast.LENGTH_LONG).show();
        if (bluetooth_manager == null) {
            Toast.makeText(this, "This device does not seem to support bluetooth", Toast.LENGTH_LONG).show();
            return true;
        } else {
            if (!bluetooth_manager.getAdapter().isEnabled()) {
                Toast.makeText(this, "Bluetooth is turned off on this device currently", Toast.LENGTH_LONG).show();
                return true;
            } else {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Toast.makeText(this, "The android version of this device is not compatible with Bluetooth Low Energy", Toast.LENGTH_LONG).show();
                    return true;
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanLeDeviceLollipop(true);
        } else {
            scanLeDevice(true);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (LocationHelper.isLocationPermissionOk(this) && (JoH.ratelimit("auto-start-bt-scan", 20))) {
            doScan();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                return doScan();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(19)
    private synchronized void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            Log.d(TAG, "Start scan 19");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    is_scanning = false;
                    try {
                        if ((bluetooth_adapter != null) && (mLeScanCallback != null)) {
                            bluetooth_adapter.stopLeScan(mLeScanCallback);
                        }
                    } catch (NullPointerException e) {
                        // concurrency pain
                    }
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            is_scanning = true;
            if (bluetooth_adapter != null) bluetooth_adapter.startLeScan(mLeScanCallback);
        } else {
            is_scanning = false;
            if (bluetooth_adapter != null && bluetooth_adapter.isEnabled()) {
                try {
                    bluetooth_adapter.stopLeScan(mLeScanCallback);
                } catch (NullPointerException e) {
                    // concurrency related
                }
            }
        }
        invalidateOptionsMenu();
    }

    @TargetApi(21)
    private void initializeScannerCallback() {
        Log.d(TAG, "initializeScannerCallback");
        mScanCallback = new ScanCallback() {
            @Override
            public void onBatchScanResults(final List<ScanResult> results) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (ScanResult result : results) {
                            BluetoothDevice device = result.getDevice();
                            if (device.getName() != null && device.getName().length() > 0) {
                                mLeDeviceListAdapter.addDevice(device);
                                try {
                                    if (result.getScanRecord() != null)
                                        adverts.put(device.getAddress(), result.getScanRecord().getBytes());
                                } catch (NullPointerException e) {
                                    //
                                }
                            }
                        }
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                final BluetoothDevice device = result.getDevice();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final String deviceName = device.getName();
                        if (deviceName != null && deviceName.length() > 0) {
                            mLeDeviceListAdapter.addDevice(device);
                            try {
                                if (result.getScanRecord() != null)
                                    adverts.put(device.getAddress(), result.getScanRecord().getBytes());
                            } catch (NullPointerException e) {
                                //
                            }
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        };
    }

    @TargetApi(21)
    private synchronized void scanLeDeviceLollipop(final boolean enable) {
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                lollipopScanner = bluetooth_adapter.getBluetoothLeScanner();
            }
            if (lollipopScanner != null) {
                Log.d(TAG, "Starting scanner 21");
                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        is_scanning = false;
                        if (bluetooth_adapter != null && bluetooth_adapter.isEnabled()) {
                            try {
                                lollipopScanner.stopScan(mScanCallback);
                            } catch (IllegalStateException e) {
                                JoH.static_toast_long(e.toString());
                                UserError.Log.e(TAG, "error stopping scan: " + e.toString());
                            }
                        }
                        invalidateOptionsMenu();
                    }
                }, SCAN_PERIOD);
                ScanSettings settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                is_scanning = true;
                if (bluetooth_adapter != null && bluetooth_adapter.isEnabled()) {
                    lollipopScanner.startScan(null, settings, mScanCallback);
                }
            } else {
                try {
                    scanLeDevice(true);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to scan for ble device", e);
                }
            }
        } else {
            is_scanning = false;
            if (bluetooth_adapter != null && bluetooth_adapter.isEnabled()) {
                lollipopScanner.stopScan(mScanCallback);
            }
        }
        invalidateOptionsMenu();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (scanResult == null || scanResult.getContents() == null) {
            return;
        }
        if (scanResult.getFormatName().equals("CODE_128")) {
            Log.d(TAG, "Setting serial number to: " + scanResult.getContents());
            prefs.edit().putString("share_key", scanResult.getContents()).apply();
            returnToHome();
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(TAG, "Item Clicked");
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null || device.getName() == null) return;
        Toast.makeText(this, R.string.connecting_to_device, Toast.LENGTH_LONG).show();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        synchronized (ActiveBluetoothDevice.table_lock) {
            ActiveBluetoothDevice btDevice = new Select().from(ActiveBluetoothDevice.class)
                    .orderBy("_ID desc")
                    .executeSingle();

            prefs.edit().putString("last_connected_device_address", device.getAddress()).apply();
            Blukon.clearPin();
            if (btDevice == null) {
                ActiveBluetoothDevice newBtDevice = new ActiveBluetoothDevice();
                newBtDevice.name = device.getName();
                newBtDevice.address = device.getAddress();
                newBtDevice.save();
            } else {
                btDevice.name = device.getName();
                btDevice.address = device.getAddress();
                btDevice.save();
            }

            startWatchUpdaterService(this, WatchUpdaterService.ACTION_SYNC_ACTIVEBTDEVICE, TAG);
        }

        // automatically set or unset the option for "Transmiter" device
        boolean using_transmiter = false;
        // Experimental support for rfduino from Tomasz Stachowicz
        // automatically set or unset the option for "RFDuino" device
        boolean using_rfduino = false;

        try {
            if (device.getName().toLowerCase().contains("limitter")
                    && (adverts.containsKey(device.getAddress())
                    && ((new String(adverts.get(device.getAddress()), "UTF-8").contains("eLeR"))
                    || (new String(adverts.get(device.getAddress()), "UTF-8").contains("data"))))||
                    device.getName().toLowerCase().contains("limitterd")) {
                String msg = "Auto-detected transmiter_pl device!";
                Log.e(TAG, msg);
                JoH.static_toast_long(msg);
                using_transmiter = true;
            }

            prefs.edit().putBoolean("use_transmiter_pl_bluetooth", using_transmiter).apply();

            // Experimental support for rfduino from Tomasz Stachowicz
            if (device.getName().toLowerCase().contains("xbridge")
                    && (adverts.containsKey(device.getAddress()) && (new String(adverts.get(device.getAddress()), "UTF-8").contains("rfduino")))) {
                String msg = "Auto-detected rfduino device!";
                Log.e(TAG, msg);
                JoH.static_toast_long(msg);
                using_rfduino = true;
            }
            prefs.edit().putBoolean("use_rfduino_bluetooth", using_rfduino).apply();


            if (device.getName().toLowerCase().contains("dexcom")) {
                if (!CollectionServiceStarter.isBTShare(getApplicationContext())) {
                    prefs.edit().putString("dex_collection_method", "DexcomShare").apply();
                    prefs.edit().putBoolean("calibration_notifications", false).apply();
                }
                if (prefs.getString("share_key", "SM00000000").compareTo("SM00000000") == 0 || prefs.getString("share_key", "SM00000000").length() < 10) {
                    requestSerialNumber(prefs);
                } else returnToHome();

            } else if (device.getName().toLowerCase().contains("bridge")) {
                if (!CollectionServiceStarter.isDexBridgeOrWifiandDexBridge())
                    prefs.edit().putString("dex_collection_method", "DexbridgeWixel").apply();
                if (prefs.getString("dex_txid", "00000").compareTo("00000") == 0 || prefs.getString("dex_txid", "00000").length() < 5) {
                    requestTransmitterId(prefs);
                } else returnToHome();

            } else if (device.getName().toLowerCase().contains("drip")) {
                if (!
                        (CollectionServiceStarter.isBTWixelOrLimiTTer(getApplicationContext())
                                || CollectionServiceStarter.isWifiandBTWixel(getApplicationContext())
                        ) || CollectionServiceStarter.isLimitter()) {
                    prefs.edit().putString("dex_collection_method", "BluetoothWixel").apply();
                }
                returnToHome();
            } else if (device.getName().toLowerCase().contains("limitter")) {
                if (!CollectionServiceStarter.isLimitter()) {
                    prefs.edit().putString("dex_collection_method", "LimiTTer").apply();
                }
                returnToHome();
            } else if (device.getName().toLowerCase().contains("bluereader")) {
                if (!CollectionServiceStarter.isLimitter()) {
                    prefs.edit().putString("dex_collection_method", "LimiTTer").apply();
                }
                returnToHome();
            } else if (device.getName().toLowerCase().contains("miaomiao")) {
                if (!(CollectionServiceStarter.isLimitter() || CollectionServiceStarter.isWifiandBTLibre())) {
                    prefs.edit().putString("dex_collection_method", "LimiTTer").apply();
                }
                returnToHome();
            } else if (device.getName().toLowerCase().contains("sweetreader")) {
                if (!CollectionServiceStarter.isLimitter()) {
                    prefs.edit().putString("dex_collection_method", "LimiTTer").apply();
                }
                returnToHome();
            } else if (device.getName().matches("^BLU[0-9][0-9][0-9][0-9][0-9].*$")) {

                Blukon.doPinDialog(this,
                        new Runnable() {
                            @Override
                            public void run() {
                                if (!CollectionServiceStarter.isLimitter()) {
                                    prefs.edit().putString("dex_collection_method", "LimiTTer").apply();
                                }
                                returnToHome();
                            }
                        });

            } else if (device.getName().matches("MT")) {
                if (Medtrum.saveSerialFromLegacy(adverts.get(device.getAddress()))) {
                    JoH.static_toast_long("Set Medtrum serial number");
                    CollectionServiceStarter.restartCollectionServiceBackground();
                    returnToHome();
                } else {
                    JoH.static_toast_long("Failed to find Medtrum serial number");
                }
            } else {
                returnToHome();
            }

        } catch (UnsupportedEncodingException | NullPointerException e) {
            Log.d(TAG, "Got exception in listitemclick: " + Arrays.toString(e.getStackTrace()));
        }
    }

    public void returnToHome() {
        try {
            if (is_scanning) {
                is_scanning = false;
                bluetooth_adapter.stopLeScan(mLeScanCallback);
            }
        } catch (NullPointerException e) {
            // meh
        }
        Inevitable.task("restart-collector", 2000, () -> CollectionServiceStarter.restartCollectionService(getApplicationContext()));
        final Intent intent = new Intent(this, Home.class);
        startActivity(intent);
        finish();
    }

    private void requestSerialNumber(final SharedPreferences prefs) {
        final Dialog dialog = new Dialog(BluetoothScan.this);
        dialog.setContentView(R.layout.dialog_single_text_field);
        Button saveButton = (Button) dialog.findViewById(R.id.saveButton);
        Button cancelButton = (Button) dialog.findViewById(R.id.cancelButton);
        final EditText serialNumberView = (EditText) dialog.findViewById(R.id.editTextField);
        serialNumberView.setHint("SM00000000");
        ((TextView) dialog.findViewById(R.id.instructionsTextField)).setText("Enter Your Dexcom Receiver Serial Number");

        dialog.findViewById(R.id.scannerButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AndroidBarcode(BluetoothScan.this).scan();
                dialog.dismiss();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(serialNumberView.getText()))
                    prefs.edit().putString("share_key", serialNumberView.getText().toString()).apply();
                dialog.dismiss();
                returnToHome();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void requestTransmitterId(final SharedPreferences prefs) {
        final Dialog dialog = new Dialog(BluetoothScan.this);
        dialog.setContentView(R.layout.dialog_single_text_field);
        Button saveButton = (Button) dialog.findViewById(R.id.saveButton);
        Button cancelButton = (Button) dialog.findViewById(R.id.cancelButton);
        dialog.findViewById(R.id.scannerButton).setVisibility(View.GONE);
        final EditText serialNumberView = (EditText) dialog.findViewById(R.id.editTextField);
        serialNumberView.setHint("00000");
        serialNumberView.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
        ((TextView) dialog.findViewById(R.id.instructionsTextField)).setText("Enter Your Dexcom Transmitter ID");

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(serialNumberView.getText())) {
                    prefs.edit().putString("dex_txid", serialNumberView.getText().toString()).apply();
                }
                dialog.dismiss();
                returnToHome();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = BluetoothScan.this.getLayoutInflater();
        }

        void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                notifyDataSetChanged();
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            final BluetoothDevice device = mLeDevices.get(i);
            if (device != null) {
                String deviceName = device.getName();
                if (deviceName == null) {
                    deviceName = "";
                }
                //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if (Pref.getString("last_connected_device_address", "").equalsIgnoreCase(device.getAddress())) {
                    viewHolder.deviceName.setTextColor(ChartUtils.COLOR_BLUE);
                    viewHolder.deviceAddress.setTextColor(ChartUtils.COLOR_BLUE);
                }
                viewHolder.deviceName.setText(deviceName);
                viewHolder.deviceAddress.setText(device.getAddress());
                if (adverts.containsKey(device.getAddress())) {
                    if (deviceName.equals("MT")) {
                        final String medtrum = getDeviceInfoStringFromLegacy(adverts.get(device.getAddress()));
                        if (medtrum != null) {
                            viewHolder.deviceName.setText(medtrum);
                        }
                    }
                    try {
                        if (Pref.getBooleanDefaultFalse("engineering_mode")) {
                            viewHolder.deviceAddress.append("   " + new String(adverts.get(device.getAddress()), "UTF-8"));
                        }
                    } catch (UnsupportedEncodingException e) {
                        //
                    }
                }
            }
            return view;
        }
    }
}
