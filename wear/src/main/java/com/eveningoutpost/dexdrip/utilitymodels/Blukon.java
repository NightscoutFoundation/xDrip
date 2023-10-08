package com.eveningoutpost.dexdrip.utilitymodels;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.SensorSanity;
import com.eveningoutpost.dexdrip.models.TransmitterData;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.services.DexCollectionService;
import com.eveningoutpost.dexdrip.utils.CheckBridgeBattery;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * Created by gregorybel / jamorham on 02/09/2017.
 */

public class Blukon {

    private static final String TAG = "Blukon";
    public static final String BLUKON_PIN_PREF = "Blukon-bluetooth-pin";

    private static int m_nowGlucoseOffset = 0;

    // @keencave - global vars for backfill processing
    private static int m_currentTrendIndex;
    private static int m_currentBlockNumber = 0;
    private static int m_currentOffset = 0;
    private static int m_minutesDiffToLastReading = 0;
    private static int m_minutesBack;
    private static boolean m_getOlderReading = false;
    private static boolean m_communicationStarted = false;

    private static String currentCommand = "";

    //TO be used later
    private static enum BLUKON_STATES {
        INITIAL
    }

    private static boolean m_getNowGlucoseDataIndexCommand = false;
    private static final int GET_SENSOR_AGE_DELAY = 3 * 3600;
    private static final String BLUKON_GETSENSORAGE_TIMER = "blukon-getSensorAge-timer";
    private static boolean m_getNowGlucoseDataCommand = false;// to be sure we wait for a GlucoseData Block and not using another block
    private static long m_timeLastBg = 0;
    private static long m_persistentTimeLastBg;
    private static int m_blockNumber = 0;
    private static byte[] m_full_data = new byte[344];
    private static long m_timeLastCmdReceived = 0;

    public static String getPin() {
        final String thepin = Pref.getString(BLUKON_PIN_PREF, null);
        if ((thepin != null) && (thepin.length() < 3))
            return null; // TODO enforce sane minimum pin length
        return thepin;
    }

    private static void setPin(String thepin) {
        if (thepin == null) return;
        Pref.setString(BLUKON_PIN_PREF, thepin);
    }

    public static boolean isCollecting() {
        // use internal logic to decide if we are collecting something, if we return true here
        // then we will never get reset due to missed reading service restarts
        long m_minutesDiff = 0;

        m_minutesDiff = (long) (JoH.msSince(m_timeLastCmdReceived) / Constants.MINUTE_IN_MS);

        Log.i(TAG, "m_minutesDiff to last cmd=" + m_minutesDiff + ", last cmd received at: " + JoH.dateTimeText(m_timeLastCmdReceived));

        if (m_communicationStarted) {
            //we need to make sure communication did not stop a long time ago because of another issue
            if (m_minutesDiff > 2)//min. A cmd should be received within a few ms so if after this time nothing has been received we overwrite this flag
            {
                m_communicationStarted = false;
            }
        }

        return m_communicationStarted;
    }

    public static void clearPin() {
        Pref.removeItem(BLUKON_PIN_PREF);
    }

