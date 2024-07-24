package com.eveningoutpost.dexdrip.insulin.opennov;

import static com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage.d;
import static com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage.log;
import static com.eveningoutpost.dexdrip.insulin.opennov.Options.extraDebug;
import static com.eveningoutpost.dexdrip.insulin.opennov.Options.playSounds;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.buffer.MyByteBuffer;
import com.eveningoutpost.dexdrip.insulin.opennov.data.ICompleted;
import com.eveningoutpost.dexdrip.insulin.opennov.ll.PHDllHelper;
import com.eveningoutpost.dexdrip.insulin.opennov.ll.T4Transceiver;
import com.eveningoutpost.dexdrip.utils.jobs.BackgroundQueue;

import java.io.IOException;

import lombok.val;

/**
 * JamOrHam
 * OpenNov implementation
 */

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class OpenNov extends MyByteBuffer {

    private static final String TAG = "OpenNov";
    private static final int MAX_ERRORS = 3;

    private T4Transceiver ts;
    private PHDllHelper ph;

    public boolean processTag(final Tag tag, final ICompleted dataSaver) {
        try {
            val isoDep = IsoDep.get(tag);
            this.ts = new T4Transceiver(isoDep);
            this.ph = new PHDllHelper(this.ts);
            d = extraDebug();

            isoDep.connect();
            isoDep.setTimeout(1000);

            if (ts.doNeededSelection()) {
                UserError.Log.d(TAG, "Selection okay");
                if (playSounds()) {
                    BackgroundQueue.post(() -> JoH.playResourceAudio(R.raw.bt_meter_connect));
                }
                int errors = 0;
                int transactions = 0;
                FSA fsa = FSA.read();
                val machine = new Machine(dataSaver);
                while (fsa.doRead()
                        && errors < MAX_ERRORS
                        && transactions < 200) {
                    transactions++;
                    val res = ts.readFromLinkLayer();
                    if (d) log("link layer read: " + HexDump.dumpHexString(res));
                    val payload = ph.extractInnerPacket(res, true);
                    if (payload != null) {
                        fsa = machine.processPayload(payload);
                        UserError.Log.d(TAG, "Got fsa action: " + fsa.action);
                        switch (fsa.action) {

                            case WRITE_READ:
                                ph.writeInnerPacket(fsa.payload);
                                break;

                            case DONE:
                                UserError.Log.d(TAG, "All done");
                                return true;
                        }
                    } else {
                        errors++;
                        UserError.Log.d(TAG, "Read cycle got null errors @ " + errors);
                    } // if payload
                } // end while

                if (fsa.doRead()) {
                    UserError.Log.d(TAG, "Overall failure to read");
                    isoDep.close();
                    return false;
                }

                isoDep.close();
                return true;
            }
            isoDep.close();
        } catch (IOException e) {
            UserError.Log.d(TAG, "Could not connect: " + e);
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Got crash in handler: " + e);
            UserError.Log.e(TAG, "Got crash in handler: ", e);
            e.printStackTrace();
        }
        return false;
    }

}
