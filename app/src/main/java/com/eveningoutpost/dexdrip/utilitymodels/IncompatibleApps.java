package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Context;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.List;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.INCOMPATIBLE_BASE_ID;

import lombok.val;

/**
 * Created by jamorham on 01/11/2017.
 */

public class IncompatibleApps {

    public final static String TAG = IncompatibleApps.class.getSimpleName();

    private static final String NOTIFY_MARKER = "-NOTIFY";
    private static final int RENOTIFY_TIME = 86400 * 30;

    public static void notifyAboutIncompatibleApps() {
        final Context context = xdrip.getAppContext();
        int id = INCOMPATIBLE_BASE_ID;
        String package_name;


        package_name = "com.ambrosia.linkblucon";
        if (InstalledApps.checkPackageExists(context, package_name)) {
            if (JoH.pratelimit(package_name + NOTIFY_MARKER, RENOTIFY_TIME)) {
                id = notify(context.getString(R.string.blukon), package_name, xdrip.getAppContext().getString(R.string.offical_msg) + " " + xdrip.getAppContext().getString(R.string.use_conflict_msg), id);
            }
        }


        package_name = "it.ct.glicemia";
        if (InstalledApps.checkPackageExists(context, package_name)) {
            if (JoH.pratelimit(package_name + NOTIFY_MARKER, RENOTIFY_TIME)) {
                id = notify("Glimp", package_name, "Glimp" + " " + xdrip.getAppContext().getString(R.string.use_conflict_msg) + "\n\n" + xdrip.getAppContext().getString(R.string.use_confict_msg_glimp), id);
            }
        }

        final List<String> medtrumApps = new ArrayList<>();
        medtrumApps.add("com.medtrum.easysenseforandroidmmol");
        medtrumApps.add("com.medtrum.easysenseforandroidmgdl");
        medtrumApps.add("com.medtrum.easysenseforandroid");

        for (String package_name_medtrum : medtrumApps) {
            if (InstalledApps.checkPackageExists(context, package_name_medtrum)) {
                if (JoH.pratelimit(package_name_medtrum + NOTIFY_MARKER, RENOTIFY_TIME)) {
                    id = notify("EasySense", package_name_medtrum, "EasySense" + " " + xdrip.getAppContext().getString(R.string.use_conflict_msg), id);
                }
            }
        }

        final List<String> speedApps = new ArrayList<>();
        speedApps.add("com.mediatek.duraspeed");
        for (val app : speedApps) {
            if (InstalledApps.checkPackageExists(context, app)) {
                if (JoH.pratelimit(app + NOTIFY_MARKER, RENOTIFY_TIME)) {
                    id = notify2("Nasty Power Manager", app, "DuraSpeed" + " " + xdrip.getAppContext().getString(R.string.aggressive_power_manager), id);
                }
            }
        }
    }

    private static int notify(String short_name, String package_string, String msg, int id) {
        JoH.showNotification("Incompatible App " + short_name, "Please uninstall or disable " + package_string, null, id, true, true, null, null, ((msg.length() > 0) ? msg + "\n\n" : "") + "Another installed app may be incompatible with xDrip. The other app should be uninstalled or disabled to prevent conflicts with shared resources.\nThe package identifier is: " + package_string);
        UserError.Log.uel(TAG, "Please uninstall or disable package");
        return id + 1;
    }

    private static int notify2(String short_name, String package_string, String msg, int id) {
        JoH.showNotification(short_name, msg, null, id, true, true, null, null, ((msg.length() > 0) ? msg + "\n\n" : "") + "The Package identifier is: " + package_string);
        UserError.Log.uel(TAG, "Remove xDrip from duraspeed");
        return id + 1;
    }


}
