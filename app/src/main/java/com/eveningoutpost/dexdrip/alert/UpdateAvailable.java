package com.eveningoutpost.dexdrip.alert;

import static com.eveningoutpost.dexdrip.models.JoH.cancelNotification;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalarNatural;
import static com.eveningoutpost.dexdrip.models.JoH.showNotification;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SENSORY_EXPIRY_NOTIFICATION_ID;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.XDRIP_UPDATE_NOTIFICATION_ID;
import static com.eveningoutpost.dexdrip.utilitymodels.UpdateActivity.AUTO_UPDATE_PREFS_NAME;

import android.app.PendingIntent;
import android.content.Intent;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.g5model.SensorDays;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.UpdateActivity;
import com.eveningoutpost.dexdrip.xdrip;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * Update available alert. Triggers when an update has been flagged as available
 */

public class UpdateAvailable extends BaseAlert {

    private static final String TAG = UpdateAvailable.class.getSimpleName();

    public static final String XDRIP_UPDATE_NOTIFICATION_PENDING = "xdrip-update-notification-pending";
    public static final String XDRIP_UPDATE_RATELIMIT = "xdrip-update-notification-ratelimit";

    public UpdateAvailable() {
        super("Update Available", When.ChargeChange, When.ScreenOn);
    }

    @Override
    public boolean activate() {
        val context = xdrip.getAppContext();
        val notificationId = XDRIP_UPDATE_NOTIFICATION_ID;
        cancelNotification(notificationId);

        val intent = new Intent(context, UpdateActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        val channel = Pref.getString("update_channel", "beta"); // get the current update channel
        showNotification(xdrip.gs(R.string.xdrip_update), xdrip.gs(R.string.a_new_version_on_channel_1_s_is_available, channel), pendingIntent, notificationId, null, true, true, null, null, null, false);
        return true;
    }

    @Override
    public boolean isMet() {
        return PersistentStore.getBoolean(XDRIP_UPDATE_NOTIFICATION_PENDING, false)
                && Pref.getBoolean(AUTO_UPDATE_PREFS_NAME, true)
                && JoH.pratelimit(XDRIP_UPDATE_RATELIMIT, 86400 * 6); // only once per 6 days
    }

}
