package com.eveningoutpost.dexdrip.glucosemeter;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.services.BluetoothGlucoseMeter;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.ListActivityWithMenu;
import com.eveningoutpost.dexdrip.utils.LocationHelper;

import java.util.ArrayList;

import static com.eveningoutpost.dexdrip.services.BluetoothGlucoseMeter.start_forget;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Created by jamorham on 09/12/2016.
 * Scan, connect and manage pairing state of Bluetooth Glucose meters
 * interacts with BluetoothGlucoseMeter service
 */

@TargetApi(18)
public class BTGlucoseMeterActivity extends ListActivityWithMenu {

    private static final String TAG = BTGlucoseMeterActivity.class.getSimpleName();
    private static final String menu_name = "Meter Scan";
    private boolean is_scanning = false;
    private LeDeviceListAdapter mLeDeviceListAdapter;

    private BluetoothAdapter bluetooth_adapter;

    private BluetoothManager bluetooth_manager;
    private BroadcastReceiver serviceDataReceiver;

    private TextView statusText;
    private boolean first_run = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.OldAppTheme); // or null actionbar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btglucose_meter);

        statusText = (TextView) findViewById(R.id.btg_scan_status);
        statusText.setText("Starting up");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            JoH.static_toast_long("The android version of this device is not compatible with Bluetooth Low Energy");
            finish();
        } else {

            bluetooth_manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            bluetooth_adapter = bluetooth_manager.getAdapter();


            if (bluetooth_adapter == null) {
                Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // get bluetooth ready
            check_and_enable_bluetooth();
            LocationHelper.requestLocationForBluetooth(this);

            serviceDataReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    final String action = intent.getAction();
                    UserError.Log.d(TAG, "Got receive:" + action + " :: " + intent.getStringExtra("data"));
                    switch (action) {
                        case BluetoothGlucoseMeter.ACTION_BLUETOOTH_GLUCOSE_METER_SERVICE_UPDATE:
                            statusText.setText(intent.getStringExtra("data"));
                            break;
                        case BluetoothGlucoseMeter.ACTION_BLUETOOTH_GLUCOSE_METER_NEW_SCAN_DEVICE:
                            mLeDeviceListAdapter.addDevice(intent.getStringExtra("data"));
                            break;
                    }
                }
            };

            mLeDeviceListAdapter = new LeDeviceListAdapter();


            setListAdapter(mLeDeviceListAdapter);

            // long click call back
            getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View v,
                                               int position, long id) {

                    final MyBluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                    if (device != null) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(adapterView.getContext());
                        builder.setTitle("Choose Action");
                        builder.setMessage("You can disconnect from this device or forget its pairing here");

                        builder.setNeutralButton("Do Nothing", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        builder.setPositiveButton("Disconnect", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                if (Pref.getStringDefaultBlank("selected_bluetooth_meter_address").equals(device.address)) {
                                    Pref.setString("selected_bluetooth_meter_address", "");
                                    mLeDeviceListAdapter.changed();
                                    JoH.static_toast_long("Disconnected!");
                                    BluetoothGlucoseMeter.start_service(null);
                                } else {
                                    JoH.static_toast_short("Not connected to this device!");
                                }
                            }
                        });

                        builder.setNegativeButton("Forget Pair", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                start_forget(device.address);
                                dialog.dismiss();
                            }
                        });

                        AlertDialog alert = builder.create();
                        alert.show();
                    } else {
                        UserError.Log.wtf(TAG, "Null pointer on list item long click");
                    }

                    return true;
                }
            });
            getListView().setLongClickable(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothGlucoseMeter.ACTION_BLUETOOTH_GLUCOSE_METER_NEW_SCAN_DEVICE);
        intentFilter.addAction(BluetoothGlucoseMeter.ACTION_BLUETOOTH_GLUCOSE_METER_SERVICE_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceDataReceiver, intentFilter);
        if (first_run) {
            BluetoothGlucoseMeter.start_service(null);
            first_run = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (serviceDataReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceDataReceiver);
            } catch (IllegalArgumentException e) {
                UserError.Log.e(TAG, "broadcast receiver not registered", e);
            }
        }
    }

    private void check_and_enable_bluetooth() {
        if (!bluetooth_manager.getAdapter().isEnabled()) {
            if (Pref.getBoolean("automatically_turn_bluetooth_on", true)) {
                JoH.setBluetoothEnabled(getApplicationContext(), true);
                Toast.makeText(this, "Trying to turn Bluetooth on", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Please turn Bluetooth on!", Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                check_and_enable_bluetooth();
                if (JoH.ratelimit("bluetooth-scan-button", 4)) {
                    UserError.Log.d(TAG, "Starting Bluetooth Glucose Meter Service");
                    mLeDeviceListAdapter.clear();
                    BluetoothGlucoseMeter.start_service(null);
                } else {
                    UserError.Log.d(TAG, "Rate limited scan button");
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bluetooth_scan, menu);
        menu.findItem(R.id.menu_refresh).setVisible(false); // not used
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
    public String getMenuName() {
        return menu_name;
    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final MyBluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device != null) {
            if (JoH.ratelimit("bt-meter-item-clicked", 7)) {
                UserError.Log.d(TAG, "Item Clicked: " + device.address);
                BluetoothGlucoseMeter.start_service(device.address);
            }
        } else {
            UserError.Log.wtf(TAG, "Null pointer on list item click");
        }
    }


    // List Adapter

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    static class MyBluetoothDevice {
        String address;
        String name;
        int pairstate;

        MyBluetoothDevice(String data) {
            // parse fixed format data string
            String[] stra = data.split("\\^");
            this.address = stra[0];
            this.pairstate = Integer.parseInt(stra[1]);
            if (stra.length > 2) {
                this.name = stra[2];
            } else {
                this.name = ""; // unnamed
            }
        }
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<MyBluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = BTGlucoseMeterActivity.this.getLayoutInflater();
        }

        synchronized boolean isDupeDevice(MyBluetoothDevice device) {
            if (device == null) return false;
            for (MyBluetoothDevice mbtd : mLeDevices) {
                if (mbtd.address.equals(device.address)) {
                    // update if pairing state changes
                    if (mbtd.pairstate != device.pairstate) {
                        mbtd.pairstate = device.pairstate;
                        notifyDataSetChanged();
                    } else if (!mbtd.name.equals(device.name)) {
                        notifyDataSetChanged();
                    }

                    return true;
                }
            }
            return false;
        }

        synchronized void addDevice(String data) {
            if (data == null) return;
            final MyBluetoothDevice device = new MyBluetoothDevice(data);
            if (!isDupeDevice(device)) {
                mLeDevices.add(device);
                notifyDataSetChanged();
                UserError.Log.d(TAG, "New list device added - data set changed");
            }
        }

        public MyBluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void changed() {
            notifyDataSetChanged();
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
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            MyBluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.name;

            if (Pref.getString("selected_bluetooth_meter_address", "").equals(device.address)) {
                viewHolder.deviceName.setTextColor(Color.parseColor("#ff99dd00"));
            } else {
                viewHolder.deviceName.setTextColor(Color.WHITE);
            }

            boolean is_bonded = device.pairstate == BluetoothDevice.BOND_BONDED;
            if (is_bonded) {
                viewHolder.deviceAddress.setTextColor(Color.YELLOW);
            } else {
                viewHolder.deviceAddress.setTextColor(Color.WHITE);
            }

            viewHolder.deviceName.setText(deviceName);
            viewHolder.deviceAddress.setText(device.address + (is_bonded ? "   " + "Paired" : ""));
            return view;
        }
    }


}
