package com.eveningoutpost.dexdrip.utilitymodels;

import android.app.Service;
import android.content.Context;


/**
 * jamorham
 *
 * On Android Wear this code has no effect and is here just for code compatibility
 *
 */
public class ForegroundServiceStarter {


    public ForegroundServiceStarter(Context context, Service service) {

    }

    public void start() {

    }

    public void stop() {

    }

    public static boolean shouldRunCollectorInForeground() {
        return false; // we may need to revisit this if there are performance issues on android 8 on wear
    }

}

