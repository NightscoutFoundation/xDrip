package com.eveningoutpost.dexdrip.watch.thinjam.messages;

import android.util.SparseArray;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Unitized;
import com.eveningoutpost.dexdrip.watch.thinjam.Const;
import com.google.gson.annotations.Expose;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.val;

// jamorham

@RequiredArgsConstructor
public class PushRx extends BaseRx {

    public enum Type {

        Charging(Const.PUSH_OPCODE_CHARGE),
        LongPress1(Const.PUSH_OPCODE_B1_LONG),
        BackFill(Const.PUSH_OPCODE_BACKFILL),
        Choice(Const.PUSH_OPCODE_CHOICE);

        byte value;

        Type(byte val) {
            this.value = val;
        }

        private static final SparseArray<Type> lookup;

        static {
            lookup = new SparseArray<>();
            for (val v : values()) {
                lookup.put(v.value, v);
            }
        }
    }


    public static class BackFillRecord {

        public final int trend;
        public final long timestamp;
        public final int mgdl;

        BackFillRecord(final int secondsSince, final int mgdl, final int trend) {
            this.timestamp = JoH.tsl() - (secondsSince * Constants.SECOND_IN_MS);
            this.mgdl = mgdl;
            this.trend = trend;
        }

        public String toS() {
            return JoH.dateTimeText(timestamp) + " " + Unitized.unitized_string_static((double) mgdl) + " trend " + trend;
        }
    }

    @Expose
    public final Type type;
    @Expose
    public final int value;

    public String text;

    public List<BackFillRecord> backfills = null;

    public static boolean isPushMessage(final byte[] bytes) {
        return bytes != null && bytes.length >= 4 && bytes[0] == Const.OPCODE_PUSH_RX && bytes[1] == Const.OPCODE_PUSH_RX;
    }

    public static PushRx parse(final byte[] bytes) {
        if (isPushMessage(bytes)) {
            val type = Type.lookup.get(bytes[2]);
            if (type == null) return null;      // unrecognised sub opcode
            val push = new PushRx(type, bytes[3] & 0xFF);

            if (bytes.length > 4) {

                push.data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                val opcode = push.data.get();
                val opcode2 = push.data.get();
                val type2 = push.data.get();

                if (push.type == Type.BackFill) {
                    val numOfRecords = push.data.get();
                    push.backfills = new ArrayList<>(numOfRecords);
                    for (int i = 0; i < numOfRecords; i++) {
                        val trend = push.data.get();        // TODO validate sign etc
                        val secondsAgo = push.getUnsignedShort();
                        val mgdl = push.getUnsignedShort();
                        push.backfills.add(new BackFillRecord(secondsAgo, mgdl, trend));
                        //UserError.Log.d("BlueJayParse",push.backfills.get(push.backfills.size()-1).toS());
                    }
                } else if (push.type == Type.Choice) {
                    val choice = push.data.get();
                    val str = new byte[push.data.remaining()];
                    push.data.get(str);
                    try {
                        push.text = new String(str, "UTF-8").replace("\0", "");
                    } catch (UnsupportedEncodingException e) {
                        // should never happen
                    }
                }
            }
            return push;
        } else {
            return null;
        }
    }

    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }

}
