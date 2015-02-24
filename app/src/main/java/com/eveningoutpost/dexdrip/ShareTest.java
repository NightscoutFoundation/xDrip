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
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.PacketBuilder;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.ReadData;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.ReadDataShare;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.EGVRecord;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.UtilityModels.DexShareAttributes;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.HM10Attributes;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import rx.Observable;
import rx.functions.Action1;


public class ShareTest extends Activity {
    private final static String TAG = ShareTest.class.getSimpleName();
    Button button;
    Button closeButton;
    Button readButton;
    Button bondButton;
    TextView details;

    private String mDeviceName;
    private String mDeviceAddress;
    private boolean is_connected = false;
    private boolean reconnecting = false;
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

    private BluetoothGattService mShareService;
    private BluetoothGattCharacteristic mAuthenticationCharacteristic;
    private BluetoothGattCharacteristic mSendDataCharacteristic;
    private BluetoothGattCharacteristic mReceiveDataCharacteristic;
    private BluetoothGattCharacteristic mHeartBeatCharacteristic;
    private BluetoothGattCharacteristic mCommandCharacteristic;
    private BluetoothGattCharacteristic mResponseCharacteristic;

    //Gatt Tasks
    public final int GATT_NOTHING = 0;
    public final int GATT_SETUP = 1;
    public final int GATT_WRITING_COMMANDS = 2;
    public final int GATT_READING_RESPONSE = 3;
    public int successfulWrites;

    //RXJAVA FUN
    Action1<byte[]> mDataResponseListener;

    public ReadDataShare mReadDataShare;

    public int currentGattTask;
    public int step;

    public List<byte[]> writePackets;
    public int recordType;

    private String mSerialNumber = "SM41878769"; //TODO: Get this out of shared Preferences

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_test);
        button = (Button) findViewById(R.id.connect);
        closeButton = (Button) findViewById(R.id.closeConnect);
        bondButton = (Button) findViewById(R.id.bond);
        readButton = (Button) findViewById(R.id.read);
        details = (TextView) findViewById(R.id.connection_details);
        addListenerOnButton();
        addListenerOnCloseButton();
        IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mPairReceiver, intent);
    }

    @Override
    public void onStop() {
        close();
        Log.w(TAG, "CLOSING CONNECTION");
    }

    public void addListenerOnButton() {
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                attemptConnection();
            }
        });
        readButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                attemptRead();
            }
        });
        bondButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bond(mBluetoothGatt);
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

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt = gatt;
                mConnectionState = STATE_CONNECTED;
                Log.w(TAG, "Connected to GATT server.");
                Log.w(TAG, "Connection state: Bonded - " + device.getBondState());

                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    currentGattTask = GATT_SETUP;
                    mBluetoothGatt.discoverServices();

                } else {
                    device.setPin("000000".getBytes());
                    device.createBond();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                Log.w(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Services Discovered: " + status);
                authenticateConnection(gatt);

            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Characteristic Read");
                byte[] value = characteristic.getValue();
                if(value != null) {
                    Log.w(TAG, "VALUE" + value);
                } else {
                    Log.w(TAG, "Characteristic was null");
                }
                nextGattStep();
            } else {
                Log.w(TAG, "Characteristic failed to read");
            }
        }
        //
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.w(TAG, "Characteristic changed");
            UUID charUuid = characteristic.getUuid();
            Log.w(TAG, "Characteristic Update Received: " + charUuid);
            if(charUuid.compareTo(mResponseCharacteristic.getUuid()) == 0) {
                Log.w(TAG, "mResponseCharacteristic Update");
            }
            if(charUuid.compareTo(mCommandCharacteristic.getUuid()) == 0) {
                Log.w(TAG, "mCommandCharacteristic Update");
            }
            if(charUuid.compareTo(mHeartBeatCharacteristic.getUuid()) == 0) {
                Log.w(TAG, "mHeartBeatCharacteristic Update");
            }
            if(charUuid.compareTo(mReceiveDataCharacteristic.getUuid()) == 0) {
                Log.w(TAG, "mReceiveDataCharacteristic Update");
                byte[] value = characteristic.getValue();
                if(value != null) {

                    Log.w(TAG, "Characteristic: " + value);
                    Log.w(TAG, "Characteristic: " + value.toString());
                    Log.w(TAG, "Characteristic getstring: " + characteristic.getStringValue(0));
                    Log.w(TAG, "SUBSCRIBED TO RESPONSE LISTENER");

                    Observable.just(characteristic.getValue()).subscribe(mDataResponseListener);
                } else {
                    Log.w(TAG, "Characteristic was null");
                }
            }
            Log.w(TAG, "NEW VALUE: " + characteristic.getValue().toString());
