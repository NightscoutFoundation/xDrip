/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// provided by lurosys

package com.eveningoutpost.dexdrip.Services;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder.unitizedDeltaString;

/**
 * Example watch face complication data provider provides a number that can be incremented on tap.
 */
public class CustomComplicationProviderService extends ComplicationProviderService {

    private static final String TAG = "ComplicationProvider";
    private static final long STALE_MS = Constants.MINUTE_IN_MS * 15;
    /*
     * Called when a complication has been activated. The method is for any one-time
     * (per complication) set-up.
     *
     * You can continue sending data for the active complicationId until onComplicationDeactivated()
     * is called.
     */
    @Override
    public void onComplicationActivated(
            int complicationId, int dataType, ComplicationManager complicationManager) {
        Log.d(TAG, "onComplicationActivated(): " + complicationId);

    }

    /*
     * Called when the complication needs updated data from your provider. There are four scenarios
     * when this will happen:
     *
     *   1. An active watch face complication is changed to use this provider
     *   2. A complication using this provider becomes active
     *   3. The period of time you specified in the manifest has elapsed (UPDATE_PERIOD_SECONDS)
     *   4. You triggered an update from your own class via the
     *       ProviderUpdateRequester.requestUpdate() method.
     */
    @Override
    public void onComplicationUpdate(
            int complicationId, int dataType, ComplicationManager complicationManager) {
        Log.d(TAG, "onComplicationUpdate() id: " + complicationId);
        // Create Tap Action so that the user can trigger an update by tapping the complication.
        final ComponentName thisProvider = new ComponentName(this, getClass());
        // We pass the complication id, so we can only update the specific complication tapped.
        PendingIntent complicationPendingIntent =
                ComplicationTapBroadcastReceiver.getToggleIntent(
                        this, thisProvider, complicationId);

        String numberText = "";
        BgReading bgReading = BgReading.last(false);
        if ((bgReading == null) || (JoH.msSince(bgReading.timestamp) >= STALE_MS)) {
            ActiveAndroid.clearCache(); // we may be in another process!
            bgReading = BgReading.last(false);
        }

        boolean is_stale = false;
        
        if (bgReading == null) {
            numberText = "null";
        } else {
            if (JoH.msSince(bgReading.timestamp) < STALE_MS) {
                numberText = bgReading.displayValue(this) + " " + bgReading.slopeArrow();
            } else {
                numberText = "old " + ((int) (JoH.msSince(bgReading.timestamp) / Constants.MINUTE_IN_MS));
                is_stale = true;
            }
        }
        Log.d(TAG, "Returning complication text: " + numberText);


        ComplicationData complicationData = null;

        final boolean doMgdl = Home.getPreferencesStringWithDefault("units", "mgdl").equals("mgdl");
        switch (dataType) {
            case ComplicationData.TYPE_SHORT_TEXT:
                complicationData =
                        new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                .setShortText(ComplicationText.plainText(numberText))
                                .setTapAction(complicationPendingIntent)
                                .setShortTitle(!is_stale ? (ComplicationText.plainText(bgReading != null ? unitizedDeltaString(false, false, Home.get_follower(), doMgdl) : "null")) : ComplicationText.plainText(""))
                                .build();
                break;
            default:
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Unexpected complication type " + dataType);
                }
        }

        if (complicationData != null) {
            complicationManager.updateComplicationData(complicationId, complicationData);

        } else {
            // If no data is sent, we still need to inform the ComplicationManager, so the update
            // job can finish and the wake lock isn't held any longer than necessary.
            complicationManager.noUpdateRequired(complicationId);
        }
    }

    /*
     * Called when the complication has been deactivated.
     */
    @Override
    public void onComplicationDeactivated(int complicationId) {
        Log.d(TAG, "onComplicationDeactivated(): " + complicationId);
    }

    public static void refresh() {
        if (JoH.ratelimit("complication-refresh", 5)) {
            final ComponentName componentName = new ComponentName(xdrip.getAppContext(), "com.eveningoutpost.dexdrip.Services.CustomComplicationProviderService");
            final ProviderUpdateRequester providerUpdateRequester = new ProviderUpdateRequester(xdrip.getAppContext(), componentName);
            providerUpdateRequester.requestUpdateAll();
        }
        Log.d(TAG, "Complication refresh() called");
    }
}