    public static void initialize() {
            Log.i(TAG, "initialize Blukon!");
            JoH.clearRatelimit(BLUKON_GETSENSORAGE_TIMER);
            m_getNowGlucoseDataCommand = false;
            m_getNowGlucoseDataIndexCommand = false;

            m_getOlderReading = false;
            m_blockNumber = 0;
            // @keencave - initialize only once during initial to ensure no backfilling at start
            //       m_timeLastBg = 0;
    }

/*
*   blucon protocol description
*
*   scheme: 
*   blucon commands starts with 0x01/0x0B followed by a 1 byte descriptor
*       - only the ackWakeup starts with 0x81/0x0A
*   blucon answers 
*       - 0x8B 0xD<command descriptor as 4 bit LSB nibble>
*       - 0x1A in NAK state
*       - wakeUp starts with 0xCB
*   payload in the commands are coded wuth a lenght byte followed a approbiate amount of bytes
*   readSIngleBlock: 01-0D-0E-01-<block number>
*   readMultipleBlock: 01-0D-0F-02-<start block>-<end block>
*/

/*
private static final String READ_SINGLE_BLOCK_PREFIX =                  "010d0e01";
private static final String BLOCK_NUMBER_TREND_HISTORY =                "03";
private static final String BLOCK_NUMBER_SENSOR_TIME =                  "27";
private static final String GET_SENSOR_TIME_BLOCK_COMMAND = READ_SINGLE_BLOCK_PREFIX+BLOCK_NUMBER_SENSOR_TIME;
private static final String GET_TREND_HISTORY_BLOCK_COMMAND = READ_SINGLE_BLOCK_PREFIX+BLOCK_NUMBER_TREND_HISTORY;
*/

private static final String WAKEUP_COMMAND =                            "cb010000";
private static final String ACK_ON_WAKEUP_ANSWER =                      "810a00";
private static final String SLEEP_COMMAND =                             "010c0e00";

//private static final String GET_SERIAL_NUMBER_COMMAND = "010d0e0100";
private static final String GET_PATCH_INFO_COMMAND =                    "010d0900";

private static final String UNKNOWN1_COMMAND =                          "010d0b00";
private static final String UNKNOWN2_COMMAND =                          "010d0a00";

private static final String GET_SENSOR_TIME_COMMAND =                   "010d0e0127";     // read single block #0x27
private static final String GET_NOW_DATA_INDEX_COMMAND =                "010d0e0103";  // read single block #0x03
//private static final String getNowGlucoseData = "9999999999";
//private static final String GET_TREND_DATA_COMMAND = "010d0f02030c";
//private static final String GET_HISTORIC_DATA_COMMAND getHistoricData = "010d0f020f18";
private static final String READ_SINGLE_BLOCK_COMMAND_PREFIX =          "010d0e010";
private static final String READ_SINGLE_BLOCK_COMMAND_PREFIX_SHORT =    "010d0e01";
private static final String GET_HISTORIC_DATA_COMMAND_ALL_BLOCKS =      "010d0f02002b"; // read all blocks from 0 to 0x2B

private static final String PATCH_INFO_RESPONSE_PREFIX =                "8bd9";
private static final String SINGLE_BLOCK_INFO_RESPONSE_PREFIX =         "8bde";
private static final String MULTIPLE_BLOCK_RESPONSE_INDEX =             "8bdf";
//private static final String SENSOR_TIME_RESPONSE_PREFIX = "8bde27";
private static final String BLUCON_ACK_RESPONSE =                       "8b0a00";
private static final String BLUCON_NAK_RESPONSE_PREFIX =                "8b1a02";

private static final String BLUCON_UNKNOWN1_COMMAND_RESPONSE =          "8bdb0101041711";
private static final String BLUCON_UNKNOWN2_COMMAND_RESPONSE =          "8bdaaa";
private static final String BLUCON_UNKNOWN2_COMMAND_RESPONSE_BATTERY_LOW = "8bda02";

private static final String BLUCON_NAK_RESPONSE_ERROR09 =               "8b1a020009";
private static final String BLUCON_NAK_RESPONSE_ERROR14 =               "8b1a020014";

private static final String PATCH_NOT_FOUND_RESPONSE =                  "8b1a02000f";
private static final String PATCH_READ_ERROR =                          "8b1a020011";

// we guess that this two commands indicate a low battery state
private static final String BLUCON_BATTERY_LOW_INDICATION1 =            "cb020000";
private static final String BLUCON_BATTERY_LOW_INDICATION2 =            "cbdb0000";

private static final int POSITION_OF_SENSOR_STATUS_BYTE = 17;

    /*
     * check first byte to detect valid blucon answers
     */ 
    public static boolean isBlukonPacket(byte[] buffer) {
    /* -53  0xCB -117 0x8B */
        return !((buffer == null) || (buffer.length < 3)) && (buffer[0] == (byte) 0xCB || buffer[0] == (byte) 0x8B);
    }

    public static boolean checkBlukonPacket(byte[] buffer) {
        return isBlukonPacket(buffer) && getPin() != null; // TODO can't be unset yet and isn't proper subtype test yet
    }

    /*
     * every blucon BT device name starts with "BLU"
     */
    public static boolean expectingBlukonDevice() {
        try {
            final ActiveBluetoothDevice btDevice = ActiveBluetoothDevice.first();
            if (btDevice.name.startsWith("BLU")) return true;
        } catch (Exception e) {
            //
        }
        return false;
    }