//            nextGattStep();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.w(TAG, "Wrote a discriptor, status: " + status);
            if(step == 2 && currentGattTask == GATT_SETUP) {
                setListeners(2);
            } else if(step == 3) {
                setListeners(3);
            } else if(step == 4) {
                setListeners(4);
            } else if(step == 5) {
                Log.w(TAG, "Done setting Listeners");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.w(TAG, "Wrote a characteristic: " + status);
            nextGattStep();
        }
    };

    public void attemptConnection() {

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (device != null) {
            details.append("\nConnection state: " + " Device is not null");

            mConnectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
        }

        Log.w(TAG, "Connection state: " + mConnectionState);
        details.append("\nConnection state: " + mConnectionState);
        if (mConnectionState == STATE_DISCONNECTED || mConnectionState == STATE_DISCONNECTING) {
            ActiveBluetoothDevice btDevice = new Select().from(ActiveBluetoothDevice.class)
                    .orderBy("_ID desc")
                    .executeSingle();

            if (btDevice != null) {
                details.append("\nBT Device: " + btDevice.name);
                mDeviceName = btDevice.name;
                mDeviceAddress = btDevice.address;

                mBluetoothAdapter = mBluetoothManager.getAdapter();
                boolean newConnection = true;
//                for(BluetoothDevice device1 : mBluetoothAdapter.getBondedDevices()) {
//                    if(device1.getAddress().compareTo(btDevice.address)==0) {
//                        details.append("\nUsing Bonded Device");
//                        reconnecting = true;
//                        device = device1;
//                        device.setPin("000000".getBytes());
//                        mBluetoothGatt = device.connectGatt(getApplicationContext(), true, mGattCallback);
//                        newConnection = false;
//                    }
//                }
                if(newConnection) {
                    is_connected = connect(mDeviceAddress);
                    details.append("\nConnecting...: ");
                }
            }
        }
    }

    public void attemptRead() {
        ReadDataShare readData = new ReadDataShare(this);
        Action1<EGVRecord[]> evgRecordListener = new Action1<EGVRecord[]>() {
            @Override
            public void call(EGVRecord[] s) {
                Log.d(TAG, "Made the full round trip, got " + s.length + " EVG Records");
            }
        };
        readData.getRecentEGVs(evgRecordListener);
    }



    public void bond(BluetoothGatt gatt) {
        reconnecting = true;
        attemptConnection();
//        if(device != null) {
//            Log.w(TAG, "Device is not null");
//            device.setPin("000000".getBytes());
//            mBluetoothGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
//        }

    }

    public boolean connect(final String address) {

        details.append("\nConnecting to device");
        Log.w(TAG, "CONNECTING TO DEVICE");
        if (mBluetoothAdapter == null || address == null) {
            details.append("\nBT adapter is null");
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            details.append("\nTrying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        } else {
            device = mBluetoothAdapter.getRemoteDevice(address);
            device.setPin("000000".getBytes());
            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                details.append("\nDevice not found.  Unable to connect.");
                return false;
            }
            mBluetoothGatt = device.connectGatt(getApplicationContext(), true, mGattCallback);
            Log.w(TAG, "Trying to create a new connection.");
            details.append("\nTrying to create a new connection to device");
//            if(mBluetoothGatt != null) {
//                authenticateConnection(mBluetoothGatt);
//                bond(mBluetoothGatt);
//                mBluetoothGatt.discoverServices();
//            }
            mConnectionState = STATE_CONNECTING;
            return true;
        }
    }

    public void authenticateConnection(BluetoothGatt bluetoothGatt) {
        if(bluetoothGatt != null) {
            mBluetoothGatt = bluetoothGatt;
//            mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
            mShareService = mBluetoothGatt.getService(DexShareAttributes.CradleService);
            if (mShareService != null) {
                mAuthenticationCharacteristic = mShareService.getCharacteristic(DexShareAttributes.AuthenticationCode);
                if(mAuthenticationCharacteristic != null) {
                    Log.w(TAG, "Auth Characteristic found: " + mAuthenticationCharacteristic.toString());
                    mAuthenticationCharacteristic.setValue((mSerialNumber + "000000").getBytes(StandardCharsets.US_ASCII));
                    currentGattTask = GATT_SETUP;
                    step = 1;
                    bluetoothGatt.writeCharacteristic(mAuthenticationCharacteristic);
                } else {
                    Log.w(TAG, "Authentication Characteristic IS NULL");
                }
            } else {
                Log.w(TAG, "CRADLE SERVICE IS NULL");
            }
        } else {
            Log.w(TAG, "GATT IS NULL");
        }
    }

    public void assignCharacteristics() {
        mSendDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageReceiver);
        mReceiveDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageResponse);
        mHeartBeatCharacteristic = mShareService.getCharacteristic(DexShareAttributes.HeartBeat);
        mCommandCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Command);
        mResponseCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Response);
    }

    public void setListeners(int listener_number) {

        Log.w(TAG, "Setting Listener: #" + listener_number);
        if(listener_number == 1) {
//            step = 2;
//            setCharacteristicNotification(mHeartBeatCharacteristic);
//        } else if(listener_number == 2) {
            step = 3;
            setCharacteristicIndication(mReceiveDataCharacteristic);
        } else if(listener_number == 3) {
//            step = 4;
            setCharacteristicIndication(mResponseCharacteristic);
//        } else if(listener_number == 4) {
            step = 5;
//            setCharacteristicIndication(mCommandCharacteristic);
        }
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
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic){ setCharacteristicNotification(characteristic, true);}
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        Log.w(TAG, "Characteristic setting notification");
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        Log.w(TAG, "UUID FOUND: " + characteristic.getUuid());
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
        Log.w(TAG, "Descriptor found: " + descriptor.getUuid());
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic){ setCharacteristicIndication(characteristic, true);}
    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enabled) {
        Log.w(TAG, "Characteristic setting notification");
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        Log.w(TAG, "UUID FOUND: " + characteristic.getUuid());
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
        Log.w(TAG, "Descriptor found: " + descriptor.getUuid());
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "CALLBACK RECIEVED Bonded");
                    Toast.makeText(getApplicationContext(), "Bonded", Toast.LENGTH_LONG).show();

