package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.EditText;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * Created by gregorybel / jamorham on 02/09/2017.
 */

public class Blukon {

    private static final String TAG = "Blukon";
    private static final String BLUKON_PIN_PREF = "Blukon-bluetooth-pin";

    private static int m_nowGlucoseOffset = 0;

    public static String getPin() {
        final String thepin = Home.getPreferencesStringWithDefault(BLUKON_PIN_PREF, null);
        if ((thepin != null) && (thepin.length() < 3))
            return null; // TODO enforce sane minimum pin length
        return thepin;
    }

    private static void setPin(String thepin) {
        if (thepin == null) return;
        Home.setPreferencesString(BLUKON_PIN_PREF, thepin);
    }

    public static void clearPin() {
        Home.removePreferencesItem(BLUKON_PIN_PREF);
    }

    public static boolean isBlukonPacket(byte[] buffer) {
    /* -53  0xCB -117 0x8B */
        return !((buffer == null) || (buffer.length < 3)) && (buffer[0] == (byte) 0xCB || buffer[0] == (byte) 0x8B);
    }

    public static boolean checkBlukonPacket(byte[] buffer) {
        return isBlukonPacket(buffer) && getPin() != null; // TODO can't be unset yet and isn't proper subtype test yet
    }

    public static byte[] decodeBlukonPacket(byte[] buffer) {

        if (buffer == null) {
            UserError.Log.e(TAG, "null buffer passed to decodeBlukonPacket");
            return null;
        }
        //BluCon code by gregorybel

        final String strRecCmd = CipherUtils.bytesToHex(buffer).toLowerCase();
        String strByteSend = "";

        UserError.Log.i(TAG, "BlueCon data: " + strRecCmd);

        //TODO add states!
        if (strRecCmd.equalsIgnoreCase("cb010000")) {
            UserError.Log.i(TAG, "wakeup received");
            strByteSend = "010d0900";
            UserError.Log.i(TAG, "getPatchInfo");
        } else {
            if (strRecCmd.startsWith("8bd9")) {
                UserError.Log.i(TAG, "Patch Info received");
                strByteSend = "810a00";
                UserError.Log.i(TAG, "Send ACK");
            } else if (strRecCmd.startsWith("8b0a00")) {
                UserError.Log.i(TAG, "Got ACK");
                strByteSend = "010d0e0103";
                UserError.Log.i(TAG, "getNowGlucoseDataIndexCommand");
            } else if (strRecCmd.startsWith("8bde03")) {
                UserError.Log.i(TAG, "gotNowDataIndex");
                //strByteSend = "010d0e0108";


                int blockNumber = blockNumberForNowGlucoseData(buffer);
                UserError.Log.i(TAG, "block Number is " + blockNumber);

                strByteSend = "010d0e010" + Integer.toHexString(blockNumber);//getNowGlucoseData


                UserError.Log.i(TAG, "getNowGlucoseData");
            } else if (strRecCmd.startsWith("8bde")) {
                final int currentGlucose = nowGetGlucoseValue(buffer);

                UserError.Log.i(TAG, "*****************got getNowGlucoseData = " + currentGlucose);

                processNewTransmitterData(TransmitterData.create(currentGlucose, currentGlucose, 0, JoH.tsl()));


                strByteSend = "010c0e00";
                UserError.Log.i(TAG, "Send sleep cmd");
            } else if (strRecCmd.startsWith("8bde08")) {
                UserError.Log.e(TAG, "Got error");
            }
        }

        if (strByteSend.length() > 0) {
            UserError.Log.d(TAG, "Sending reply: " + strByteSend);
            return CipherUtils.hexToBytes(strByteSend);
        } else {
            return null;
        }

    }


    private static synchronized void processNewTransmitterData(TransmitterData transmitterData) {
        if (transmitterData == null) {
            return;
        }

        final Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            UserError.Log.i(TAG, "processNewTransmitterData: No Active Sensor, Data only stored in Transmitter Data");
            return;
        }

        DexCollectionService.last_transmitter_Data = transmitterData;
        UserError.Log.d(TAG, "BgReading.create: new BG reading at " + transmitterData.timestamp + " with a timestamp of " + transmitterData.timestamp);
        BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, xdrip.getAppContext(), transmitterData.timestamp);
    }

    /*
 * extract trend index from FRAM block #3 from the libre sensor
 * input: string with blucon answer to trend index request, including 6 starting protocol bytes
 * return: 2 byte string containing the next abolute block index to be read from
 * the libre sensor
 */

    private static int blockNumberForNowGlucoseData(byte[] input) {
        int nowGlucoseIndex2 = 0;
        int nowGlucoseIndex3 = 0;

        nowGlucoseIndex2 = (int) input[5];

        // caculate byte position in sensor body
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

        UserError.Log.i(TAG, "m_nowGlucoseOffset=" + m_nowGlucoseOffset);

        return (nowGlucoseIndex3);
    }

        /*
 * rescale raw BG reading to BG data format used in xDrip+
 * use 8.5 devider
 * raw format is in 1000 range
 * xDrip format is 100 range
 */

    private static int getGlucose(long rawGlucose) {
        // standard devicder for raw Libre data (1000 range) to 100 range
        return (int) (rawGlucose * Constants.LIBRE_MULTIPLIER);
    }

        /*
 * extract BG reading from the raw data block containing the most recent BG reading
 * input: bytearray with blucon answer including 3 header protocol bytes
 * uses nowGlucoseOffset to calculate the offset of the two bytes neede
 * return: BG reading in float
 */

    private static int nowGetGlucoseValue(byte[] input) {
        final int curGluc;
        final long rawGlucose;

        // grep 2 bytes with BG data from input bytearray, mask out 12 LSB bits and rescale for xDrip+
        //rawGlucose = (input[3+m_nowGlucoseOffset+1]&0x0F)*16 + input[3+m_nowGlucoseOffset];
        rawGlucose = ((input[3 + m_nowGlucoseOffset + 1] & 0x0F) << 8) | (input[3 + m_nowGlucoseOffset] & 0xFF);
        UserError.Log.i(TAG, "rawGlucose=" + rawGlucose);

        // rescale
        curGluc = getGlucose(rawGlucose);

        return curGluc;
    }

    public static void doPinDialog(final Activity activity, final Runnable runnable) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Please enter " + activity.getString(R.string.blukon) + " device PIN number");
        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        builder.setView(input);
        builder.setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setPin(input.getText().toString().trim());
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
        AlertDialog dialog = builder.create();
        try {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } catch (NullPointerException e) {
            //
        }
        dialog.show();
    }
}
