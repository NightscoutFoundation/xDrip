package com.eveningoutpost.dexdrip.Models;

import android.content.Context;
import android.content.SharedPreferences;

import com.eveningoutpost.dexdrip.Home;

import java.nio.ByteBuffer;
import java.util.Date;

import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * Created by MasterPlexus on 11.12.2017.
 */

public class blueReader {
    private static final String TAG = "blueReader";
    private static SharedPreferences prefs;
    public static TransmitterData last_transmitter_Data;

    public static boolean isblueReader() {
        ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        return activeBluetoothDevice.name.contentEquals("blueReader");
    }

    public static boolean isblueReaderPacket(byte[] buffer) {
        return !((buffer == null) || (new String(buffer).startsWith("IDR") ||
                                    new String(buffer).startsWith("TRANS_FAILED") ||
                                    new String(buffer).startsWith("HYBERNATE SUCCESS") ||
                                    new String(buffer).startsWith("not ready for") ||
                                    new String(buffer).startsWith("NFC_DISABLED") )
        );
    }

    public static byte[] decodeblueReaderPacket(byte[] buffer, int len) {
        int cmdFound = 0;
        long timestamp = new Date().getTime();
        String bufferstring;
        //Log.w(TAG, "Packet: " + bufferstring);
        if (buffer == null) {
            Log.e(TAG, "null buffer passed to decodeblueReaderPacket");
            return null;
        } else {
            bufferstring=new String(buffer);
        }
        if (bufferstring.startsWith("not ready for") ) { //delete the trans_failed, because its normal only if the bluereader could not read the sensor.
            Log.e(TAG, "Found blueReader in a ugly State, send hibernate to reset! If this does not help in the next 5 Minutes, then turn the bluereader manually off and on!");
            Home.toaststatic("Found blueReader in a ugly State, send hibernate to reset!");
            return new byte[]{0x68}; //send hard hibernate, because blueReader is in a ugly state
        } else if (bufferstring.startsWith("IDR")){
            Log.i(TAG, bufferstring);
            Home.setPreferencesString("blueReaderFirmware", bufferstring );
            return null;
        } else if (bufferstring.startsWith("WAKE")) {
            Log.d (TAG, "blueReader was set to wakeup-mode manually...");
            return null;
        } else if (bufferstring.startsWith("ECHO")) {
            Log.d (TAG, "blueReader was set to Echo-Mode manually...");
            return null;
        } else if (bufferstring.startsWith("NFC READY")) {
            Log.d (TAG, "blueReader notice that NFC is active...");
            return null;
        } else if (bufferstring.startsWith("NFC_DISABLED")) {
            Log.d (TAG, "blueReader notice that NFC is now hibernated...");
            return null;
        } else if (bufferstring.startsWith("HYBERNATE SUCCESS")) {
            Log.i (TAG, "blueReader notice that NFC is now really hibernated...");
            return null;
        } else if (bufferstring.startsWith("-r 0:")) {
            Log.i (TAG, "blueReader sends an unknown reaktion: '" + bufferstring + "'");
            return null;
        } else if (bufferstring.startsWith("TRANS_FAILED")) {
            Log.w (TAG, "Attention: check position of blueReader on the sensor, as it was not able to read!");
            Home.toaststatic("check position of blueReader on the sensor, as it was not able to read!");
            return null;
        } else if (bufferstring.startsWith("battery: ")) {
            if (BgReading.last() == null || BgReading.last().timestamp + (4 * 60 * 1000) < System.currentTimeMillis()) {
                return new byte[]{0x6C};
            }
        } else {
            processNewTransmitterData(TransmitterData.create(buffer, len, timestamp), timestamp);
        }
        /* // turn off the automatic power off, as it looks like not to help really for the ugly mode
        if ( Home.getPreferencesInt("bridge_battery", 100) <= 0 ) {
            Log.e(TAG, "Send blueReader power-off, due to low Battery!");
            Home.toaststatic("Send blueReader Power-off, due to low Battery!");
            //return new byte[]{0x6B}; //send powerdown to bluereader (k)
        }
        */
        return null;
    }

    private static synchronized void processNewTransmitterData(TransmitterData transmitterData, long timestamp) {
        if (transmitterData == null) {
            return;
        }

        final Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.i(TAG, "setSerialDataToTransmitterRawData: No Active Sensor, Data only stored in Transmitter Data");
            return;
        }

        double blueReaderDays =0;
        if (transmitterData.sensor_battery_level > Home.getPreferencesInt("blueReader_Full_Battery", 4000)) {
            Home.setPreferencesInt("blueReader_Full_Battery", transmitterData.sensor_battery_level);
            Log.i(TAG, "blueReader_Full_Battery set to: " + transmitterData.sensor_battery_level) ;
        }
        Home.setPreferencesInt("bridge_battery", ((transmitterData.sensor_battery_level - 3300) * 100 / ( Home.getPreferencesInt("blueReader_Full_Battery", 4000)-3300)));
        sensor.latest_battery_level = ((transmitterData.sensor_battery_level - 3300) * 100 / (Home.getPreferencesInt("blueReader_Full_Battery", 4000)-3300));
        blueReaderDays = 6.129200670865791d / (1d + Math.pow(((double)transmitterData.sensor_battery_level/3763.700630306379d),(-61.04241888028577d)));
        if (transmitterData.sensor_battery_level < 3600) {
            blueReaderDays=blueReaderDays + 0.1d;
        }
        blueReaderDays = ((Math.round((blueReaderDays)*10d)/10d));

        Home.setPreferencesString("bridge_battery_days", String.valueOf(blueReaderDays));
        sensor.save();

        DexCollectionService.last_transmitter_Data = transmitterData;
        Log.d(TAG, "BgReading.create: new BG reading at " + timestamp + " with a timestamp of " + transmitterData.timestamp);
        BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, xdrip.getAppContext(), transmitterData.timestamp);
    }


    public static ByteBuffer initialize() {
        Log.i(TAG, "initialize!");
        Home.setPreferencesInt("bridge_battery", 0); //force battery to no-value before first reading

        //command to get Firmware
        ByteBuffer ackMessage = ByteBuffer.allocate(3);
        ackMessage.put(0, (byte) 0x49);
        ackMessage.put(1, (byte) 0x44);
        ackMessage.put(2, (byte) 0x4E);
        return ackMessage;
    }
}