//                    authenticateConnection(mBluetoothGatt);
                    currentGattTask = GATT_SETUP;
                    mBluetoothGatt.discoverServices();
//                    authenticateConnection(mBluetoothGatt);
                } else if (state == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "CALLBACK RECIEVED: Not Bonded");
                    Toast.makeText(getApplicationContext(), "unBonded", Toast.LENGTH_LONG).show();
                } else if (state == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "CALLBACK RECIEVED: Trying to bond");
                    Toast.makeText(getApplicationContext(), "trying to bond", Toast.LENGTH_LONG).show();
                }

            }
        }
    };

    ///READ AND WRITE JOBS

    public void writeCommand(List<byte[]> packets, int aRecordType, Action1<byte[]> dataResponseListener) {
        mDataResponseListener = dataResponseListener;
        successfulWrites = 0;
        writePackets = packets;
        recordType = aRecordType;
        step = 0;
        currentGattTask = GATT_WRITING_COMMANDS;
        gattWritingStep();
    }

    public void writeSuccessAck(Action1<byte[]> dataResponseListener) {
        mDataResponseListener = dataResponseListener;
        byte[] byteA = new byte[1];
        byteA[0] = (byte) 1;
        mCommandCharacteristic.setValue(byteA);
        mBluetoothGatt.writeCharacteristic(mCommandCharacteristic);
    }

    //CALLBACKS
    private void nextGattStep() {
        Log.d(TAG, "Next Gatt Step");
        step++;
        switch (currentGattTask) {
            case GATT_NOTHING:
                Log.d(TAG, "Next NOTHING: " + step);
                break;
            case GATT_SETUP:
                Log.d(TAG, "Next GATT SETUP: " + step);
                gattSetupStep();
                break;
            case GATT_WRITING_COMMANDS:
                Log.d(TAG, "Next GATT WRITING: " + step);
                gattWritingStep();
                break;
            case GATT_READING_RESPONSE:
                Log.d(TAG, "Next GATT READING: " + step);
                gattReadingStep();
                break;
        }
    }

    public void clearGattTask() {
        currentGattTask = GATT_NOTHING;
        step = 0;
    }

    private void gattSetupStep() {
        step = 1;
        assignCharacteristics();
        setListeners(1);
    }

    private void gattWritingStep() {
        Log.d(TAG, "Writing command to the Gatt, step: " + step);
        int index = step;
        if (index <= (writePackets.size() - 1)) {
            Log.d(TAG, "Writing: " + writePackets.get(index) + " index: " + index);
            mSendDataCharacteristic.setValue(writePackets.get(index));
            mBluetoothGatt.writeCharacteristic(mSendDataCharacteristic);
        } else {
            clearGattTask();
//            gattReadingStep();
        }
    }

    private void gattReadingStep() {
        currentGattTask = GATT_READING_RESPONSE;

    }

}
