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
 *
 * Additions by jamorham to facilitate tasker plugin interface for xDrip
 *
 */

package com.eveningoutpost.dexdrip.localeTasker.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.SnoozeActivity;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.localeTasker.Constants;
import com.eveningoutpost.dexdrip.localeTasker.bundle.BundleScrubber;
import com.eveningoutpost.dexdrip.localeTasker.bundle.PluginBundleManager;
import com.eveningoutpost.dexdrip.localeTasker.ui.EditActivity;
import com.eveningoutpost.dexdrip.utils.BgToSpeech;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.Arrays;
import java.util.Locale;

import static com.eveningoutpost.dexdrip.utils.BgToSpeech.BG_TO_SPEECH_PREF;


/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in setting.
 *
 * @see com.twofortyfouram.locale.Intent#ACTION_FIRE_SETTING
 * @see com.twofortyfouram.locale.Intent#EXTRA_BUNDLE
 */
public final class FireReceiver extends BroadcastReceiver {

    /**
     * @param context {@inheritDoc}.
     * @param intent the incoming {@link com.twofortyfouram.locale.Intent#ACTION_FIRE_SETTING} Intent. This
     * should contain the {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} that was saved by
     * {@link EditActivity} and later broadcast by Locale.
     */
    private final String TAG = FireReceiver.class.getSimpleName();


