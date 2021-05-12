package com.eveningoutpost.dexdrip;

import android.app.Application;

import com.activeandroid.ActiveAndroid;

import org.robolectric.RuntimeEnvironment;

/**
 * Created by jamorham on 01/10/2017.
 * <p>
 * Skeleton Application class
 * Just to allow ActiveAndroid to be initialized inside RoboElectric
 * as it depends on Android Framework
 */

public class TestingApplication extends Application {
    @Override
    public void onCreate() {
        xdrip.checkAppContext(RuntimeEnvironment.application);
        super.onCreate();
        ActiveAndroid.initialize(this);
    }

    @Override
    public void onTerminate() {
        try {
            ActiveAndroid.getDatabase().close();
        } catch (Exception e) {
            System.out.println("Exception stopping database: " + e);
        }
    }
}