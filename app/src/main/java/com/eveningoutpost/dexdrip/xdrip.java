package com.eveningoutpost.dexdrip;

import android.app.Application;

/**
 * Created by stephenblack on 3/21/15.
 */

public class xdrip extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //Fabric.with(this, new Crashlytics());
    }
}