    @Override
    public void onReceive(final Context context, final Intent intent) {
        /*
         * Always be strict on input parameters! A malicious third-party app could send a malformed Intent.
         */

       final PowerManager.WakeLock wl = JoH.getWakeLock("xdrip-tasker-onreceiver", 60000);
        try {

            if (!com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
                if (Constants.IS_LOGGABLE) {
                    Log.e(Constants.LOG_TAG,
                            String.format(Locale.US, "Received unexpected Intent action %s", intent.getAction())); //$NON-NLS-1$
                }
                JoH.releaseWakeLock(wl);
                return;
            }

            BundleScrubber.scrub(intent);

            final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
            BundleScrubber.scrub(bundle);

            if (PluginBundleManager.isBundleValid(bundle)) {
                // We receive a space delimited string message from Tasker in the format
                // COMMAND PARAM1 PARAM2 etc..

                final String message = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE);

                if ((message != null) && !message.isEmpty()) {
                    final String[] message_array = message.split("\\s+"); // split by space
                    Log.d(TAG,"Received tasker message: "+message);

                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());

                    // Commands recognised:
                    //

                    // CAL: BG: -> Calibrate from a historical blood glucose reading

                    switch (message_array[0].toUpperCase()) {

                        case "CAL":
                        case "BG":
                        case "CAL:":
                        case "BG:":

                            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                            // format = BG BGAGE
                            // bg in whatever format the app is using mmol/l or mg/dl
                            // needs sanity check and potential standardizing

                            // bgage is positive age ago of bg test reading in seconds (minus applied later)

                            // We push the values to the Calibration Activity
                            if (message_array.length < 3) {
                                Log.e(TAG, "Not enough parameters for BG message");
                                break;
                            }
                            final Intent calintent = new Intent();
                            calintent.setClassName(xdrip.getAppContext().getString(R.string.local_target_package), "com.eveningoutpost.dexdrip.AddCalibration");
                            calintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            calintent.putExtra("timestamp", JoH.tsl());
                            calintent.putExtra("bg_string", message_array[1]);
                            calintent.putExtra("bg_age", message_array[2]);
                            calintent.putExtra("from_external", "true");
                            calintent.putExtra("cal_source", "FireReceiver");
                            context.startActivity(calintent);

                            break;


                        case "ALERT":
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show();

                            // Modify Alerts

                            break;

                        // VOLUME set volume profile, eg

                        /*  <item>High</item>
                            <item>medium</item>
                            <item>ascending</item>
                            <item>vibrate only</item>
                            <item>Silent</item>
                        */

                        // Case sensitive so be careful

                        case "VOLUME":
                            final String[] volumeArray = xdrip.getAppContext().getResources().getStringArray(R.array.BgAlertProfileValues);
                            if (message_array.length > 1) {
                                if (Arrays.asList(volumeArray).contains(message_array[1])) {
                                    Pref.setString("bg_alert_profile", message_array[1]);
                                    JoH.static_toast_long("Volume Profile changed by Tasker to: "+message_array[1]);
                                } else {
                                    JoH.static_toast_long("Invalid volume parameter: one of: " + Arrays.asList(volumeArray).toString());
                                }
                            } else {
                                JoH.static_toast_long("VOLUME command must be followed by volume profile name: "+Arrays.asList(volumeArray).toString());
                            }
                            break;

                        case "SNOOZE":
                            // default: snoozes the length of the current alert, or the default alert length
                            int minutes = -1;
                            if (message_array.length > 1) {
                                minutes = Integer.valueOf(message_array[1]);
                                JoH.static_toast_long("SNOOZE from Tasker for "+minutes+" min");
                            } else {
                                JoH.static_toast_long("SNOOZE from Tasker");
                            }
                            AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), minutes);
                            break;

                        case "SNOOZE_LOW":
                            // default: snoozes the length of the current alert, or the default alert length
                            minutes = AlertPlayer.getPlayer().GuessDefaultSnoozeTime();

                            // If a specific number of minutes is given, use it.
                            // If -1, enables this alert indefinitely.
                            // If 0, disables an already configured alert.
                            if (message_array.length > 1) {
                                minutes = Integer.valueOf(message_array[1]);
                            }

                            if (minutes == -1) {
                                JoH.static_toast_long("SNOOZE_LOW from Tasker enabled indefinitely (until disabled)");
                            } else if (minutes == 0) {
                                JoH.static_toast_long("SNOOZE_LOW from Tasker disabled");
                            } else {
                                JoH.static_toast_long("SNOOZE_LOW from Tasker for " + minutes + " min");
                            }

                            SnoozeActivity.snoozeForType(minutes, SnoozeActivity.SnoozeType.LOW_ALERTS, prefs);
                            break;

                        case "SNOOZE_HIGH":
                            // default: snoozes the length of the current alert, or the default alert length
                            minutes = AlertPlayer.getPlayer().GuessDefaultSnoozeTime();

                            // If a specific number of minutes is given, use it.
                            // If -1, enables this alert indefinitely.
                            // If 0, disables an already configured alert.
                            if (message_array.length > 1) {
                                minutes = Integer.valueOf(message_array[1]);
                            }

                            if (minutes == -1) {
                                JoH.static_toast_long("SNOOZE_HIGH from Tasker enabled indefinitely (until disabled)");
                            } else if (minutes == 0) {
                                JoH.static_toast_long("SNOOZE_HIGH from Tasker disabled");
                            } else {
                                JoH.static_toast_long("SNOOZE_HIGH from Tasker for " + minutes + " min");
                            }

                            SnoozeActivity.snoozeForType(minutes, SnoozeActivity.SnoozeType.HIGH_ALERTS, prefs);
                            break;

                            //opportunistic snooze that only does anything if an alert is active
                        case "OSNOOZE":
                            AlertPlayer.getPlayer().OpportunisticSnooze();
                            break;

                        case "RESTART":
                            if (message_array.length> 1) {
                                switch (message_array[1].toUpperCase()) {
                                    case "COLLECTOR":
                                        CollectionServiceStarter.restartCollectionService(xdrip.getAppContext());
                                        JoH.static_toast_long("Collector restarted by Tasker");
                                        break;
                                    case "APP":
                                        JoH.static_toast_long("xDrip restarted by Tasker");
                                        JoH.hardReset();
                                        break;
                                    default:
                                        JoH.static_toast_long("Unknown parameter to tasker restart command");
                                }
                            }
                           break;

                        case "SPEAK":
                            if (message_array.length > 1) {
                                switch (message_array[1].toUpperCase()) {
                                    case "NOW":
                                        BgToSpeech.speakNow(0);
                                        JoH.static_toast_long("Speak Now by Tasker");
                                        break;
                                    case "ON":
                                        Pref.setBoolean(BG_TO_SPEECH_PREF, true);
                                        JoH.static_toast_long("Speech On by Tasker");
                                        break;
                                    case "OFF":
                                        Pref.setBoolean(BG_TO_SPEECH_PREF, false);
                                        JoH.static_toast_long("Speech Off by Tasker");
                                        break;
                                    case "ALERTON":
                                        Pref.setBoolean("speak_alerts", true);
                                        JoH.static_toast_long("Speech Alert On by Tasker");
                                        break;
                                    case "ALERTOFF":
                                        Pref.setBoolean("speak_alerts", false);
                                        JoH.static_toast_long("Speech Alert Off by Tasker");
                                        break;
                                    default:
                                        JoH.static_toast_long("Unknown parameter to tasker speak command");
                                }
                            }
                            break;



//                    case "PREFS":
//
//                        SharedPreferences prefs = getMultiProcessSharedPreferences(context);
//                        if (prefs==null) {
//
//                            Toast.makeText(context, "xDrip tasker - Cannot get access to Preferences", Toast.LENGTH_LONG).show();
//                            return;
//                        }
//
//
//                        switch (message_array[1]) {
//
//                            case "B":
//                                // needs boolean type handler
//                                break;
//                            case "S":
//                                // string type handler
//                                if ((message_array[2]==null) || (message_array[3] == null)) {
//                                    Toast.makeText(context, "xDrip tasker - Blank parameter passed to string preferences", Toast.LENGTH_LONG).show();
//                                    return;
//                                }
//                                // Toast.makeText(context, "SET PREF STRING: " + message_array[2] + " to " + message_array[3], Toast.LENGTH_SHORT).show();
//                                Log.e(TAG,"firereceiver - About to write prefs");
//                                prefs.edit().putString(message_array[2], message_array[3]).commit();
//                                // If preferences screen is actually open right now then shut it down to avoid cache errors
//                                Intent fintent = new Intent(context.getString(R.string.finish_preferences_activity));
//                                context.sendBroadcast(fintent);
//                                prefs.edit().putString(message_array[2],message_array[3]).commit(); // save again
//                                Log.e(TAG,"firereceiver about to restart collector");
//                                // blanket restart
//                                try {
//                                    CollectionServiceStarter.restartCollectionService(context.getApplicationContext());
//                                } catch (Exception e) {
//
//                                    Log.e(TAG,"fireReceiver: error restarting collectionservice "+e.toString());
//                                }
//
//                                break;
//                            default:
//                                Toast.makeText(context, "Unknown xDrip Tasker prefs type "+message_array[1], Toast.LENGTH_LONG).show();
//
//                                break;
//                        }
//
//                        break;
                        default:
                            Toast.makeText(context, "Unknown xDrip first Tasker parameter " + message_array[0], Toast.LENGTH_LONG).show();
                            break;
                    }

                } else {
                    Log.e(TAG,"Message is empty!");
                }

            } else {
                Log.e(TAG,"Bundle is invalid!");
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }


}