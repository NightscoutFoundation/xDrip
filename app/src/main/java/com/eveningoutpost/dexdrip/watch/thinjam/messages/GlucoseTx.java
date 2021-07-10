package com.eveningoutpost.dexdrip.watch.thinjam.messages;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.G5Model.CalibrationState;
import com.eveningoutpost.dexdrip.Models.BgReading;

import lombok.Getter;
import lombok.val;

import static com.eveningoutpost.dexdrip.Models.JoH.msSince;
import static com.eveningoutpost.dexdrip.UtilityModels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_INBOUND_GLUCOSE;

// jamorham

public class GlucoseTx extends BaseTx {

    private static final String TAG = "BlueJaySlope";

    @Getter
    private boolean valid;

    public GlucoseTx(final BgReading last) {
        init(OPCODE_INBOUND_GLUCOSE, 11);
        if (last != null) {
            if (last.source_info == null || !last.source_info.contains("BlueJay")) {
                val secondsAgo = (msSince(last.timestamp) / 1000);
                if (secondsAgo < 30000 && secondsAgo >= 0) {
                    data.putInt(0); // TODO proper UTC
                    data.putShort(((short) secondsAgo)); // check this would be unsigned
                    data.putShort((short) Math.round(last.getDg_mgdl()));
                    data.put((byte) 0); // status
                    // push the state if we have it otherwise just send ok as we must have got this data from elsewhere
                    //data.put(Ob1G5CollectionService.lastSensorState != null ? Ob1G5CollectionService.lastSensorState.getValue() : CalibrationState.Ok.getValue()); // state
                    data.put(CalibrationState.Ok.getValue()); // state hardcoded as okay due to circular update reference

                    // take slope from displayglucose as it handles zero slope situations much better
                    final BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
                    if (dg != null) {
                        int slope = (int) Math.round(dg.slope * MINUTE_IN_MS * 10);

                        if (slope < -126) slope = -126;
                        if (slope > 126) slope = 126;
                        if (last.hide_slope) {
                            slope = 127;
                        }
                        android.util.Log.d(TAG, "dg slope: " + (dg.slope * MINUTE_IN_MS) + " " + dg.delta_arrow + " hide slope:" + last.hide_slope + " slope byte: " + slope);
                        data.put((byte) slope); // trend
                    } else {
                        android.util.Log.d(TAG, "Couldn't get display glucose value");
                    }
                    valid = true;
                } else {
                    android.util.Log.d(TAG, "Last glucose seconds ago was invalid @ : " + secondsAgo);
                }
            }
        }
    }
}
