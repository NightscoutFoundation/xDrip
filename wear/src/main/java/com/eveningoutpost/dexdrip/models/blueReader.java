package com.eveningoutpost.dexdrip.models;

import android.text.format.DateFormat;
import com.eveningoutpost.dexdrip.Home;
import java.nio.ByteBuffer;
import java.util.Date;
import com.eveningoutpost.dexdrip.services.DexCollectionService;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Notifications;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;
import com.eveningoutpost.dexdrip.R;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.*;

import static com.eveningoutpost.dexdrip.utils.FileUtils.getExternalDir;
import static com.eveningoutpost.dexdrip.utils.FileUtils.makeSureDirectoryExists;


/**
 * Created by MasterPlexus on 11.12.2017.
 */

public class blueReader {
    private static final String TAG = "blueReader";
    private static final String BatLog="/BatteryLog.csv";
    private static int counterHibernated = 0;
    private static Matcher tempVers;

    private static final byte[] shutdown = new byte[]{0x6B};        // Char 'k'
    private static final byte[] requestValue = new byte[]{0x6C};    // Char 'l'
    private static final byte[] goHybernate = new byte[]{0x68};     // Char 'h'
    private static final byte[] restart = new byte[]{0x79};         // Char 'y'


    public static boolean isblueReader() {
        final ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        try {
            return activeBluetoothDevice.name.contentEquals("blueReader");
        } catch (NullPointerException e) {
            return false;
        }
    }

