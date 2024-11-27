package com.eveningoutpost.dexdrip.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.SpeechUtil;
import com.eveningoutpost.dexdrip.utilitymodels.VehicleMode;
import com.eveningoutpost.dexdrip.ui.activities.SelectAudioDevice;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.utilitymodels.SpeechUtil.TWICE_DELIMITER;

/**
 * jamorham
 *
 * Track connection changes with bluetooth headsets
 * Activate vehicle mode if we have been configured to do so
 */

public class HeadsetStateReceiver extends BroadcastReceiver {

    private static final String TAG = "BluetoothHeadset";
    private static final String PREF_LAST_CONNECTED_MAC = "bluetooth-last-audio-connected-mac";
    private static final String PREF_LAST_CONNECTED_NAME = "bluetooth-last-audio-connected-name";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {

            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null && device.getAddress() != null) {
                final int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
                final int previousState = intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, -1);
                final String deviceInfo = device.getName() + "\n" + device.getAddress() + " " + (device.getBluetoothClass() != null ? device.getBluetoothClass() : "<unknown class>");
                // UserError.Log.uel(TAG, "Bluetooth audio connection state change: from " + previousState + " to " + state + " " + device.getAddress() + " " + device.getName());
                if (state == BluetoothProfile.STATE_CONNECTED && previousState != BluetoothProfile.STATE_CONNECTED) {
                    PersistentStore.setString(PREF_LAST_CONNECTED_MAC, device.getAddress());
                    PersistentStore.setString(PREF_LAST_CONNECTED_NAME, device.getName());
                    UserError.Log.uel(TAG, "Bluetooth Audio connected: " + deviceInfo);
                    processDevice(device.getAddress(), true);
                } else if (state == BluetoothProfile.STATE_DISCONNECTED && previousState != BluetoothProfile.STATE_DISCONNECTED) {
                    UserError.Log.uel(TAG, "Bluetooth Audio disconnected: " + deviceInfo);
                    processDevice(device.getAddress(), false);
                }
            } else {
                UserError.Log.d(TAG, "Device was null in intent!");
            }
        } else if (BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED.equals(action)) {
            // TODO this probably just for debugging could remove later
            final int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
            UserError.Log.e(TAG, "audio state changed: " + state);

        } else if (BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT.equals(action)) {
            // TODO this will probably never fire due to manifest intent filter - check if there is any use for it
            UserError.Log.e(TAG, "Vendor specific command event received");
        }
    }

    private static final long NOISE_DELAY = 10000; // allow time for bt volume control

    private static void processDevice(final String mac, final boolean connected) {
        if (VehicleMode.isEnabled() && VehicleMode.viaCarAudio() && SelectAudioDevice.getAudioMac().equals(mac)) {
            VehicleMode.setVehicleModeActive(connected);
            UserError.Log.ueh(TAG, "Vehicle mode: " + (connected ? "Enabled" : "Disabled"));
            if (connected) {
                Inevitable.task("xdrip-vehicle-mode", NOISE_DELAY, HeadsetStateReceiver::audioNotification);
            }
            Home.staticRefreshBGChartsOnIdle();
        }
    }

    private static void audioNotification() {
        if (VehicleMode.isVehicleModeActive()) {
            if (VehicleMode.shouldUseSpeech()) {
                SpeechUtil.say(" X Drip " + TWICE_DELIMITER, 500);
            } else if (VehicleMode.shouldPlaySound()) {
                JoH.playResourceAudio(R.raw.labbed_musical_chime);
            }
        }
    }


    public static String getLastConnectedMac() {
        return PersistentStore.getString(PREF_LAST_CONNECTED_MAC);
    }


    public static String getLastConnectedName() {
        return PersistentStore.getString(PREF_LAST_CONNECTED_NAME);
    }


    public static void reprocessConnectionIfAlreadyConnected(final String mac) {
        VehicleMode.setVehicleModeActive(false);
        areWeConnectedToMac(mac, () -> processDevice(mac, true));
    }

    // if connected run the runnable, proxy service object may mean async running
    private static void areWeConnectedToMac(final String mac, Runnable runnable) {

        final int state = BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(BluetoothProfile.HEADSET);
        if (state != BluetoothProfile.STATE_CONNECTED) {
            return;
        }


        final BluetoothProfile.ServiceListener serviceListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceDisconnected(int profile) {

            }

            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                for (BluetoothDevice device : proxy.getConnectedDevices()) {
                    if (device.getAddress().equals(mac)) {
                        final boolean isConnected = proxy.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED;
                        UserError.Log.uel(TAG, "Found: " + device.getName() + " " + device.getAddress() + " " + (isConnected ? "Connected" : "Not connected"));
                        if (isConnected) {
                            if (runnable != null) runnable.run();
                        }
                        break;
                    }
                }
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(profile, proxy);
            }
        };

        BluetoothAdapter.getDefaultAdapter().getProfileProxy(xdrip.getAppContext(), serviceListener, BluetoothProfile.HEADSET);
    }
}

