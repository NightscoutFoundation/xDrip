package com.eveningoutpost.dexdrip.utilitymodels;

import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry.isNative;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.eveningoutpost.dexdrip.models.UserError;

import java.util.List;

/**
 * Created by jamorham on 04/09/2017.
 */

public class InstalledApps {

    private static final String TAG = "InstalledApps";

    private static final String GOOGLE_PLAY_SERVICES_PACKAGE = "com.google.android.gms";

    public static boolean isGooglePlayInstalled(Context context) {
        return isNative() || checkPackageExists(context, GOOGLE_PLAY_SERVICES_PACKAGE);
    }

    public static boolean checkPackageExists(Context context, String packageName) {
        try {
            final PackageManager pm = context.getPackageManager();
            final PackageInfo pi = pm.getPackageInfo(packageName, 0);
            return pi.packageName.equals(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } catch (Exception e) {
            Log.wtf(TAG, "Exception trying to determine packages! " + e);
            return false;
        }
    }

    public static void getIntentActivities(Context context) {
        final PackageManager pm = context.getPackageManager();
        final Intent search = new Intent("android.intent.action.MAIN");
        final List<ResolveInfo> activityList = pm.queryIntentActivities(search, 0);
        for (final ResolveInfo app : activityList) {
            UserError.Log.d(TAG, app.activityInfo.packageName);
        }
    }

    private static void sendImplicitBroadcast(Context ctxt, Intent i) {
        final PackageManager pm = ctxt.getPackageManager();
        final List<ResolveInfo> matches = pm.queryBroadcastReceivers(i, 0);

        for (ResolveInfo resolveInfo : matches) {
            final Intent explicit = new Intent(i);
            ComponentName cn = new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, resolveInfo.activityInfo.name);
            explicit.setComponent(cn);
            ctxt.sendBroadcast(explicit);
        }
    }
}
