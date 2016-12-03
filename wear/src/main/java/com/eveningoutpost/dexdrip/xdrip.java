package com.eveningoutpost.dexdrip;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;
import com.bugfender.sdk.Bugfender;

import java.util.Locale;

//import io.fabric.sdk.android.Fabric;

/**
 * Created by stephenblack on 3/21/15.
 */

public class xdrip extends Application {

    private static final String TAG = "xdrip.java";
    private static Context context;
    private static boolean fabricInited = false;
    private static boolean bfInited = false;
    public static boolean useBF = false;

    @Override
    public void onCreate() {
        xdrip.context = getApplicationContext();
        super.onCreate();

     }

    public synchronized static void initBF() {
        try {
            if (PreferenceManager.getDefaultSharedPreferences(xdrip.context).getBoolean("enable_bugfender", false)) {
                new Thread() {
                    @Override
                    public void run() {
                        String app_id = PreferenceManager.getDefaultSharedPreferences(xdrip.context).getString("bugfender_appid", "").trim();
                        if (!useBF && (app_id.length() > 10)) {
                            if (!bfInited) {
                                Bugfender.init(xdrip.context, app_id, BuildConfig.DEBUG);
                                bfInited = true;
                            }
                            useBF = true;
                        }
                    }
                }.start();
            } else {
                useBF = false;
            }
        } catch (Exception e) {
            //
        }
    }

    public static Context getAppContext() {
        return xdrip.context;
    }

    public static boolean checkAppContext(Context context) {
        if (getAppContext() == null) {
            xdrip.context = context;
            return false;
        } else {
            return true;
        }
    }
}