    public static void unBondIfBlukonAtInit() {
        try {
            if (Blukon.expectingBlukonDevice() && Pref.getBooleanDefaultFalse("blukon_unbonding")) {
                final ActiveBluetoothDevice btDevice = ActiveBluetoothDevice.first();
                if (btDevice != null) {
                    UserError.Log.d(TAG, "Unbonding blukon at initialization");
                    JoH.unBond(btDevice.address);
                }
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception trying to unbond blukon at init");
        }
    }

    // .*(dexdrip|gatt|Blukon).
    /* 
     * state machine to deal with the blucon protocol
     */
    public synchronized static byte[] decodeBlukonPacket(byte[] buffer) {
        int cmdFound = 0;
        Boolean gotLowBat = false;
        Boolean getHistoricReadings = false;

        if (buffer == null) {
            Log.e(TAG, "null buffer passed to decodeBlukonPacket");
            return null;
        }

        m_timeLastCmdReceived = JoH.tsl();

        // calculate time delta to last valid BG reading
        m_persistentTimeLastBg = PersistentStore.getLong("blukon-time-of-last-reading");
        m_minutesDiffToLastReading = (int) (((JoH.msSince(m_persistentTimeLastBg) / 1000) + 30) / 60);

        Log.i(TAG, "m_minutesDiffToLastReading=" + m_minutesDiffToLastReading + ", last reading: " + JoH.dateTimeText(m_persistentTimeLastBg));

        // Get history if the last reading is older than we can reasonably backfill
        if (Pref.getBooleanDefaultFalse("retrieve_blukon_history") && (m_persistentTimeLastBg > 0) && (m_minutesDiffToLastReading > 17)) {
            getHistoricReadings = true;
        }

        //BluCon code by gregorybel
        final String strRecCmd = CipherUtils.bytesToHex(buffer).toLowerCase();
        Log.i(TAG, "Blukon data: " + strRecCmd);

        if (Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            Log.i(TAG, HexDump.dumpHexString(buffer));
        }

        /*
         * step 1: have we got a wakeUp command from blucon?
         */
        if (strRecCmd.equalsIgnoreCase(WAKEUP_COMMAND)) {
            cmdFound = 1;
            Log.i(TAG, "Reset currentCommand");
            currentCommand = "";
            m_communicationStarted = true;
        }

        // BluconACKResponse will come in two different situations
        // 1) after we have sent an ackwakeup command
        // 2) after we have a sleep command
        /* 
         * step 4 / step 11: receive ACK on wakeup or after sending sleep command
         */
        if (strRecCmd.startsWith(BLUCON_ACK_RESPONSE)) {
            cmdFound = 1;
            Log.i(TAG, "Got ACK");

            if (currentCommand.startsWith(ACK_ON_WAKEUP_ANSWER)) {//ACK sent
                //ack received

                currentCommand = UNKNOWN1_COMMAND;
                Log.i(TAG, "getUnknownCmd1: " + currentCommand);

            } else {
                Log.i(TAG, "Got sleep ack, resetting initialstate!");
                currentCommand = "";
            }
        }

        if (strRecCmd.startsWith(BLUCON_NAK_RESPONSE_PREFIX)) {
            cmdFound = 1;
            Log.e(TAG, "Got NACK on cmd=" + currentCommand + " with error=" + strRecCmd.substring(6));

            if (strRecCmd.startsWith(BLUCON_NAK_RESPONSE_ERROR14)) {
                Log.e(TAG, "Timeout: please wait 5min or push button to restart!");
            }

            if (strRecCmd.startsWith(PATCH_NOT_FOUND_RESPONSE)) {
                Log.e(TAG, "Libre sensor has been removed!");
            }

            if (strRecCmd.startsWith(PATCH_READ_ERROR)) {
                Log.e(TAG, "Patch read error.. please check the connectivity and re-initiate... or maybe battery is low?");
                Pref.setInt("bridge_battery", 1);
                gotLowBat = true;
            }

            if (strRecCmd.startsWith(BLUCON_NAK_RESPONSE_ERROR09)) {
                //Log.e(TAG, "");
            }

            m_getNowGlucoseDataCommand = false;
            m_getNowGlucoseDataIndexCommand = false;

            currentCommand = SLEEP_COMMAND;
            Log.i(TAG, "Send sleep cmd");
            m_communicationStarted = false;


            JoH.clearRatelimit(BLUKON_GETSENSORAGE_TIMER);// set to current time to force timer to be set back
        }

        /* 
         * step 2: process getPatchInfo
         */
        if (currentCommand.equals("") && strRecCmd.equalsIgnoreCase(WAKEUP_COMMAND)) {
            cmdFound = 1;
            Log.i(TAG, "wakeup received");

            //must be first cmd to be sent otherwise get NACK!
           if (JoH.ratelimit("blukon-request_patch_info",1)) {
               currentCommand = GET_PATCH_INFO_COMMAND;
           }
            Log.i(TAG, "getPatchInfo");
        /*
         * step 3: analyse received patch info, decode serial number and check sensorStatus
         */
        } else if (currentCommand.startsWith(GET_PATCH_INFO_COMMAND) /*getPatchInfo*/ && strRecCmd.startsWith(PATCH_INFO_RESPONSE_PREFIX)) {
            cmdFound = 1;
            Log.i(TAG, "Patch Info received");

            /*
                in getPatchInfo: blucon answer is 20 bytes long.
                Bytes 13 - 19 (0 indexing) contains the bytes 0 ... 6 of block #0
                Bytes 11 to 12: ?
                Bytes 3 to 10: Serial Number reverse order
                Byte 2: 04: ?
                Bytes 0 - 1 (0 indexing) is the ordinary block request answer (0x8B 0xD9).

                Remark: Byte #17 (0 indexing) contains the SensorStatusByte.
            */

            if (!LibreUtils.validatePatchInfo(buffer)) {
                Log.e(TAG, "Patch info doesn't look valid - read error? " + JoH.bytesToHex(buffer));
            } else {

                final String SensorSn = LibreUtils.decodeSerialNumber(buffer);

                if (SensorSanity.checkLibreSensorChangeIfEnabled(SensorSn)) {
                    Log.e(TAG, "Problem with Libre Serial Number - not processing");
                    return null;
                }

                // TODO: Only write this after checksum was verified
                PersistentStore.setString("LibreSN", SensorSn);
            }

            if (LibreUtils.isSensorReady(buffer[POSITION_OF_SENSOR_STATUS_BYTE])) {
                currentCommand = ACK_ON_WAKEUP_ANSWER;
                Log.i(TAG, "Send ACK");
            } else {
                Log.e(TAG, "Sensor is not ready, stop!");
                currentCommand = SLEEP_COMMAND;
                Log.i(TAG, "Send sleep cmd");
                m_communicationStarted = false;
            }

        /*
         * step 5: send unknownCommand1 as otherwise communication errors will occur
         */
        } else if (currentCommand.startsWith(UNKNOWN1_COMMAND) /*getUnknownCmd1*/ && strRecCmd.startsWith("8bdb")) {
            cmdFound = 1;
            Log.i(TAG, "gotUnknownCmd1 (010d0b00): " + strRecCmd);

            if (!strRecCmd.equals(BLUCON_UNKNOWN1_COMMAND_RESPONSE)) {
                Log.e(TAG, "gotUnknownCmd1 (010d0b00): " + strRecCmd);
            }

            currentCommand = UNKNOWN2_COMMAND;
            Log.i(TAG, "getUnknownCmd2 " + currentCommand);

        /*
         * step 6: send unknownCommand2 as otherwise communication errors will occur
         */
        } else if (currentCommand.startsWith(UNKNOWN2_COMMAND) /*getUnknownCmd2*/ && strRecCmd.startsWith("8bda")) {
            cmdFound = 1;
            Log.i(TAG, "gotUnknownCmd2 (010d0a00): " + strRecCmd);

            if (!strRecCmd.equals(BLUCON_UNKNOWN2_COMMAND_RESPONSE)) {
                Log.e(TAG, "gotUnknownCmd2 (010d0a00): " + strRecCmd);
            }

            if (strRecCmd.equals(BLUCON_UNKNOWN2_COMMAND_RESPONSE_BATTERY_LOW)) {
                Log.e(TAG, "gotUnknownCmd2: is maybe battery low????");
                Pref.setInt("bridge_battery", 5);
                gotLowBat = true;
            }

            /* LibreAlarmReceiver.CalculateFromDataTransferObject, called when processing historical data,
             * expects the sensor age not to be updated yet, so only update the sensor age when not retrieving history.
             */
            if (Pref.getBooleanDefaultFalse("external_blukon_algorithm") || getHistoricReadings) {
                // Send the command to getHistoricData (read all blocks from 0 to 0x2b)
                Log.i(TAG, "getHistoricData (2)");
                currentCommand = GET_HISTORIC_DATA_COMMAND_ALL_BLOCKS;
                m_blockNumber = 0;

                //force read from sensor age when getting historic on next reading
                if (getHistoricReadings) {
                    JoH.clearRatelimit(BLUKON_GETSENSORAGE_TIMER);
                }
            } else {
                if (JoH.pratelimit(BLUKON_GETSENSORAGE_TIMER, GET_SENSOR_AGE_DELAY)) {
                    currentCommand = GET_SENSOR_TIME_COMMAND;
                    Log.i(TAG, "getSensorAge");
                } else {
                    currentCommand = GET_NOW_DATA_INDEX_COMMAND;
                    m_getNowGlucoseDataIndexCommand = true;//to avoid issue when gotNowDataIndex cmd could be same as getNowGlucoseData (case block=3)
                    Log.i(TAG, "getNowGlucoseDataIndexCommand");
                }
            }

        /*
         * step 7: calculate sensorAge from sensors FRAM copy
         */
        } else if (currentCommand.startsWith(GET_SENSOR_TIME_COMMAND) /*getSensorAge*/ && strRecCmd.startsWith(SINGLE_BLOCK_INFO_RESPONSE_PREFIX)) {
            cmdFound = 1;

            int sensorAge = sensorAge(buffer);
            Log.d(TAG, "SensorAge received=" + sensorAge);

            int currentSensorAge = Pref.getInt("nfc_sensor_age", 0);
            Log.d(TAG, "current SensorAge=" + currentSensorAge);

            //This is a new sensor, force read from serial
            if (sensorAge < currentSensorAge) {
                Log.i(TAG, "new sensor?");
            }

            if ((sensorAge >= 0) && (sensorAge < 200000)) {
                Pref.setInt("nfc_sensor_age", sensorAge);//in min
                //when getting historic, we use LibreAlarm Code and sensor age is not exactly same as calculated here
                //to avoid warning, simply overide this flag
                Pref.setBoolean("nfc_age_problem", false);
            } else {
                Log.e(TAG, "Do not set 'nfc_sensor_age'");
            }

            currentSensorAge = Pref.getInt("nfc_sensor_age", 0);
            Log.d(TAG, "[After set] current SensorAge=" + currentSensorAge);

            currentCommand = GET_NOW_DATA_INDEX_COMMAND;
            m_getNowGlucoseDataIndexCommand = true;//to avoid issue when gotNowDataIndex cmd could be same as getNowGlucoseData (case block=3)
            Log.i(TAG, "getNowGlucoseDataIndexCommand");

        /*
         * step 8: determine trend or historic data index
         */
        } else if (currentCommand.startsWith(GET_NOW_DATA_INDEX_COMMAND) /*getNowDataIndex*/ && m_getNowGlucoseDataIndexCommand == true && strRecCmd.startsWith(SINGLE_BLOCK_INFO_RESPONSE_PREFIX)) {
            cmdFound = 1;

            // check time range for valid backfilling
            if ((m_minutesDiffToLastReading > 7) && (m_minutesDiffToLastReading < (8 * 60))) {
                Log.i(TAG, "start backfilling");
                m_getOlderReading = true;
            } else {
                m_getOlderReading = false;
            }
            // get index to current BG reading
            m_currentBlockNumber = blockNumberForNowGlucoseData(buffer);
            m_currentOffset = m_nowGlucoseOffset;
            // time diff must be > 5,5 min and less than the complete trend buffer
            if (!m_getOlderReading) {
                currentCommand = READ_SINGLE_BLOCK_COMMAND_PREFIX + Integer.toHexString(m_currentBlockNumber);//getNowGlucoseData
                m_nowGlucoseOffset = m_currentOffset;
                Log.i(TAG, "getNowGlucoseData");
            } else {
                m_minutesBack = m_minutesDiffToLastReading;
                int delayedTrendIndex = m_currentTrendIndex;
                // ensure to have min 3 mins distance to last reading to avoid doible draws (even if they are distict)
                if (m_minutesBack > 17) {
                    m_minutesBack = 15;
                } else if (m_minutesBack > 12) {
                    m_minutesBack = 10;
                } else if (m_minutesBack > 7) {
                    m_minutesBack = 5;
                }
                Log.i(TAG, "read " + m_minutesBack + " mins old trend data");
                for (int i = 0; i < m_minutesBack; i++) {
                    if (--delayedTrendIndex < 0)
                        delayedTrendIndex = 15;
                }
                int delayedBlockNumber = blockNumberForNowGlucoseDataDelayed(delayedTrendIndex);
                currentCommand = READ_SINGLE_BLOCK_COMMAND_PREFIX + Integer.toHexString(delayedBlockNumber);//getNowGlucoseData
                Log.i(TAG, "getNowGlucoseData backfilling");
            }
            m_getNowGlucoseDataIndexCommand = false;
            m_getNowGlucoseDataCommand = true;

        /*
         * step 9: calculate fro current index the block number next to read
         */
        } else if (currentCommand.startsWith(READ_SINGLE_BLOCK_COMMAND_PREFIX_SHORT) /*getNowGlucoseData*/ && m_getNowGlucoseDataCommand == true && strRecCmd.startsWith(SINGLE_BLOCK_INFO_RESPONSE_PREFIX)) {
            Log.d(TAG, "Before Saving data: + currentCommand = " + currentCommand);
            String blockId = currentCommand.substring(READ_SINGLE_BLOCK_COMMAND_PREFIX_SHORT.length());
            long now = JoH.tsl();
            if(!blockId.isEmpty()) {
                int blockNum = JoH.parseIntWithDefault(blockId, 16, -1);
                if(blockNum != -1) {
                    Log.d(TAG, "Saving data: + blockid = " + blockNum);
                    LibreBlock.createAndSave("blukon", now , buffer, blockNum * 8);
                }
            }

            cmdFound = 1;
            int currentGlucose = nowGetGlucoseValue(buffer);

            Log.i(TAG, "********got getNowGlucoseData=" + currentGlucose);

            if (!m_getOlderReading) {

                m_minutesDiffToLastReading = (int) (JoH.msSince(m_persistentTimeLastBg) / Constants.MINUTE_IN_MS);
                Log.i(TAG, "m_minutesDiffToLastReading (no rounding)=" + m_minutesDiffToLastReading + ", last reading: " + JoH.dateTimeText(m_persistentTimeLastBg));

                if (m_minutesDiffToLastReading >= 4) {
                    processNewTransmitterData(TransmitterData.create(currentGlucose, currentGlucose, 0 /*battery level force to 0 as unknown*/, now));

                    m_timeLastBg = now;

                    PersistentStore.setLong("blukon-time-of-last-reading", m_timeLastBg);
                    Log.i(TAG, "time of current reading: " + JoH.dateTimeText(m_timeLastBg));
                } else {
                    Log.e(TAG, "New Cmd received too early, send blukon to sleep and ignore BG value");
                }

                /* 
                 * step 10: send sleep command
                 */
                currentCommand = SLEEP_COMMAND;
                Log.i(TAG, "Send sleep cmd");
                m_communicationStarted = false;
                m_getNowGlucoseDataCommand = false;
            } else {
                Log.i(TAG, "bf: processNewTransmitterData with delayed timestamp of " + m_minutesBack + " min");
                processNewTransmitterData(TransmitterData.create(currentGlucose, currentGlucose, 0 /*battery level force to 0 as unknown*/, now - (m_minutesBack * 60 * 1000)));
                // @keencave - count down for next backfilling entry
                m_minutesBack -= 5;
                if (m_minutesBack < 5) {
                    m_getOlderReading = false;
                }
                Log.i(TAG, "bf: calculate next trend buffer with " + m_minutesBack + " min timestamp");
                int delayedTrendIndex = m_currentTrendIndex;
                for (int i = 0; i < m_minutesBack; i++) {
                    if (--delayedTrendIndex < 0)
                        delayedTrendIndex = 15;
                }
                int delayedBlockNumber = blockNumberForNowGlucoseDataDelayed(delayedTrendIndex);
                currentCommand = READ_SINGLE_BLOCK_COMMAND_PREFIX + Integer.toHexString(delayedBlockNumber);//getNowGlucoseData
                Log.i(TAG, "bf: read next block: " + currentCommand);


            }
        } else if ((currentCommand.startsWith(GET_HISTORIC_DATA_COMMAND_ALL_BLOCKS) /*getHistoricData */ || (currentCommand.isEmpty() && m_blockNumber > 0)) && strRecCmd.startsWith(MULTIPLE_BLOCK_RESPONSE_INDEX)) {
            cmdFound = 1;
            handlegetHistoricDataResponse(buffer);
        } else if (strRecCmd.startsWith(BLUCON_BATTERY_LOW_INDICATION1)) {
            cmdFound = 1;
            Log.e(TAG, "is bridge battery low????!");
            Pref.setInt("bridge_battery", 3);
            gotLowBat = true;
        } else if (strRecCmd.startsWith(BLUCON_BATTERY_LOW_INDICATION2)) {
            cmdFound = 1;
            Log.e(TAG, "is bridge battery really low????!");
            Pref.setInt("bridge_battery", 2);
            gotLowBat = true;
        }

        if (!gotLowBat) {
            Pref.setInt("bridge_battery", 100);
        }

        CheckBridgeBattery.checkBridgeBattery();

        if (currentCommand.length() > 0 && cmdFound == 1) {
            Log.i(TAG, "Sending reply: " + currentCommand);
            return CipherUtils.hexToBytes(currentCommand);
        } else {
            if (cmdFound == 0) {
                Log.e(TAG, "***COMMAND NOT FOUND! -> " + strRecCmd + " on currentCmd=" + currentCommand);
            }
            currentCommand = "";
            return null;
        }

    }

    private static void handlegetHistoricDataResponse(byte[] buffer) {
        Log.e(TAG, "recieved historic data, m_block_number = " + m_blockNumber);
        // We are looking for 43 blocks of 8 bytes.
        // The bluekon will send them as 21 blocks of 16 bytes, and the last one of 8 bytes. 
        // The packet will look like "0x8b 0xdf 0xblocknumber 0x02 DATA" (so data starts at place 4)
        if (m_blockNumber > 42) {
            Log.e(TAG, "recieved historic data, but block number is too big " + m_blockNumber);
            return;
        }

        int len = buffer.length - 4;
        Log.e(TAG, "len = " + len + " " + len + " blocknum " + buffer[2]);

        if (buffer[2] != m_blockNumber) {
            Log.e(TAG, "We have recieved a bad block number buffer[2] = " + buffer[2] + " m_blockNumber = " + m_blockNumber);
            return;
        }
        if (8 * m_blockNumber + len > m_full_data.length) {
            Log.e(TAG, "We have recieved too much data  m_blockNumber = " + m_blockNumber + " len = " + len +
                    " m_full_data.length = " + m_full_data.length);
            return;
        }

        System.arraycopy(buffer, 4, m_full_data, 8 * m_blockNumber, len);
        m_blockNumber += len / 8;

        if (m_blockNumber >= 43) {
            long now = JoH.tsl();
            currentCommand = SLEEP_COMMAND;
            Log.i(TAG, "Send sleep cmd");
            m_communicationStarted = false;

            Log.i(TAG, "Full data that was received is " + HexDump.dumpHexString(m_full_data));

            final String tagId = PersistentStore.getString("LibreSN");
            NFCReaderX.HandleGoodReading(tagId, m_full_data, now);

            PersistentStore.setLong("blukon-time-of-last-reading", now);
            Log.i(TAG, "time of current reading: " + JoH.dateTimeText(now));
        } else {
            currentCommand = "";
        }
    }


    private static synchronized void processNewTransmitterData(TransmitterData transmitterData) {
        if (transmitterData == null) {
            Log.e(TAG, "Got duplicated data! Last BG at " + JoH.dateTimeText(m_timeLastBg));
            return;
        }

        final Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.i(TAG, "processNewTransmitterData: No Active Sensor, Data only stored in Transmitter Data");
            return;
        }

        DexCollectionService.last_transmitter_Data = transmitterData;
        Log.d(TAG, "BgReading.create: new BG reading at " + transmitterData.timestamp);
        BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, xdrip.getAppContext(), transmitterData.timestamp);
    }

    /* @keencave
     * extract trend index from FRAM block #3 from the libre sensor
     * input: blucon answer to trend index request, including 6 starting protocol bytes
     * return: 2 byte containing the next absolute block index to be read from
     * the libre sensor
     */

    private static int blockNumberForNowGlucoseData(byte[] input) {
        int nowGlucoseIndex2 = 0;
        int nowGlucoseIndex3 = 0;

        nowGlucoseIndex2 = (int) (input[5] & 0x0F);

        m_currentTrendIndex = nowGlucoseIndex2;

        // calculate byte position in sensor body
        nowGlucoseIndex2 = (nowGlucoseIndex2 * 6) + 4;

        // decrement index to get the index where the last valid BG reading is stored
        nowGlucoseIndex2 -= 6;
        // adjust round robin
        if (nowGlucoseIndex2 < 4)
            nowGlucoseIndex2 = nowGlucoseIndex2 + 96;

        // calculate the absolute block number which correspond to trend index
        nowGlucoseIndex3 = 3 + (nowGlucoseIndex2 / 8);

        // calculate offset of the 2 bytes in the block
        m_nowGlucoseOffset = nowGlucoseIndex2 % 8;

        Log.i(TAG, "++++++++currentTrendData: index " + m_currentTrendIndex + ", block " + nowGlucoseIndex3 + ", offset " + m_nowGlucoseOffset);

        return (nowGlucoseIndex3);
    }

    private static int blockNumberForNowGlucoseDataDelayed(int delayedIndex) {
        int i;
        int ngi2;
        int ngi3;

        // calculate byte offset in libre FRAM
        ngi2 = (delayedIndex * 6) + 4;

        ngi2 -= 6;
        if (ngi2 < 4)
            ngi2 = ngi2 + 96;

        // calculate the block number where to get the BG reading
        ngi3 = 3 + (ngi2 / 8);

        // calculate the offset in the block
        m_nowGlucoseOffset = ngi2 % 8;
        Log.i(TAG, "++++++++backfillingTrendData: index " + delayedIndex + ", block " + ngi3 + ", offset " + m_nowGlucoseOffset);

        return (ngi3);
    }


    /* @keencave
     * rescale raw BG reading to BG data format used in xDrip+
     * use 8.5 devider
     * raw format is in 1000 range
     */
    private static int getGlucose(long rawGlucose) {
        // standard divider for raw Libre data (1000 range)
        return (int) (rawGlucose * Constants.LIBRE_MULTIPLIER);
    }

    /* @keencave
     * extract BG reading from the raw data block containing the most recent BG reading
     * input: bytearray with blucon answer including 3 header protocol bytes
     * uses nowGlucoseOffset to calculate the offset of the two bytes needed
     * return: BG reading as int
     */

    private static int nowGetGlucoseValue(byte[] input) {
        final int curGluc;
        final long rawGlucose;

        // option to use 13 bit mask
        //final boolean thirteen_bit_mask = Pref.getBooleanDefaultFalse("testing_use_thirteen_bit_mask");
        final boolean thirteen_bit_mask = true;
        // grep 2 bytes with BG data from input bytearray, mask out 12 LSB bits and rescale for xDrip+
        rawGlucose = ((input[3 + m_nowGlucoseOffset + 1] & (thirteen_bit_mask ? 0x1F : 0x0F)) << 8) | (input[3 + m_nowGlucoseOffset] & 0xFF);
        Log.i(TAG, "rawGlucose=" + rawGlucose + ", m_nowGlucoseOffset=" + m_nowGlucoseOffset);

        // rescale
        curGluc = getGlucose(rawGlucose);

        return curGluc;
    }


    private static int sensorAge(byte[] input) {
        int sensorAge = ((input[3 + 5] & 0xFF) << 8) | (input[3 + 4] & 0xFF);
        Log.i(TAG, "sensorAge=" + sensorAge);

        return sensorAge;
    }

    public static void doPinDialog(final Activity activity, final Runnable runnable) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Please enter " + activity.getString(R.string.blukon) + " device PIN number");
        final View input = activity.getLayoutInflater().inflate(R.layout.dialog_pin_entry, null);
        builder.setView(input);
        builder.setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setPin(((EditText) input.findViewById(R.id.pinfield)).getText().toString().trim());
                if (getPin() != null) {
                    JoH.static_toast_long("Data source set to: " + activity.getString(R.string.blukon) + " pin: " + getPin());
                    runnable.run();
                    dialog.dismiss();
                } else {
                    JoH.static_toast_long("Invalid pin!");
                }
            }
        });
        builder.setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog dialog = builder.create();
        try {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } catch (NullPointerException e) {
            //
        }
        try {
            dialog.show();
        } catch (IllegalStateException e) {
            Log.e(TAG, e.toString());
            JoH.static_toast_long("Error displaying PIN entry. Please contact us if this keeps happening");
        }
    }
}