    /* no real need at the moment 30.01.2018
    public static boolean isblueReaderPacket(byte[] buffer) {
          return !((buffer == null) || (new String(buffer).startsWith("IDR") ||
                                    new String(buffer).startsWith("TRANS_FAILED") ||
                                    new String(buffer).startsWith("HYBERNATE SUCCESS") ||
                                    new String(buffer).startsWith("not ready for") ||
                                    new String(buffer).startsWith("NFC_DISABLED") )
        );
    }
    */

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
            counterHibernated++;
            Log.e(TAG, "Found blueReader in a ugly State (" + counterHibernated + "/3), send hibernate to reset! If this does not help in the next 5 Minutes, then turn the bluereader manually off and on!");
            if (counterHibernated > 2) {
                Log.wtf(TAG, "Ugly state not resolveable. Bluereader will be shut down! Please restart it!");
                Home.toaststatic("BlueReader ugly state not resolveable, bluereader will be shut down. Please restart it!");
                if (!Pref.getBooleanDefaultFalse("blueReader_suppressuglystatemsg")) {
                    Notifications.RiseDropAlert(xdrip.getAppContext(),true,"BlueReader Alarm", xdrip.getAppContext().getString(R.string.bluereaderuglystate),1);
                }
                return shutdown;
            } else {
                Home.toaststatic("Found blueReader in a ugly State, send hibernate to reset!");
                return goHybernate; //send hard hibernate, because blueReader is in a ugly state
            }
        } else if (bufferstring.startsWith("IDR")){
            Log.i(TAG, bufferstring);
            PersistentStore.setString("blueReaderFirmware", bufferstring );
            tempVers=Pattern.compile(".*\\|blue(.*)-.*").matcher(bufferstring);
            tempVers.find();
            PersistentStore.setDouble("blueReaderFirmwareValue",Double.parseDouble(tempVers.group(1)));
            Log.i(TAG, "bluereader-Firmware-Version: " + tempVers);
            if (BgReading.last() == null || BgReading.last().timestamp + (4 * 60 * 1000) < System.currentTimeMillis()) {
                return requestValue;
            } else {
                return null;
            }
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
            if (counterHibernated > 0) {
                Log.w (TAG,"Found hibernation after wrong read. Resend read-command...");
                return requestValue;
            } else {
                return null;
            }
        } else if (bufferstring.startsWith("-r 0:")) {
            Log.d (TAG, "blueReader sends an unknown reaction: '" + bufferstring + "'");
            return null;
        } else if (bufferstring.startsWith("TRANS_FAILED")) {
            Log.w (TAG, "Attention: check position of blueReader on the sensor, as it was not able to read!");
            Home.toaststatic(xdrip.getAppContext().getString(R.string.bluereader_position));
            return null;
        } else if (bufferstring.startsWith("battery: ")) {
            if (BgReading.last() == null || BgReading.last().timestamp + (4 * 60 * 1000) < System.currentTimeMillis()) {
                return requestValue;
            }
        } else {
            counterHibernated = 0;
            processNewTransmitterData(TransmitterData.create(buffer, len, timestamp), timestamp);
            // check for shutdown blueReader if Battery is too low
            if (Pref.getBooleanDefaultFalse("blueReader_turn_off")) {
                if (Pref.getInt("blueReader_turn_off_value",5) > Pref.getInt("bridge_battery",100)) {
                    Log.w (TAG, "blueReader will be turn off, as the battery is lower then " + Pref.getInt("blueReader_turn_off_value",5) +"%");
                    Home.toaststatic(xdrip.getAppContext().getString(R.string.bluereaderoff) + Pref.getInt("blueReader_turn_off_value",5) +"%");
                    return shutdown;
                }
            }
        }

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

        if(PersistentStore.getLong("blueReader_Full_Battery") <3000 )
            PersistentStore.setLong("blueReader_Full_Battery", 4100);

        double blueReaderDays =0;
        if (transmitterData.sensor_battery_level > PersistentStore.getLong("blueReader_Full_Battery")) {
            PersistentStore.setLong("blueReader_Full_Battery", transmitterData.sensor_battery_level);
            Log.i(TAG, "blueReader_Full_Battery set to: " + transmitterData.sensor_battery_level) ;
        }
        int localBridgeBattery =((transmitterData.sensor_battery_level - 3300) * 100 / (((int) (long) PersistentStore.getLong("blueReader_Full_Battery"))-3300));
        Pref.setInt("bridge_battery", localBridgeBattery);
        sensor.latest_battery_level = localBridgeBattery;
        blueReaderDays = 6.129200670865791d / (1d + Math.pow(((double)transmitterData.sensor_battery_level/3763.700630306379d),(-61.04241888028577d))); //todo compare with test-formular, and new Data of batterylog
        if (transmitterData.sensor_battery_level < 3600) {
            blueReaderDays=blueReaderDays + 0.1d;
        }
        blueReaderDays = ((Math.round((blueReaderDays)*10d)/10d));

        PersistentStore.setString("bridge_battery_days", String.valueOf(blueReaderDays));
        sensor.save();
        if (Pref.getBooleanDefaultFalse("blueReader_writebatterylog")) {
            final String dir = getExternalDir();
            makeSureDirectoryExists(dir);
            writeLog(dir + BatLog,
                    DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()).toString() + "|" +
                            PersistentStore.getLong("blueReader_Full_Battery") + "|" +
                            transmitterData.sensor_battery_level + "|" +
                            sensor.latest_battery_level + "|" +
                            blueReaderDays
            );
        }
        DexCollectionService.last_transmitter_Data = transmitterData;
        Log.d(TAG, "BgReading.create: new BG reading at " + timestamp + " with a timestamp of " + transmitterData.timestamp);
        BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, xdrip.getAppContext(), transmitterData.timestamp);

    }


    public static ByteBuffer initialize() {
        Log.i(TAG, "initialize blueReader!");
        Pref.setInt("bridge_battery", 0);
        PersistentStore.setDouble("blueReaderFirmwareValue", 0);

        //command to get Firmware
        ByteBuffer ackMessage = ByteBuffer.allocate(3);
        ackMessage.put(0, (byte) 0x49);
        ackMessage.put(1, (byte) 0x44);
        ackMessage.put(2, (byte) 0x4E);
        return ackMessage;
    }

    private static void writeLog(String logFile, String logLine) {
        PrintWriter pWriter = null;
        try {
            pWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
            pWriter.println(logLine);
        } catch (IOException ioe) {
            Log.w(TAG, "log write error: " + ioe.toString());
        } finally {
            if (pWriter != null){
                pWriter.flush();
                pWriter.close();
            }
        }
    }

}
