package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.UtilityModels.DexShareAttributes;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.HM10Attributes;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;


public class ShareTest extends Activity {
    private final static String TAG = ShareTest.class.getSimpleName();
    Button button;
    Button closeButton;
    TextView details;

    private String mDeviceName;
    private String mDeviceAddress;
    private boolean is_connected = false;
    SharedPreferences prefs;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private ForegroundServiceStarter foregroundServiceStarter;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothDevice device;
    int mStartMode;
    private Context mContext = null;

    private static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
    private static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    private static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_test);
        button = (Button) findViewById(R.id.connect);
        closeButton = (Button) findViewById(R.id.closeConnect);
        details = (TextView) findViewById(R.id.connection_details);
        addListenerOnButton();
        addListenerOnCloseButton();
        IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mPairReceiver, intent);
    }

    public void addListenerOnButton() {
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                attemptConnection();
            }
        });
    }

    public void addListenerOnCloseButton() {
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                close();
                details.setText("");
            }
        });
    }

    public void attemptConnection() {
        if (device != null) {
            mConnectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
        }
        Log.w(TAG, "Connection state: " + mConnectionState);
        details.append("Connection state: " + mConnectionState);
        if (mConnectionState == STATE_DISCONNECTED || mConnectionState == STATE_DISCONNECTING) {
            ActiveBluetoothDevice btDevice = new Select().from(ActiveBluetoothDevice.class)
                    .orderBy("_ID desc")
                    .executeSingle();
            if (btDevice != null) {
                mDeviceName = btDevice.name;
                mDeviceAddress = btDevice.address;

                if (mBluetoothManager == null) {
                    mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    if (mBluetoothManager == null) {
                        Log.w(TAG, "Unable to initialize BluetoothManager.");
                    }
                }
                if (mBluetoothManager != null) {
                    mBluetoothAdapter = mBluetoothManager.getAdapter();
                    if (mBluetoothAdapter == null) {
                        Log.w(TAG, "Unable to obtain a BluetoothAdapter.");
                    }
                    is_connected = connect(mDeviceAddress);
                    if (is_connected) {
                        details.append("\nConnection state: connected to device");

                    } else {
                        Log.w(TAG, "Unable to connect to device");
                    }

                } else {
                    Log.w(TAG, "Still no bluetooth Manager");
                }
            } else {
                Log.w(TAG, "No bluetooth device to try to connect to");
            }
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;

                details.append("\nConnection state: connected to GATT server");
                details.append("\nConnection state: Attempting service discovery");
//                gatt.getDevice()
                pokeService(gatt);
                Log.w(TAG, "Connected to GATT server.");
                Log.w(TAG, "Attempting to start service discovery: " +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                Log.w(TAG, "Disconnected from GATT server.");
                details.append("\nConnection state: disconnected from GATT server");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService gattService : mBluetoothGatt.getServices()) {
                    Log.w(TAG, "Service Found");
                    for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                        Log.w(TAG, "Characteristic Found");
                        setCharacteristicNotification(gattCharacteristic, true);
                    }
                }
                Log.w(TAG, "onServicesDiscovered received success: " + status);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                details.append("\nCharacteristic Read!");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //Do Something here!
            details.append("\nCharacteristic Changed");
        }
    };

    public boolean connect(final String address) {
        Log.w(TAG, "CONNECTING TO DEVICE");
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.w(TAG, "Trying to use an existing mBluetoothGatt for connection.");

            details.append("\nTrying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            details.append("\nDevice not found.  Unable to connect.");
            return false;
        }

        boolean bonded = device.createBond();

        details.append("\nConnection state: Bonded - " + bonded);

        boolean didpin = device.setPin("000000".getBytes());

        details.append("\nConnection state: Set Pin - " + didpin);

        boolean pairconf = device.setPairingConfirmation(true);

        details.append("\nConnection state: Set Pin - " + pairconf);

         bonded = device.createBond();

        details.append("\nConnection state: Bonded - " + bonded);

        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
        Log.w(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;


        return true;
    }

    public void pokeService(BluetoothGatt bluetoothGatt) {

        BluetoothGattService cradleService = bluetoothGatt.getService(DexShareAttributes.CradleService);
        BluetoothGattCharacteristic authenticationCharacteristic = cradleService.getCharacteristic(DexShareAttributes.AuthenticationCode);
        details.append("\nCharacteristic found: " + authenticationCharacteristic.toString());

        for( BluetoothGattDescriptor descriptor : authenticationCharacteristic.getDescriptors()) {
            Log.w(TAG, "Descriptor found: " + descriptor.getUuid());
            Log.w(TAG, "Descriptor found: " + descriptor.getValue());
            Log.w(TAG, "Descriptor found: " + descriptor.getPermissions());
            details.append("\nDescriptor found: " + descriptor.getUuid());
            details.append("\nDescriptor found: " + descriptor.getValue());
            details.append("\nDescriptor found: " + descriptor.getPermissions());
        }

//            authenticationCharacteristic.setValue();

    }
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        disconnect();
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        mConnectionState = STATE_DISCONNECTED;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        details.append("\nReading Characteristic...");
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        details.append("\nSetting Characteristic Notification...");
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        Log.w(TAG, "UUID FOUND: " + characteristic.getUuid());
        details.append("\nCharacteristic: " + characteristic.getUuid());
        if (DexShareAttributes.AuthenticationCode.equals(characteristic.getUuid())) {

            Log.w(TAG, "UUID MATCH FOUND!!! " + characteristic.getUuid());

            details.append("\nAuthentication Characteristic Found");

            for( BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                Log.w(TAG, "Descriptor found: " + descriptor.getUuid());
                Log.w(TAG, "Descriptor found: " + descriptor.getValue());
                Log.w(TAG, "Descriptor found: " + descriptor.getPermissions());
                details.append("\nDescriptor found: " + descriptor.getUuid());
                details.append("\nDescriptor found: " + descriptor.getValue());
                details.append("\nDescriptor found: " + descriptor.getPermissions());

            }
//                    UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }
    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "CALLBACK RECIEVED Bonded");
                    Toast.makeText(getApplicationContext(), "Paired", Toast.LENGTH_LONG).show();
                } else if (state == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "CALLBACK RECIEVED: Not Bonded");
                    Toast.makeText(getApplicationContext(), "unPaired", Toast.LENGTH_LONG).show();
                }

            }
        }
    };

}
