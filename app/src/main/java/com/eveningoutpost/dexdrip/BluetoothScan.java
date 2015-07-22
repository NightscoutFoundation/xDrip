package com.eveningoutpost.dexdrip;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utils.ListActivityWithMenu;

import java.util.ArrayList;

import lecho.lib.hellocharts.util.Utils;

@TargetApi(android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothScan extends ListActivityWithMenu {
    public static String menu_name = "Scan for BT";

    private final static String TAG = BluetoothScan.class.getSimpleName();
    private static final long SCAN_PERIOD = 10000;
    private boolean is_scanning;
    private boolean has_bluetooth;

    private Handler mHandler;
    private LeDeviceListAdapter mLeDeviceListAdapter;

    private ArrayList<BluetoothDevice> found_devices;
    private BluetoothAdapter bluetooth_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_scan);

        ListView lv = (ListView)findViewById(android.R.id.list);

        final BluetoothManager bluetooth_manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        bluetooth_adapter = bluetooth_manager.getAdapter();
        mHandler = new Handler();

        if (bluetooth_adapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
            has_bluetooth = false;
            finish();
            return;
        } else {
            has_bluetooth = true;
        }
        if(bluetooth_manager == null) {
            Toast.makeText(this, "This device does not seem to support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if(!bluetooth_manager.getAdapter().isEnabled()) {
                Toast.makeText(this, "Bluetooth is turned off on this device currently", Toast.LENGTH_LONG).show();
            } else {
                if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){
                    Toast.makeText(this, "The android version of this device is not compatible with Bluetooth Low Energy", Toast.LENGTH_LONG).show();
                }
            }
        }
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
        return menu_name;
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
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                scanLeDevice(true);
                BluetoothManager bluetooth_manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                Toast.makeText(this, "Scanning", Toast.LENGTH_LONG).show();
                if(bluetooth_manager == null) {
                    Toast.makeText(this, "This device does not seem to support bluetooth", Toast.LENGTH_LONG).show();
                } else {
                    if(!bluetooth_manager.getAdapter().isEnabled()) {
                        Toast.makeText(this, "Bluetooth is turned off on this device currently", Toast.LENGTH_LONG).show();
                    } else {
                        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){
                            Toast.makeText(this, "The android version of this device is not compatible with Bluetooth Low Energy", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                return true;
//            case R.id.menu_stop:
//                Intent tableIntent = new Intent(this, RawDataTable.class);
//                startActivity(tableIntent);
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    is_scanning = false;
                    bluetooth_adapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            is_scanning = true;
            bluetooth_adapter.startLeScan(mLeScanCallback);
        } else {
            is_scanning = false;
            bluetooth_adapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(TAG, "Item Clicked");
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        Toast.makeText(this, R.string.connecting_to_device, Toast.LENGTH_LONG).show();

        ActiveBluetoothDevice btDevice = new Select().from(ActiveBluetoothDevice.class)
                .orderBy("_ID desc")
                .executeSingle();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.edit().putString("last_connected_device_address", device.getAddress()).apply();
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
        if(device.getName().toLowerCase().contains("dexcom")) {
            if(!CollectionServiceStarter.isBTShare(getApplicationContext())) {
                prefs.edit().putString("dex_collection_method", "DexcomShare").apply();
                prefs.edit().putBoolean("calibration_notifications", false).apply();
            }
            if(prefs.getString("share_key", "SM00000000").compareTo("SM00000000") == 0 || prefs.getString("share_key", "SM00000000").length() < 10) {
                requestSerialNumber(prefs);
            } else returnToHome();

        } else if(device.getName().toLowerCase().contains("bridge")) {
            if(!CollectionServiceStarter.isDexbridgeWixel(getApplicationContext()))
                prefs.edit().putString("dex_collection_method", "DexbridgeWixel").apply();
            if(prefs.getString("dex_txid", "00000").compareTo("00000") == 0 || prefs.getString("dex_txid", "00000").length() < 5) {
                requestTransmitterId(prefs);
            } else returnToHome();

        } else if(device.getName().toLowerCase().contains("drip")) {
            if (!CollectionServiceStarter.isBTWixel(getApplicationContext()))
                prefs.edit().putString("dex_collection_method", "BluetoothWixel").apply();
            returnToHome();
        } else {
            returnToHome();
        }
    }

    public void returnToHome() {
        if (is_scanning) {
            bluetooth_adapter.stopLeScan(mLeScanCallback);
            is_scanning = false;
        }
        Intent intent = new Intent(this, Home.class);
        CollectionServiceStarter.newStart(getApplicationContext());
        startActivity(intent);
        finish();
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = BluetoothScan.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
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


            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if(prefs.getString("last_connected_device_address", "").compareTo(device.getAddress()) == 0) {
                viewHolder.deviceName.setTextColor(Utils.COLOR_BLUE);
                viewHolder.deviceAddress.setTextColor(Utils.COLOR_BLUE);
            }
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();

                        }
                    });
                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private void requestSerialNumber(final SharedPreferences prefs) {
        final Dialog dialog = new Dialog(BluetoothScan.this);
        dialog.setContentView(R.layout.dialog_single_text_field);
        Button saveButton = (Button) dialog.findViewById(R.id.saveButton);
        Button cancelButton = (Button) dialog.findViewById(R.id.cancelButton);
        final EditText serialNumberView = (EditText) dialog.findViewById(R.id.editTextField);
        serialNumberView.setHint("SM00000000");
        ((TextView) dialog.findViewById(R.id.instructionsTextField)).setText("Enter Your Dexcom Receiver Serial Number");

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!TextUtils.isEmpty(serialNumberView.getText()))
                    prefs.edit().putString("share_key", serialNumberView.getText().toString()).apply();
                dialog.dismiss();
                returnToHome();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                return;
            }
        });
        dialog.show();
    }

    private void requestTransmitterId(final SharedPreferences prefs) {
        final Dialog dialog = new Dialog(BluetoothScan.this);
        dialog.setContentView(R.layout.dialog_single_text_field);
        Button saveButton = (Button) dialog.findViewById(R.id.saveButton);
        Button cancelButton = (Button) dialog.findViewById(R.id.cancelButton);
        final EditText serialNumberView = (EditText) dialog.findViewById(R.id.editTextField);
        serialNumberView.setHint("00000");
        ((TextView) dialog.findViewById(R.id.instructionsTextField)).setText("Enter Your Dexcom Transmitter ID");

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!TextUtils.isEmpty(serialNumberView.getText())) {
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

}
