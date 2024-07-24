package com.eveningoutpost.dexdrip.watch.thinjam;

// jamorham

import com.eveningoutpost.dexdrip.models.UserError;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_DIALOG;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_OTHER_ALERT;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_TEXTBOX1;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_TEXTBOX2;

public class BlueJayAPI {

    public static final String TEXT_FIT = "TEXT_FIT";
    public static final String TEXT_FIT2 = "TEXT_FIT2";
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String DIALOG = "DIALOG";
    public static final String MONO_PNG = "MONO_PNG";
    public static final String COLOUR_PNG = "COLOUR_PNG";

    private static final String TAG = "BlueJayAPI";

    public static void processAPI(final String command, final String parameter, final byte[] bytes) {
        if (BlueJay.remoteApiEnabled()) {
            switch (command) {
                case TEXT_FIT:
                    BlueJayEntry.sendNotifyIfEnabled(THINJAM_NOTIFY_TYPE_TEXTBOX1, parameter);
                    break;
                case TEXT_FIT2:
                    BlueJayEntry.sendNotifyIfEnabled(THINJAM_NOTIFY_TYPE_TEXTBOX2, parameter);
                    break;
                case NOTIFICATION:
                    BlueJayEntry.sendNotifyIfEnabled(THINJAM_NOTIFY_TYPE_OTHER_ALERT, parameter);
                    break;
                case DIALOG:
                    BlueJayEntry.sendNotifyIfEnabled(THINJAM_NOTIFY_TYPE_DIALOG, parameter);
                    break;
                case COLOUR_PNG:
                    BlueJayEntry.sendPngIfEnabled(bytes, parameter, "colour");
                    break;
                case MONO_PNG:
                    BlueJayEntry.sendPngIfEnabled(bytes, parameter, "mono");
                    break;
                default:
                    UserError.Log.d(TAG, "Unknown command: " + command);
            }
        } else {
            UserError.Log.e(TAG, "Ignoring API command as remote API feature is disabled");
        }
    }
}
