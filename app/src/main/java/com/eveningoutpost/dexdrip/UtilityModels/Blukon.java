package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;


import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.LibreBlock;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.utils.CheckBridgeBattery;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.xdrip;
import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;

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
    private static int GET_SENSOR_AGE_DELAY = 3 * 3600;
    private static final String BLUKON_GETSENSORAGE_TIMER = "blukon-getSensorAge-timer";
    private static final String BLUKON_DECODE_SERIAL_TIMER = "blukon-decodeSerial-timer";
    private static int GET_DECODE_SERIAL_DELAY = 12 * 3600;
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

        m_minutesDiff = (long) ((((JoH.tsl() - m_timeLastCmdReceived) / 1000) + 30) / 60);
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
            Log.i(TAG, "initialize!");
            Pref.setInt("bridge_battery", 0); //force battery to no-value before first reading
            Pref.setInt("nfc_sensor_age", 0); //force sensor age to no-value before first reading
            JoH.clearRatelimit(BLUKON_GETSENSORAGE_TIMER);
            m_getNowGlucoseDataCommand = false;
            m_getNowGlucoseDataIndexCommand = false;

            m_getOlderReading = false;
            m_blockNumber = 0;
            // @keencave - initialize only once during initial to ensure no backfilling at start
            //       m_timeLastBg = 0;
    }

    public static boolean isBlukonPacket(byte[] buffer) {
    /* -53  0xCB -117 0x8B */
        return !((buffer == null) || (buffer.length < 3)) && (buffer[0] == (byte) 0xCB || buffer[0] == (byte) 0x8B);
    }

    public static boolean checkBlukonPacket(byte[] buffer) {
        return isBlukonPacket(buffer) && getPin() != null; // TODO can't be unset yet and isn't proper subtype test yet
    }

    // .*(dexdrip|gatt|Blukon).
    public static byte[] decodeBlukonPacket(byte[] buffer) {
        int cmdFound = 0;
        Boolean gotLowBat = false;

        if (buffer == null) {
            Log.e(TAG, "null buffer passed to decodeBlukonPacket");
            return null;
        }

        m_timeLastCmdReceived = JoH.tsl();

        //BluCon code by gregorybel
        final String strRecCmd = CipherUtils.bytesToHex(buffer).toLowerCase();
        Log.i(TAG, "Blukon data: " + strRecCmd);

        if (Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            Log.i(TAG, HexDump.dumpHexString(buffer));
        }

        if (strRecCmd.equalsIgnoreCase("cb010000")) {
            Log.i(TAG, "Reset currentCommand");
            currentCommand = "";
            cmdFound = 1;
            m_communicationStarted = true;
        }

        // BluconACKRespons will come in two different situations
        // 1) after we have sent an ackwakeup command
        // 2) after we have a sleep command
        if (strRecCmd.startsWith("8b0a00")) {
            cmdFound = 1;
            Log.i(TAG, "Got ACK");

            if (currentCommand.startsWith("810a00")) {//ACK sent
                //ack received

                currentCommand = "010d0b00";
                Log.i(TAG, "getUnknownCmd1: " + currentCommand);

            } else {
                Log.i(TAG, "Got sleep ack, resetting initialstate!");
                currentCommand = "";
            }
        }

        if (strRecCmd.startsWith("8b1a02")) {
            cmdFound = 1;
            Log.e(TAG, "Got NACK on cmd=" + currentCommand + " with error=" + strRecCmd.substring(6));

            if (strRecCmd.startsWith("8b1a020014")) {
                Log.e(TAG, "Timeout: please wait 5min or push button to restart!");
            }

            if (strRecCmd.startsWith("8b1a02000f")) {
                Log.e(TAG, "Libre sensor has been removed!");
            }

            if (strRecCmd.startsWith("8b1a020011")) {
                Log.e(TAG, "Patch read error.. please check the connectivity and re-initiate... or maybe battery is low?");
                Pref.setInt("bridge_battery", 1);
                gotLowBat = true;
            }

            if (strRecCmd.startsWith("8b1a020009")) {
                //Log.e(TAG, "");
            }

            m_getNowGlucoseDataCommand = false;
            m_getNowGlucoseDataIndexCommand = false;

            currentCommand = "010c0e00";
            Log.i(TAG, "Send sleep cmd");
            m_communicationStarted = false;


            JoH.clearRatelimit(BLUKON_GETSENSORAGE_TIMER);// set to current time to force timer to be set back
        }

        if (currentCommand.equals("") && strRecCmd.equalsIgnoreCase("cb010000")) {
            cmdFound = 1;
            Log.i(TAG, "wakeup received");

            //must be first cmd to be sent otherwise get NACK!
            currentCommand = "010d0900";
            Log.i(TAG, "getPatchInfo");

        } else if (currentCommand.startsWith("010d0900") /*getPatchInfo*/ && strRecCmd.startsWith("8bd9")) {
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

            if (JoH.pratelimit(BLUKON_DECODE_SERIAL_TIMER, GET_DECODE_SERIAL_DELAY)) {
                decodeSerialNumber(buffer);
            }

            if (isSensorReady(buffer[17])) {
                currentCommand = "810a00";
                Log.i(TAG, "Send ACK");
            } else {
                Log.e(TAG, "Sensor is not ready, stop!");
                currentCommand = "010c0e00";
                Log.i(TAG, "Send sleep cmd");
                m_communicationStarted = false;
            }

        } else if (currentCommand.startsWith("010d0b00") /*getUnknownCmd1*/ && strRecCmd.startsWith("8bdb")) {
            cmdFound = 1;
            Log.i(TAG, "gotUnknownCmd1 (010d0b00): " + strRecCmd);

            if (!strRecCmd.equals("8bdb0101041711")) {
                Log.e(TAG, "gotUnknownCmd1 (010d0b00): " + strRecCmd);
            }

            currentCommand = "010d0a00";
            Log.i(TAG, "getUnknownCmd2 " + currentCommand);

        } else if (currentCommand.startsWith("010d0a00") /*getUnknownCmd2*/ && strRecCmd.startsWith("8bda")) {
            cmdFound = 1;
            Log.i(TAG, "gotUnknownCmd2 (010d0a00): " + strRecCmd);

            if (!strRecCmd.equals("8bdaaa")) {
                Log.e(TAG, "gotUnknownCmd2 (010d0a00): " + strRecCmd);
            }

            if (strRecCmd.equals("8bda02")) {
                Log.e(TAG, "gotUnknownCmd2: is maybe battery low????");
                Pref.setInt("bridge_battery", 5);
                gotLowBat = true;
            }

            if (JoH.pratelimit(BLUKON_GETSENSORAGE_TIMER, GET_SENSOR_AGE_DELAY)) {
                currentCommand = "010d0e0127";
                Log.i(TAG, "getSensorAge");
            } else {
                if (Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
                    // Send the command to getHistoricData (read all blcoks from 0 to 0x2b)
                    Log.i(TAG, "getHistoricData (2)");
                    currentCommand = "010d0f02002b";
                    m_blockNumber = 0;
                } else {
                    currentCommand = "010d0e0103";
                    m_getNowGlucoseDataIndexCommand = true;//to avoid issue when gotNowDataIndex cmd could be same as getNowGlucoseData (case block=3)
                    Log.i(TAG, "getNowGlucoseDataIndexCommand");
                }
            }

        } else if (currentCommand.startsWith("010d0e0127") /*getSensorAge*/ && strRecCmd.startsWith("8bde")) {
            cmdFound = 1;
            Log.i(TAG, "SensorAge received");

            int sensorAge = sensorAge(buffer);

            if ((sensorAge > 0) && (sensorAge < 200000)) {
                Pref.setInt("nfc_sensor_age", sensorAge);//in min
            }
            if (Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
                // Send the command to getHistoricData (read all blcoks from 0 to 0x2b)
                Log.i(TAG, "getHistoricData (3)");
                currentCommand = "010d0f02002b";
                m_blockNumber = 0;
            } else {
                currentCommand = "010d0e0103";
                m_getNowGlucoseDataIndexCommand = true;//to avoid issue when gotNowDataIndex cmd could be same as getNowGlucoseData (case block=3)
                Log.i(TAG, "getNowGlucoseDataIndexCommand");
            }

        } else if (currentCommand.startsWith("010d0e0103") /*getNowDataIndex*/ && m_getNowGlucoseDataIndexCommand == true && strRecCmd.startsWith("8bde")) {
            cmdFound = 1;
            // calculate time delta to last valid BG reading
            m_persistentTimeLastBg = PersistentStore.getLong("blukon-time-of-last-reading");
            m_minutesDiffToLastReading = (int) ((((JoH.tsl() - m_persistentTimeLastBg) / 1000) + 30) / 60);
            Log.i(TAG, "m_minutesDiffToLastReading=" + m_minutesDiffToLastReading + ", last reading: " + JoH.dateTimeText(m_persistentTimeLastBg));

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
                currentCommand = "010d0e010" + Integer.toHexString(m_currentBlockNumber);//getNowGlucoseData
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
                currentCommand = "010d0e010" + Integer.toHexString(delayedBlockNumber);//getNowGlucoseData
                Log.i(TAG, "getNowGlucoseData backfilling");
            }
            m_getNowGlucoseDataIndexCommand = false;
            m_getNowGlucoseDataCommand = true;

        } else if (currentCommand.startsWith("010d0e01") /*getNowGlucoseData*/ && m_getNowGlucoseDataCommand == true && strRecCmd.startsWith("8bde")) {
            Log.d(TAG, "Before Saving data: + currentCommand = " + currentCommand);
            String blockId = currentCommand.substring("010d0e01".length());
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
                processNewTransmitterData(TransmitterData.create(currentGlucose, currentGlucose, 0 /*battery level force to 0 as unknown*/, now));

                m_timeLastBg = now;

                PersistentStore.setLong("blukon-time-of-last-reading", m_timeLastBg);
                Log.i(TAG, "time of current reading: " + JoH.dateTimeText(m_timeLastBg));

                currentCommand = "010c0e00";
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
                currentCommand = "010d0e010" + Integer.toHexString(delayedBlockNumber);//getNowGlucoseData
                Log.i(TAG, "bf: read next block: " + currentCommand);


            }
        } else if ((currentCommand.startsWith("010d0f02002b") /*getHistoricData */ || (currentCommand.isEmpty() && m_blockNumber > 0)) && strRecCmd.startsWith("8bdf")) {
            cmdFound = 1;
            handlegetHistoricDataResponse(buffer);
        } else if (strRecCmd.startsWith("cb020000")) {
            cmdFound = 1;
            Log.e(TAG, "is bridge battery low????!");
            Pref.setInt("bridge_battery", 3);
            gotLowBat = true;
        } else if (strRecCmd.startsWith("cbdb0000")) {
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
            currentCommand = "010c0e00";
            Log.i(TAG, "Send sleep cmd");
            m_communicationStarted = false;

            Log.i(TAG, "Full data that was received is " + HexDump.dumpHexString(m_full_data));
            LibreBlock.createAndSave("blukon", now, m_full_data, 0);
            
            Intent intent = new Intent(Intents.XDRIP_PLUS_LIBRE_DATA);
            Bundle bundle = new Bundle();
            bundle.putByteArray(Intents.LIBRE_DATA_BUFFER, m_full_data);
            bundle.putLong(Intents.LIBRE_DATA_TIMESTAMP, now);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            xdrip.getAppContext().sendBroadcast(intent);
        } else {
            currentCommand = "";
        }
    }

    private static boolean isSensorReady(byte sensorStatusByte) {

        String sensorStatusString = "";
        boolean ret = false;

        switch (sensorStatusByte) {
            case 0x01:
                sensorStatusString = "not yet started";
                break;
            case 0x02:
                sensorStatusString = "starting";
                ret = true;
                break;
            case 0x03:          // status for 14 days and 12 h of normal operation, abbott reader quits after 14 days
                sensorStatusString = "ready";
                ret = true;
                break;
            case 0x04:          // status of the following 12 h, sensor delivers last BG reading constantly
                sensorStatusString = "expired";
                // @keencave: to use dead sensor for test
//                ret = true;
                break;
            case 0x05:          // sensor stops operation after 15d after start
                sensorStatusString = "shutdown";
                // @keencave: to use dead sensors for test
//                ret = true;
                break;
            case 0x06:
                sensorStatusString = "in failure";
                break;
            default:
                sensorStatusString = "in an unknown state";
                break;
        }

        Log.i(TAG, "Sensor status is: " + sensorStatusString);

        if (!ret) {
            Home.toaststaticnext("Can't use this sensor as it is " + sensorStatusString);
        }

        return ret;
    }

    private static void decodeSerialNumber(byte[] input) {

        byte[] uuid = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
        String lookupTable[] =
                {
                        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                        "A", "C", "D", "E", "F", "G", "H", "J", "K", "L",
                        "M", "N", "P", "Q", "R", "T", "U", "V", "W", "X",
                        "Y", "Z"
                };
        byte[] uuidShort = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
        int i;

        for (i = 2; i < 8; i++) uuidShort[i - 2] = input[(2 + 8) - i];
        uuidShort[6] = 0x00;
        uuidShort[7] = 0x00;

        String binary = "";
        String binS = "";
        for (i = 0; i < 8; i++) {
            binS = String.format("%8s", Integer.toBinaryString(uuidShort[i] & 0xFF)).replace(' ', '0');
            binary += binS;
        }

        String v = "0";
        char[] pozS = {0, 0, 0, 0, 0};
        for (i = 0; i < 10; i++) {
            for (int k = 0; k < 5; k++) pozS[k] = binary.charAt((5 * i) + k);
            int value = (pozS[0] - '0') * 16 + (pozS[1] - '0') * 8 + (pozS[2] - '0') * 4 + (pozS[3] - '0') * 2 + (pozS[4] - '0') * 1;
            v += lookupTable[value];
        }
        Log.e(TAG, "decodeSerialNumber=" + v);

        PersistentStore.setString("blukon-serial-number", v);
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
        final boolean thirteen_bit_mask = Pref.getBooleanDefaultFalse("testing_use_thirteen_bit_mask");
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
