package com.eveningoutpost.dexdrip.watch.thinjam;

// jamorham

import com.eveningoutpost.dexdrip.Models.UserError;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_TEXTBOX1;

public class BlueJayAPI {

    public static final String TEXT_FIT = "TEXT_FIT";

    private static final String TAG = "BlueJayAPI";

    public static void processAPI(final String command, final String parameter, final byte[] bytes) {
        if (BlueJay.remoteApiEnabled()) {
            switch (command) {
                case TEXT_FIT:
                    BlueJayEntry.sendNotifyIfEnabled(THINJAM_NOTIFY_TYPE_TEXTBOX1, parameter);
                    break;
                default:
                    UserError.Log.d(TAG, "Unknown command: " + command);
            }
        } else {
            UserError.Log.e(TAG, "Ignoring API command as remote API feature is disabled");
        }
    }
}
