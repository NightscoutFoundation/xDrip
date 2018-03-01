package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.webservices.XdripWebService;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.UtilityModels.Constants.COMPATIBLE_BASE_ID;

/**
 * Created by jamorham on 01/11/2017.
 */

public class CompatibleApps extends BroadcastReceiver {

    private static final String NOTIFY_MARKER = "-NOTIFY";
    private static final int RENOTIFY_TIME = 86400 * 30;

    public static void notifyAboutCompatibleApps() {
        final Context context = xdrip.getAppContext();
        int id = COMPATIBLE_BASE_ID;
        String package_name;

        package_name = "com.garmin.android.apps.connectmobile";
        if (InstalledApps.checkPackageExists(context, package_name)) {
            if (!Pref.getBooleanDefaultFalse("xdrip_webservice")) {
                if (JoH.pratelimit(package_name + NOTIFY_MARKER, RENOTIFY_TIME)) {
                    id = notify(gs(R.string.garmin), gs(R.string.enable_local_web_server_feature), id, Feature.ENABLE_GARMIN_FEATURES);
                }
            }
        }

        // TODO add more here

    }

    private static String gs(int id) {
        return xdrip.getAppContext().getString(id);
    }

    private static int notify(String short_name, String msg, int id, Feature action) {
        final String title = xdrip.getAppContext().getString(R.string.xdrip_compatible_features, short_name);
        showNotification(title, msg,
                createActionIntent(id, id + 1, action),
                createActionIntent(id, id + 2, Feature.CANCEL),
                createChoiceIntent(id, id + 3, action, title, msg),
                id);
        return id + 4;
    }

    public static void showNotification(String title, String content, PendingIntent intent, PendingIntent intent2, PendingIntent contentIntent, int notificationId) {

        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(xdrip.getAppContext(), null)
                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.tick_icon_small, gs(R.string.yes), intent)
                .addAction(android.R.drawable.ic_delete, gs(R.string.no), intent2);

        final NotificationManager mNotifyMgr = (NotificationManager) xdrip.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (mNotifyMgr != null) {
            mNotifyMgr.notify(notificationId, XdripNotificationCompat.build(mBuilder));
        } else {
            JoH.static_toast_long("Cannot notify!");
        }
    }

    private static PendingIntent createActionIntent(int parent_id, int id, Feature action) {
        return PendingIntent.getBroadcast(xdrip.getAppContext(), id,
                new Intent(xdrip.getAppContext(), CompatibleApps.class)
                        .putExtra("action", action)
                        .putExtra("id", parent_id)
                        .putExtra("auth", BuildConfig.buildUUID),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent createChoiceIntent(int parent_id, int id, Feature action, String title, String msg) {
        return PendingIntent.getBroadcast(xdrip.getAppContext(), id,
                new Intent(xdrip.getAppContext(), CompatibleApps.class)
                        .putExtra("action", Feature.CHOICE)
                        .putExtra("choice", action)
                        .putExtra("id", parent_id)
                        .putExtra("title", title)
                        .putExtra("msg", msg)
                        .putExtra("auth", BuildConfig.buildUUID),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void showChoiceDialog(Activity activity, final Intent intent) {
        if (intent == null) return;
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(intent.getStringExtra("title"));
            builder.setMessage(intent.getStringExtra("msg"));
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    final PendingIntent pi = createActionIntent(intent.getIntExtra("id", 555),
                            intent.getIntExtra("id", 555) + 1,
                            (Feature) intent.getSerializableExtra("choice"));
                    try {
                        pi.send();
                    } catch (Exception e) {
                        //
                    }
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                }
            });

            builder.create().show();
        } catch (Exception e) {
            JoH.static_toast_long("Error: in compatible apps dialog");
        }

    }

    // handle incoming button pushes
    @Override
    public void onReceive(Context context, final Intent intent) {
        final String action = intent.getAction();
        // system package added
        if ((action != null) && (action.equals("android.intent.action.PACKAGE_ADDED"))) {
            if (JoH.ratelimit("package-added-check", 10)) {
                notifyAboutCompatibleApps();
                // TODO add incompatible app check also
            }
        } else {
            // internal pending intent from notification
            final String auth = intent.getStringExtra("auth");
            if ((auth != null) && (auth.equals(BuildConfig.buildUUID))) {
                context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                final Feature feature = (Feature) intent.getSerializableExtra("action");
                switch (feature) {
                    case CHOICE:
                        final Intent homeIntent = new Intent(context, Home.class);
                        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        homeIntent.putExtra("choice-intent", "jamorham");
                        homeIntent.putExtra("choice-intentx", intent);
                        context.startActivity(homeIntent);
                        break;
                    case CANCEL:
                        JoH.cancelNotification(intent.getIntExtra("id", 555));
                        break;


                    case ENABLE_GARMIN_FEATURES:
                        Pref.setBoolean("xdrip_webservice", true);
                        XdripWebService.immortality();
                        JoH.cancelNotification(intent.getIntExtra("id", 555));
                        JoH.static_toast_long("xDrip Web Service Enabled!");
                        break;


                    default:
                        JoH.static_toast_long("Unhandled action: " + feature);
                        break;
                }

            } else {
                JoH.static_toast_long("Invalid xDrip Authorization");
            }
        }
    }

    private enum Feature {
        UNKNOWN,
        CHOICE,
        CANCEL,
        ENABLE_GARMIN_FEATURES,
        FEATURE_X
    }

}
