package com.eveningoutpost.dexdrip.glucosemeter.glucomen;

import static com.eveningoutpost.dexdrip.glucosemeter.glucomen.GlucoMen.playSounds;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NfcV;

import com.eveningoutpost.dexdrip.glucosemeter.glucomen.blocks.IndexBlock;
import com.eveningoutpost.dexdrip.glucosemeter.glucomen.blocks.RecordBlock;
import com.eveningoutpost.dexdrip.glucosemeter.glucomen.blocks.SerialBlock;
import com.eveningoutpost.dexdrip.glucosemeter.glucomen.data.ProcessingThread;
import com.eveningoutpost.dexdrip.glucosemeter.glucomen.devices.BaseDevice;
import com.eveningoutpost.dexdrip.glucosemeter.glucomen.devices.Identify;
import com.eveningoutpost.dexdrip.glucosemeter.glucomen.st.T5StRead;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utils.jobs.BackgroundQueue;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.Getter;
import lombok.val;

/**
 * JamOrHam
 * GlucoMen NFC driver
 */

public class GlucoMenNfc {

    private static final String TAG = GlucoMenNfc.class.getSimpleName();
    private static final boolean D = false;

    private NfcV v;
    private BaseDevice d;
    @Getter
    private String serial;
    @Getter
    private IndexBlock indexBlock;

    private final ProcessingThread processor = new ProcessingThread(this);
    @Getter
    private final ConcurrentLinkedQueue<RecordBlock> glucoseBlocks = new ConcurrentLinkedQueue<>();
    @Getter
    private final ConcurrentLinkedQueue<RecordBlock> ketoneBlocks = new ConcurrentLinkedQueue<>();


    public GlucoMenNfc(final Tag tag) {
        v = NfcV.get(tag);
        d = Identify.getDevice(tag);
    }

    public void scan() {
        UserError.Log.d(TAG, "Scan starting");
        val diag = new Diagnostic(this);
        try {
            v.connect();
            if (v.isConnected()) {
                UserError.Log.d(TAG, "Connected");
                if (playSounds()) {
                    BackgroundQueue.post(() -> JoH.playResourceAudio(R.raw.bt_meter_connect));
                }
                if (diag.check(0)
                        && getDeviceSerial()
                        && diag.check(1)
                        && getIndex()
                        && diag.check(2)
                        && getGlucose()
                        && diag.check(3)
                        && getKetones()
                        && diag.check(4)) {

                    Home.staticRefreshBGChartsOnIdle();
                    UserError.Log.d(TAG, "All steps executed successfully");
                    // next step
                } else {
                    UserError.Log.d(TAG, "Something went wrong during processing");
                    if (playSounds()) {
                        BackgroundQueue.post(() -> JoH.playResourceAudio(R.raw.bt_meter_disconnect));
                    }
                }
            } else {
                UserError.Log.d(TAG, "Could not connect to tag");
            }
        } catch (IOException e) {
            UserError.Log.d(TAG, "Exception when trying to connect");
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Caught exception: " + e);
        }
    }


    private boolean getDeviceSerial() {
        val serial = SerialBlock.parse(read(d.getSerialOffset(), d.getSerialSize()));
        if (serial != null) {
            UserError.Log.d(TAG, "Serial: " + serial);
            this.serial = serial.serial;
            return true;
        } else {
            return false;
        }
    }

    private boolean getIndex() {
        indexBlock = IndexBlock.parse(read(d.getIndexOffset(), d.getIndexSize()));
        if (indexBlock != null) {
            UserError.Log.d(TAG, "Index: " + indexBlock);
        }
        return indexBlock != null;
    }

    private boolean getGlucoseRecord(final int index) {
        val b = RecordBlock.parse(read(d.getGlucoseRecordOffset(index), d.getGlucoseSize()));
        if (b == null) return false;
        glucoseBlocks.add(b);
        UserError.Log.d(TAG, "Glucose @ " + index + ": " + b);
        return true;
    }

    private boolean getKetoneRecord(final int index) {
        val b = RecordBlock.parse(read(d.getKetoneRecordOffset(index), d.getKetoneSize()));
        if (b == null) return false;
        ketoneBlocks.add(b);
        UserError.Log.d(TAG, "Ketone @ " + index + ": " + b);
        return true;
    }

    private boolean getGlucose() {
        processor.start();
        val records = indexBlock.glucoseRecords;
        for (int i = records - 1; i >= 0; i--) {
            if (!getGlucoseRecord(i)) {
                return false;
            }
            if (processor.isEnoughGlucose()) {
                return true;
            }
        }
        return true;
    }

    private boolean getKetones() {
        processor.start();
        val records = indexBlock.ketoneRecords;
        for (int i = records - 1; i >= 0; i--) {
            if (!getKetoneRecord(i)) {
                return false;
            }
            if (processor.isEnoughKetones()) {
                return true;
            }
        }
        return true;
    }

    private byte[] read(final int offset, final int len) {
        if (offset < 0) return null;
        val cmd = T5StRead.builder()
                .offset(offset)
                .length(len)
                .build().encode();
        return rTransceive(cmd);
    }

    byte[] rTransceive(final byte[] command) {
        if (v == null || command == null) return null;
        if (D) {
            UserError.Log.d(TAG, "Attempting to send command: " + HexDump.dumpHexString(command));
        }
        if (v.isConnected()) {
            try {
                val bytes = v.transceive(command);
                if (D) {
                    UserError.Log.d(TAG, "Received bytes: " + HexDump.dumpHexString(bytes));
                }
                return bytes;
            } catch (TagLostException e) {
                UserError.Log.d(TAG, "Tag was lost");
                return null;
            } catch (IOException e) {
                UserError.Log.d(TAG, "Failed to transceive: " + e);
            }
        } else {
            UserError.Log.d(TAG, "Tag no longer connected");
        }
        return null;
    }

}
