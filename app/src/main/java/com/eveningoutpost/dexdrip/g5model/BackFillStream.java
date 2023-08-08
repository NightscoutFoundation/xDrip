package com.eveningoutpost.dexdrip.g5model;

// created by jamorham

import static com.eveningoutpost.dexdrip.g5model.DexTimeKeeper.fromDexTimeCached;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

public class BackFillStream extends BaseMessage {

    private volatile int last_sequence = 0;
    private volatile boolean locked = false;

    public BackFillStream() {
        data = ByteBuffer.allocate(1000);
        data.order(ByteOrder.LITTLE_ENDIAN);
    }

    public synchronized void push(byte[] packet) {

        if (packet == null) return;
        final int this_sequence = (int) packet[0];
        if (this_sequence == last_sequence + 1) {
            last_sequence++;

            for (int i = 2; i < packet.length; i++) {
                if (data.position() < data.limit()) {
                    data.put(packet[i]);
                } else {
                    UserError.Log.wtf(TAG, "Reached limit for backfill stream size");
                }
            }
        } else {
            UserError.Log.wtf(TAG, "Received backfill packet out of sequence: " + this_sequence + " vs " + (last_sequence + 1));
        }
    }

    public synchronized void pushNew(final byte[] packet) {
        if (packet == null) return;
        if (locked) {
            UserError.Log.d(TAG, "Locked stream so ignoring");
            return;
        }
        if (packet.length == 9) {
            last_sequence = -1;
            data.put(packet);
        } else {
            if (last_sequence == 0) {
                if (packet.length > 17) {
                    if (packet[5] != 0x00 || packet[9] != 0x00 || packet[17] != 0x00) {
                        UserError.Log.d(TAG, "Non backfill data received - locking stream");
                        locked = true;
                        return;
                    }
                }
            }
            push(packet);
        }
    }

    public void reset() {
        data.clear();
        last_sequence = 0;
        locked = false;
    }


    public List<Backsie> decode() {
        final List<Backsie> backsies = new LinkedList<>();

        int extent = data.position();
        data.rewind();
        if (last_sequence >= 0) {
            final int length = data.getInt();
        }
        // TODO check length
        try {
            while (data.position() < extent) {
                final int dexTime = data.getInt();
                final int glucose = data.getShort();
                final byte type = data.get();
                if (last_sequence < 0) {
                    final byte extra = data.get();
                }
                final byte trend = data.get();

                final CalibrationState state = CalibrationState.parse(type);

                switch (state) {
                    case Ok:
                    case NeedsCalibration:
                        insertBackfillItem(backsies, dexTime, glucose, trend);
                        break;

                    case WarmingUp:
                        break;

                    case Errors:
                    /* This preference option has never been available outside of unit testing
                       and can now be removed.
                    if (Pref.getBooleanDefaultFalse("ob1_g5_use_errored_data")) {
                        insertBackfillItem(backsies, dexTime, glucose, trend);
                    }
                    */
                        break;

                    case InsufficientCalibration:
                        if (Pref.getBoolean("ob1_g5_use_insufficiently_calibrated", true)) {
                            insertBackfillItem(backsies, dexTime, glucose, trend);
                        }
                        break;

                    case NeedsFirstCalibration:
                    case NeedsSecondCalibration:
                    case Unknown:
                        break;

                    default:
                        UserError.Log.wtf(TAG, "Encountered backfill data we don't recognise: " + type + " " + glucose + " " + trend + " " + " " + JoH.dateTimeText(fromDexTimeCached(dexTime)));
                        break;

                }
            }
        } catch (final BufferUnderflowException e) {
            UserError.Log.e(TAG, "Got mismatched buffer when decoding: " + e);
        }
        return backsies;


    }

    private void insertBackfillItem(List<Backsie> backsies, int dexTime, int glucose, byte trend) {
        if (dexTime != 0) {
            backsies.add(new Backsie(glucose, trend, dexTime));
        }
    }

    public void enumerate(int size) {

        System.out.println("Size:" + size);
        byte[] output = data.array();
        int i = 4;
        while (i < data.position()) {
            if ((i - 4) % size == 0) {
                System.out.println("");
            }
            System.out.print(JoH.bytesToHex(new byte[]{output[i]}));
            i++;
        }
        System.out.println("\n");
    }

    @Data
    @AllArgsConstructor()
    public class Backsie {
        private final int glucose;
        private final int trend;
        private final int dextime;
    }

}
