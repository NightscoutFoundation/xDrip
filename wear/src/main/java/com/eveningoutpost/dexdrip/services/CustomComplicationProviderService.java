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

package com.eveningoutpost.dexdrip.services;

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
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder.unitizedDeltaString;

/**
 * Example watch face complication data provider provides a number that can be incremented on tap.
 */
public class CustomComplicationProviderService extends ComplicationProviderService {

    private static final String TAG = "ComplicationProvider";
    private static final long STALE_MS = Constants.MINUTE_IN_MS * 15;
    private static final long FRESH_MS = Constants.MINUTE_IN_MS * 5;

    enum COMPLICATION_STATE {
        DELTA(0),
        AGO(1),
        RESET(2);

        private int enum_value;

        COMPLICATION_STATE(int value) {
            this.enum_value = value;
        }

        public int getValue() {
            return enum_value;
        }

        public static COMPLICATION_STATE get_enum(int value) {
            for (COMPLICATION_STATE state : COMPLICATION_STATE.values()) {
                if (state.getValue() == value) return state;
            }
            return null;
        }
    }


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
        final PendingIntent complicationPendingIntent =
                ComplicationTapBroadcastReceiver.getToggleIntent(
                        this, thisProvider, complicationId);

        String numberText = "";
        BgReading bgReading = BgReading.last(true);
        if ((bgReading == null) || (JoH.msSince(bgReading.timestamp) >= FRESH_MS)) {
            try {
                ActiveAndroid.clearCache(); // we may be in another process!
            } catch (Exception e) {
                Log.d(TAG, "Couldn't clear cache: " + e);
            }
            bgReading = BgReading.last(true);
        }

        boolean is_stale = false;

        if (bgReading == null) {
            numberText = "null";
        } else {
            if (JoH.msSince(bgReading.timestamp) < STALE_MS) {
                numberText = bgReading.displayValue(this) + " " + bgReading.displaySlopeArrow();
            } else {
                numberText = "old " + niceTimeSinceBgReading(bgReading);
                is_stale = true;
            }
        }

        Log.d(TAG, "Returning complication text: " + numberText);

        COMPLICATION_STATE state = COMPLICATION_STATE.get_enum((int) PersistentStore.getLong(ComplicationTapBroadcastReceiver.COMPLICATION_STORE));
        if (state == null) state = COMPLICATION_STATE.DELTA;

        ComplicationData complicationData = null;

        switch (dataType) {
            case ComplicationData.TYPE_SHORT_TEXT:

                final ComplicationData.Builder builder = new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText(numberText))
                        .setTapAction(complicationPendingIntent);

                UserError.Log.d(TAG, "TYPE_SHORT_TEXT Current complication state:" + state);
                switch (state) {
                    case DELTA:
                        builder.setShortTitle(ComplicationText.plainText(getDeltaText(bgReading, is_stale)));
                        break;
                    case AGO:
                        builder.setShortTitle(ComplicationText.plainText(niceTimeSinceBgReading(bgReading)));
                        break;
                    default:
                        builder.setShortTitle(ComplicationText.plainText("ERR!"));
                }

                complicationData = builder.build();
                break;
            case ComplicationData.TYPE_LONG_TEXT:
                String numberTextLong = numberText + " " + getDeltaText(bgReading, is_stale) + " (" + niceTimeSinceBgReading(bgReading) + ")";
                Log.d(TAG, "Returning complication text Long: " + numberTextLong);

                //Loop status by @gregorybel
                String externalStatusString = PersistentStore.getString("remote-status-string");
                Log.d(TAG, "Returning complication status: " + externalStatusString);

                final ComplicationData.Builder builderLong = new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                        .setLongTitle(ComplicationText.plainText(numberTextLong))
                        .setLongText(ComplicationText.plainText(externalStatusString))
                        .setTapAction(complicationPendingIntent);

                UserError.Log.d(TAG, "TYPE_LONG_TEXT Current complication state:" + state);
                complicationData = builderLong.build();

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

    private static String niceTimeSinceBgReading(BgReading bgReading) {
        return bgReading != null ? JoH.niceTimeSince(bgReading.timestamp).replaceAll(" ", "").replaceAll("(^[0-9]+[a-zA-Z])[a-zA-Z]*$", "$1") : "";
    }

    private static String getDeltaText(BgReading bgReading, boolean is_stale) {
        final boolean doMgdl = Pref.getString("units", "mgdl").equals("mgdl");
        return (!is_stale ? (bgReading != null ? unitizedDeltaString(false, false, Home.get_follower(), doMgdl) : "null") : "");
    }

    /*
     * Called when the complication has been deactivated.
     */
    @Override
    public void onComplicationDeactivated(int complicationId) {
        Log.d(TAG, "onComplicationDeactivated(): " + complicationId);
    }

    public static void refresh() {
        Inevitable.task("refresh-complication", 500, new Runnable() {
            @Override
            public void run() {
                if (JoH.ratelimit("complication-refresh", 5)) {
                    Log.d(TAG, "Complication refresh() executing");
                    final ComponentName componentName = new ComponentName(xdrip.getAppContext(), "com.eveningoutpost.dexdrip.services.CustomComplicationProviderService");
                    final ProviderUpdateRequester providerUpdateRequester = new ProviderUpdateRequester(xdrip.getAppContext(), componentName);
                    providerUpdateRequester.requestUpdateAll();
                }
            }
        });
        Log.d(TAG, "Complication refresh() called");
    }
}
