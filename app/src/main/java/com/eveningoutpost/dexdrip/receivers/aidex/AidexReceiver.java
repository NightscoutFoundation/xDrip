package com.eveningoutpost.dexdrip.receivers.aidex;

import static com.eveningoutpost.dexdrip.receivers.aidex.AidexBroadcastIntents.AIDEX_BG_TYPE;
import static com.eveningoutpost.dexdrip.receivers.aidex.AidexBroadcastIntents.AIDEX_BG_VALUE;
import static com.eveningoutpost.dexdrip.receivers.aidex.AidexBroadcastIntents.AIDEX_SENSOR_ID;
import static com.eveningoutpost.dexdrip.receivers.aidex.AidexBroadcastIntents.AIDEX_TIMESTAMP;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;


/**
 * Created by andy.rozman on 12/03/2022.
 */
public class AidexReceiver extends BroadcastReceiver {

    private static final String TAG = "AidexReceiver";
    private static final String AIDEX_RECEIVER = "AidexReceiver";
    private static final boolean debug = true;
    private static final Object lock = new Object();
    private static SharedPreferences prefs;
    private static final long segmentation_timeslice = (long) (Constants.MINUTE_IN_MS * 4.5);

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DexCollectionType.getDexCollectionType() != DexCollectionType.AidexReceiver) {
            UserError.Log.w(TAG, "Received Aidex Broadcast, but AidexReceiver is not selected as collector.");
            return;
        }
        new Thread() {
            @Override
            public void run() {
                PowerManager.WakeLock wl = JoH.getWakeLock("aidex-receiver", 60000);
                synchronized (lock) {
                    try {
                        UserError.Log.d(TAG, "Aidex onReceiver: " + intent.getAction());
                        JoH.benchmark(null);

                        // check source
                        if (prefs == null)
                            prefs = PreferenceManager.getDefaultSharedPreferences(context);

                        final Bundle bundle = intent.getExtras();
                        final String action = intent.getAction();

                        if (bundle == null || action == null) {
                            UserError.Log.d(TAG, "Either bundle or action is null.");
                            return;
                        }

                        UserError.Log.d(TAG, "Action: " + action);

                        if (debug) {
                            JoH.dumpBundle(bundle, TAG);
                        }

                        switch (action) {

                            case AidexBroadcastIntents.ACTION_NEW_BG_ESTIMATE:
                                processNewBGEstimate(bundle);
                                break;

                            case AidexBroadcastIntents.ACTION_CALIBRATION:
                                processCalibration(bundle);
                                break;

                            case AidexBroadcastIntents.ACTION_SENSOR_NEW:
                                processSensorStart(bundle);
                                break;

                            case AidexBroadcastIntents.ACTION_SENSOR_RESTART:
                                processSensorRestart(bundle);
                                break;

                            case AidexBroadcastIntents.ACTION_SENSOR_STOP:
                                processSensorStop(bundle);
                                break;

                            case AidexBroadcastIntents.ACTION_NOTIFICATION:
                                processNotification(bundle);
                                break;

                            default:
                                UserError.Log.e(TAG, "Unknown action! " + action);
                                break;
                        }
                    } finally {
                        JoH.benchmark("Aidex process");
                        JoH.releaseWakeLock(wl);
                    }
                } // lock
            }

        }.start();

    }


    private void processNewBGEstimate(Bundle bundle) {

        if (!bundle.containsKey(AIDEX_TIMESTAMP) ||
                !bundle.containsKey(AIDEX_SENSOR_ID) ||
                !bundle.containsKey(AIDEX_BG_TYPE) ||
                !bundle.containsKey(AIDEX_BG_VALUE)
        ) {
            UserError.Log.e(TAG, "Aidex Broadcast for NewBGEstimate invalid, missing one of required parameters: SendorId, Timestamp, BgType or BgValue! Ignoring broadcast.");
            return;
        }

        String sensorId = bundle.getString(AIDEX_SENSOR_ID);
        long timeStamp = bundle.getLong(AIDEX_TIMESTAMP);
        String bgType = bundle.getString(AIDEX_BG_TYPE);
        Double bgValue = bundle.getDouble(AIDEX_BG_VALUE);

        int bgValueMgDl = 0;

        if (AidexBroadcastIntents.UNIT_MG_DL.equalsIgnoreCase(bgType)) {
            bgValueMgDl = bgValue.intValue();
        } else if (AidexBroadcastIntents.UNIT_MMOL_L.equalsIgnoreCase(bgType)) {
            bgValueMgDl = (int) (bgValue * Constants.MMOLL_TO_MGDL);
        } else {
            UserError.Log.e(TAG, "Aidex Broadcast BgType invalid. BgType needs to be either: mg/dl or mmol/l.");
            return;
        }

        if (bgValueMgDl <= 0) {
            UserError.Log.w(TAG, "Aidex Broadcast received BG=0, ignoring it.");
            return;
        }

        checkIfCorrectSensorIsRunning(sensorId, timeStamp);

        UserError.Log.i(TAG, "Aidex Broadcast NewBGEstimate received: bg=" + bgValueMgDl + ", time=" + JoH.dateTimeText(timeStamp));
        BgReading.bgReadingInsertFromInt(bgValueMgDl, timeStamp, segmentation_timeslice, true);

    }


    private void checkIfCorrectSensorIsRunning(String sensorId, long timeStamp) {
        Sensor currentSensor = Sensor.currentSensor();
        if (currentSensor != null) {
            if (!currentSensor.uuid.equals(sensorId)) {
                if (JoH.pratelimit("aidex-sensor-restart", 1200)) {
                    Sensor.stopSensor();
                    Sensor.create(timeStamp - 10000, sensorId);
                }
            }
        } else {
            Sensor.create(timeStamp - 10000, sensorId);
        }
    }


    private void processCalibration(Bundle bundle) {
        if (Pref.getBooleanDefaultFalse("accept_broadcast_calibrations")) {

            final long calibration_timestamp = bundle.getLong(AIDEX_TIMESTAMP, -1);
            double bgValue = bundle.getDouble(AIDEX_BG_VALUE, -1);
            final String units = bundle.getString(AIDEX_BG_TYPE, AidexBroadcastIntents.UNIT_MG_DL);

            final long timeoffset = JoH.tsl() - calibration_timestamp;

            if (bgValue > 0) {

                if (timeoffset < 0) {
                    Home.toaststaticnext(gs(R.string.got_calibration_in_the_future__cannot_process));
                    return;
                }

                double valueMgDl = bgValue;

                if (AidexBroadcastIntents.UNIT_MMOL_L.equals(units)) {
                    valueMgDl = bgValue * Constants.MMOLL_TO_MGDL;
                }

                UserError.Log.ueh(TAG, "Adding blood test calibration: " + JoH.qs(bgValue, 2) + " offset ms: " + JoH.qs(timeoffset, 0));

                BloodTest.create(calibration_timestamp, valueMgDl, "Aidex", null);
            } else {
                Log.e(TAG, "Received broadcast calibration without glucose number");
            }
        } else {
            Log.w(TAG, "Received broadcast calibration from Aidex, but we don't accept calibrations (check settings)");
        }
    }


    private void processSensorStart(Bundle bundle) {
        if (bundle != null && bundle.containsKey(AIDEX_TIMESTAMP)) {
            long sensorStartTime = bundle.getLong(AIDEX_TIMESTAMP);
            String sensorId = bundle.getString(AIDEX_SENSOR_ID);

            if (stopCurrentSensor(sensorId)) {
                Sensor.create(sensorStartTime, bundle.getString(AIDEX_SENSOR_ID));
            }
        }
    }

    private boolean stopCurrentSensor(String nextSensorId) {
        Sensor last = Sensor.currentSensor();
        if (last != null) {
            if (last.uuid.equals(nextSensorId)) {
                // if we restart sensor we would have same uuid, so we don't do anything.
                return false;
            }
            Sensor.stopSensor();
        }

        return true;
    }


    private void processSensorRestart(Bundle bundle) {
        Log.w(TAG, "Received broadcast Sensor Restart from Aidex, but we don't process it.");
        if (JoH.pratelimit("aidex-sensor-restart", 1200)) {
            String sensorId = bundle.getString(AIDEX_SENSOR_ID);
            Sensor sensorSearch = Sensor.getByUuid(sensorId);
            long sensorStartTime = bundle.getLong(AIDEX_TIMESTAMP);

            if (sensorSearch != null) {
                Sensor currentSensor = Sensor.currentSensor();

                if (currentSensor == null) {
                    if (sensorSearch.stopped_at != 0) {
                        Sensor.restartSensor(sensorId);
                    }
                } else {
                    if (sensorSearch.equals(currentSensor)) {
                        return;
                    } else {
                        stopCurrentSensor("x");
                        if (sensorSearch.stopped_at != 0) {
                            Sensor.restartSensor(sensorId);
                        }
                    }
                }

            } else {
                if (stopCurrentSensor(sensorId)) {
                    Sensor.create(sensorStartTime, sensorId);
                }
            }
        } else {
            UserError.Log.wtf(TAG, "Exceeded rate limit for sensor restart in processSensorRestart()");
        }
    }


    private void processSensorStop(Bundle bundle) {
        String sensorId = bundle.getString(AidexBroadcastIntents.AIDEX_SENSOR_ID);

        Sensor last = Sensor.currentSensor();
        if (last != null) {
            if (!last.uuid.equals(sensorId)) {
                Sensor.stopSensor();
            }
        }
    }


    private void processNotification(Bundle bundle) {
        UserError.Log.i(TAG, "Received Aidex Notification");

        String messageType = bundle.getString(AidexBroadcastIntents.AIDEX_MESSAGE_TYPE);
        String messageValue = bundle.getString(AidexBroadcastIntents.AIDEX_MESSAGE_VALUE);

        if (messageValue != null && messageValue.length() > 0) {
            UserError.Log.i(TAG, "  Notification type=" + messageType + " with value=" + messageValue);
        } else {
            UserError.Log.i(TAG, "  Notification type=" + messageType);
        }

        // this messages could also be displayed as notifications but for now, we just log them
    }

}


