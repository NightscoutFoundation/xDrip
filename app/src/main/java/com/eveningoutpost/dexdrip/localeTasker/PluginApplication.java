/*
 * Copyright 2013 two forty four a.m. LLC <http://www.twofortyfouram.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.eveningoutpost.dexdrip.localeTasker;

import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;
import android.util.Log;

import com.eveningoutpost.dexdrip.BuildConfig;

/**
 * Implements an application object for the plug-in.
 * <p>
 * This application is non-essential for the plug-in's operation; it simply enables debugging options globally
 * for the app.
 */
public final class PluginApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        if (BuildConfig.DEBUG)
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, "Application is debuggable.  Enabling additional debug logging"); //$NON-NLS-1$
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
            {
                enableApiLevel9Debugging();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            {
                enableApiLevel11Debugging();
            }

            /*
             * If using the Fragment compatibility library, enable debug logging here
             */
            // android.support.v4.app.FragmentManager.enableDebugLogging(true);
            // android.support.v4.app.LoaderManager.enableDebugLogging(true);
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static void enableApiLevel9Debugging()
    {
        android.os.StrictMode.enableDefaults();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void enableApiLevel11Debugging()
    {
        android.app.LoaderManager.enableDebugLogging(true);
        android.app.FragmentManager.enableDebugLogging(true);
    }
}