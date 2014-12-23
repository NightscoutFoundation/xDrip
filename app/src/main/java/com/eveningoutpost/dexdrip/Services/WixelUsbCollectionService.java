package com.eveningoutpost.dexdrip.Services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Sensor;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;

/**
 * Created by stephenblack on 12/22/14.
 */
public class WixelUsbCollectionService extends Service {
    private final static String TAG = WixelUsbCollectionService.class.getSimpleName();
    boolean stop = false;
    private static UsbSerialDriver sDriver = null;
    private SerialInputOutputManager mSerialIoManager;
    int mStartMode;

    @Override
    public void onCreate() {
        Log.w(TAG, "STARTING SERVICE");
    }

    @Override
    public IBinder onBind(Intent intent) { throw new UnsupportedOperationException("Not yet implemented"); }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PendingIntent pending = PendingIntent.getService(this, 0, new Intent(this, WixelUsbCollectionService.class), 0);
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pending);

        startUsbListener();
        return mStartMode;
    }

    @Override
    public void onDestroy() {
        AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarm.set(
                alarm.ELAPSED_REALTIME_WAKEUP,
                System.currentTimeMillis() + (1000 * 60),
                PendingIntent.getService(this, 0, new Intent(this, WixelUsbCollectionService.class), 0)
        );
    }

    private void startUsbListener() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbSerialDriver sDriver = UsbSerialProber.acquire(manager);

        if (sDriver != null) {
            try {
                sDriver.open();
                sDriver.setBaudRate(9600);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                try {
                    sDriver.close();
                } catch (IOException e2) {

                    Log.e(TAG, "Cant even close device, cool!");
                }
                sDriver = null;
                return;
            }
        }
        onDeviceStateChange();
    }

//TODO: refactor this as it now exists in two places...
    public void setSerialDataToTransmitterRawData(byte[] buffer, int len) {
        TransmitterData transmitterData = TransmitterData.create(buffer, len);
        if (transmitterData != null) {
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                BgReading bgReading = BgReading.create(transmitterData.raw_data, this);
                sensor.latest_battery_level = transmitterData.sensor_battery_level;
                sensor.save();
            } else {
                Log.w(TAG, "No Active Sensor, Data only stored in Transmitter Data");
            }
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sDriver != null) {
            mSerialIoManager = new SerialInputOutputManager(sDriver, mListener);
            mSerialIoManager.run();
        }
    }

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {
                @Override
                public void onRunError(Exception e) { Log.d(TAG, "ERROR, Listener stopped"); }

                @Override
                public void onNewData(final byte[] data) { setSerialDataToTransmitterRawData(data, data.length); }
            };

}
