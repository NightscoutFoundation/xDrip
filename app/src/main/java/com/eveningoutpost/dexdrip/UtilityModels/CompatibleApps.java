package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.G5BaseService;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.webservices.XdripWebService;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.UtilityModels.Constants.COMPATIBLE_BASE_ID;

/**
 * Created by jamorham on 01/11/2017.
 *
 * Prompt helpfully about other compatible apps within the device ecosystem.
 *
 */

public class CompatibleApps extends BroadcastReceiver {

    public static final String EXTERNAL_ALG_PACKAGES = "EXTERNAL_ALG_PACKAGES";

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
        } else {
            package_name = "com.fitbit.FitbitMobile";
            if (InstalledApps.checkPackageExists(context, package_name)) {
                if (!Pref.getBooleanDefaultFalse("xdrip_webservice")) {
                    if (JoH.pratelimit(package_name + NOTIFY_MARKER, RENOTIFY_TIME)) {
                        id = notify(gs(R.string.fitbit), gs(R.string.enable_local_web_server_feature_fitbit), id, Feature.ENABLE_FITBIT_FEATURES);
                    }
                }
            }
        }

        package_name = "com.google.android.wearable.app";
        if (InstalledApps.checkPackageExists(context, package_name)) {
            if (!Pref.getBooleanDefaultFalse("wear_sync")) {
                if (JoH.pratelimit(package_name + NOTIFY_MARKER, RENOTIFY_TIME)) {
                    id = notify(gs(R.string.androidwear), gs(R.string.enable_wear_os_sync), id, Feature.ENABLE_WEAR_OS_SYNC);
                }
            }
        }

        package_name = "info.nightscout.androidaps";
        if (InstalledApps.checkPackageExists(context, package_name)) {
            if (!Pref.getBooleanDefaultFalse("broadcast_data_through_intents")) {
                if (JoH.pratelimit(package_name + NOTIFY_MARKER, RENOTIFY_TIME)) {
                    id = notify(gs(R.string.androidaps), gs(R.string.enable_local_broadcast), id, Feature.ENABLE_ANDROIDAPS_FEATURE1);
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!Pref.getString("local_broadcast_specific_package_destination", "").contains(package_name)) {
                    if (JoH.pratelimit(package_name + NOTIFY_MARKER + "2", RENOTIFY_TIME)) {
                        id = notify(gs(R.string.androidaps), gs(R.string.broadcast_only_to), id, Feature.ENABLE_ANDROIDAPS_FEATURE2);
                    }
                }
            }
        }

        package_name = "net.dinglisch.android.tasker";
        if (InstalledApps.checkPackageExists(context, package_name) || InstalledApps.checkPackageExists(context, package_name + "m")) {
            if (!Pref.getString("local_broadcast_specific_package_destination", "").contains(package_name)) {
                if (JoH.pratelimit(package_name + NOTIFY_MARKER, RENOTIFY_TIME)) {
                    id = notify("Tasker", gs(R.string.enable_local_broadcast), id, Feature.ENABLE_TASKER);
                }
            }
        }

        package_name = "com.pimpimmobile.librealarm";
        if (InstalledApps.checkPackageExists(context, package_name)) {
            if (DexCollectionType.getDexCollectionType() != DexCollectionType.LibreAlarm) {
                if (JoH.pratelimit(package_name + NOTIFY_MARKER, RENOTIFY_TIME)) {
                    id = notify(gs(R.string.librealarm), gs(R.string.use_librealarm), id, Feature.ENABLE_LIBRE_ALARM);
                }
            }
        }

        if (!Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            final String[] oop_package_names = {"info.nightscout.deeplearning", "com.hg4.oopalgorithm.oopalgorithm", "org.andesite.lucky8"};
            final StringBuilder sb = new StringBuilder();
            for (String package_name_o : oop_package_names) {
                if (InstalledApps.checkPackageExists(context, package_name_o)) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(package_name_o);
                    if (JoH.pratelimit(package_name_o + NOTIFY_MARKER, RENOTIFY_TIME)) {
                        final String short_package = package_name_o.substring(package_name_o.lastIndexOf(".") + 1).toUpperCase();
                        id = notify(gs(R.string.external_calibration_app),
                                short_package + " " + gs(R.string.use_app_for_calibration),
                                id, Feature.ENABLE_OOP);
                    }
                }
            }
            if (sb.length() > 0) {
                PersistentStore.setString(EXTERNAL_ALG_PACKAGES, sb.toString());
            }
        }

        checkMemoryConstraints();

        // TODO add pebble

        // TODO add more here

    }


    private static void checkMemoryConstraints() {
        final ActivityManager actManager = (ActivityManager) xdrip.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
        final ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        final long totalMemory = memInfo.totalMem;
        // TODO react to total memory
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

    public static void showNotification(String title, String content, PendingIntent yesIntent, PendingIntent noIntent, PendingIntent contentIntent, int notificationId) {

        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(xdrip.getAppContext(), null)
                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.tick_icon_small, gs(R.string.yes), yesIntent)
                .addAction(android.R.drawable.ic_delete, gs(R.string.no), noIntent);

        final NotificationManager mNotifyMgr = (NotificationManager) xdrip.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (mNotifyMgr != null) {
            mNotifyMgr.notify(notificationId, XdripNotificationCompat.build(mBuilder));
        } else {
            JoH.static_toast_long("Cannot notify!");
        }
    }

    public static PendingIntent createActionIntent(int parent_id, int id, Feature action) {
        return PendingIntent.getBroadcast(xdrip.getAppContext(), id,
                new Intent(xdrip.getAppContext(), CompatibleApps.class)
                        .putExtra("action", action)
                        .putExtra("id", parent_id)
                        .putExtra("auth", BuildConfig.buildUUID),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent createChoiceIntent(int parent_id, int id, Feature action, String title, String msg) {
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

    private static void cancelSourceNotification(Intent intent) {
        JoH.cancelNotification(intent.getIntExtra("id", 555));
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
                        cancelSourceNotification(intent);
                        break;

                    case ENABLE_GARMIN_FEATURES:
                    case ENABLE_FITBIT_FEATURES:
                        enableBoolean("xdrip_webservice", "xDrip Web Service Enabled!", intent);
                        XdripWebService.immortality();
                        break;

                    case ENABLE_ANDROIDAPS_FEATURE1:
                        enableBoolean("broadcast_data_through_intents", "Local Broadcast Enabled!", intent);
                        break;

                    case ENABLE_ANDROIDAPS_FEATURE2:
                        final String msg = "Enabling broadcast to info.nightscout.androidaps !";
                        addStringtoSpaceDelimitedPreference("local_broadcast_specific_package_destination", "info.nightscout.androidaps");
                        JoH.static_toast_long(msg);
                        cancelSourceNotification(intent);
                        break;

                    case ENABLE_TASKER:
                        addStringtoSpaceDelimitedPreference("local_broadcast_specific_package_destination", "net.dinglisch.android.tasker net.dinglisch.android.taskerm");
                        JoH.static_toast_long("Setting specific package broadcast for Tasker");
                        cancelSourceNotification(intent);
                        break;

                    case ENABLE_LIBRE_ALARM:
                        DexCollectionType.setDexCollectionType(DexCollectionType.LibreAlarm);
                        cancelSourceNotification(intent);
                        break;

                    case ENABLE_OOP:
                        enableBoolean("external_blukon_algorithm", "Enabled External Calibration App!", intent);
                        break;

                    case ENABLE_WEAR_OS_SYNC:
                        enableBoolean("wear_sync", "Enabled Wear OS Sync!", intent);
                        break;

                    case HARD_RESET_TRANSMITTER:
                        G5BaseService.setHardResetTransmitterNow();
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

    private void enableBoolean(String id, String msg, Intent intent) {
        Pref.setBoolean(id, true);
        JoH.static_toast_long(msg);
        cancelSourceNotification(intent);
    }

    private void addStringtoSpaceDelimitedPreference(final String key, final String parameter) {

        String value = Pref.getString(key, "");
        if (value.length() > 3) {
            for (final String this_value : value.split(" ")) {
                if (this_value != null && this_value.length() > 3) {
                    if (this_value.equals(parameter)) {
                        return; //already present in string
                    }
                }
            }
            value += " " + parameter;
        } else {
            value = parameter;
        }
        Pref.setString(key, value);
    }

    public enum Feature {
        UNKNOWN,
        CHOICE,
        CANCEL,
        ENABLE_GARMIN_FEATURES,
        ENABLE_ANDROIDAPS_FEATURE1,
        ENABLE_ANDROIDAPS_FEATURE2,
        ENABLE_FITBIT_FEATURES,
        ENABLE_LIBRE_ALARM,
        ENABLE_OOP,
        ENABLE_WEAR_OS_SYNC,
        HARD_RESET_TRANSMITTER,
        ENABLE_TASKER,
        FEATURE_X
    }

}
