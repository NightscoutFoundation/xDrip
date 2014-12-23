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

import java.io.IOException;
import java.util.List;

/**
 * Created by stephenblack on 12/22/14.
 */
public class WixelUsbCollectionService extends Service {
    private final static String TAG = WixelUsbCollectionService.class.getSimpleName();
    boolean stop = false;
    int mStartMode;


    @Override
    public void onCreate() {
        Log.w(TAG, "STARTING SERVICE");
    }

    @Override
    public IBinder onBind(Intent intent) { throw new UnsupportedOperationException("Not yet implemented"); }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

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

// Find the first available driver.
        UsbSerialDriver driver = UsbSerialProber.acquire(manager);

        if (driver != null) {
            try {
                driver.open();
                try {
                    driver.setBaudRate(9600);
                    byte buffer[] = new byte[1000];

                    while(!stop) {
                        try
                        {
                            int size = driver.read(buffer, 30000);
                            if (size > 0) {
                                buffer[size] = 0;
                                setSerialDataToTransmitterRawData(buffer, size);
                            }
                        }
                        catch (IOException e)
                        {
                            stop = true;
                        }
                    }
                    int numBytesRead = driver.read(buffer, 1000);
                    Log.d(TAG, "Read " + numBytesRead + " bytes.");
                } catch (IOException e) {
                    // Deal with error.
                } finally {
                    driver.close();
                }
            } catch (IOException e){
            //TODO: Deal with an error opening here!
            }
        }
    }

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
}
