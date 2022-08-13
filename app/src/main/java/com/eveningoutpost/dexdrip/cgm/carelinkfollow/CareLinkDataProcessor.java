package com.eveningoutpost.dexdrip.cgm.carelinkfollow;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.ConnectData;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.SensorGlucose;

import java.util.Collections;

import static com.eveningoutpost.dexdrip.Models.BgReading.SPECIAL_FOLLOWER_PLACEHOLDER;


/**
 * Medtronic CareLink Connect Data Processor
 *   - process CareLink data and convert to xDrip internal data
 *   - update xDrip internal data
 */
public class CareLinkDataProcessor {


    private static final String TAG = "ConnectFollowDP";
    private static final boolean D = false;

    private static  final String UUID_CF_PREFIX = "1";
    private static  final String UUID_BG_PREFIX = "1";
    private static  final String EPOCH_0_YEAR = "1970";


    static synchronized void processData(final ConnectData connectData, final boolean live) {

        if (connectData == null) return;

        UserError.Log.d(TAG, "Create Sensor");
        final Sensor sensor = Sensor.createDefaultIfMissing();

        //TODO not good for backfill!
        //Sensor status
        sensor.latest_battery_level = connectData.medicalDeviceBatteryLevelPercent;
        sensor.save();

        if (connectData.sgs == null) UserError.Log.d(TAG, "SGs is null!");

        //1) SGs (if available)
        if (connectData.sgs != null) {
            // place in order of oldest first
            UserError.Log.d(TAG, "Sort SGs");
            try {
                Collections.sort(connectData.sgs, (o1, o2) -> o1.getTimestamp().compareTo(o2.getTimestamp()));
            } catch (Exception e) {
                UserError.Log.e(TAG, "Sort SGs error! Details: " + e);
                return;
            }

            UserError.Log.d(TAG, "For each SG");
            for (final SensorGlucose sg : connectData.sgs) {

                //Not NULL SG (shouldn't happen?!)
                if (sg != null) {

                    //Not NULL DATETIME (sensorchange?)
                    if (sg.datetime != null) {

                        //Not EPOCH 0 (warmup?)
                        if (!sg.datetime.startsWith(EPOCH_0_YEAR)) {

                            //Not 0 SG (not calibrated?)
                            if (sg.sg > 0) {

                                final long recordTimestamp = sg.getTimestamp();
                                if (recordTimestamp > 0) {

                                    final BgReading existing = BgReading.getForPreciseTimestamp(recordTimestamp, 10_000);
                                    if (existing == null) {
                                        UserError.Log.d(TAG, "NEW NEW NEW New entry: " + sg.toS());

                                        if (live) {
                                            final BgReading bg = new BgReading();
                                            bg.timestamp = recordTimestamp;
                                            bg.calculated_value = (double) sg.sg;
                                            bg.raw_data = SPECIAL_FOLLOWER_PLACEHOLDER;
                                            bg.filtered_data = (double) sg.sg;
                                            bg.noise = "";
                                            bg.uuid = UUID_CF_PREFIX + UUID_BG_PREFIX + String.valueOf(bg.timestamp);
                                            bg.calculated_value_slope = 0;
                                            bg.sensor = sensor;
                                            bg.sensor_uuid = sensor.uuid;
                                            bg.source_info = "Connect Follow";
                                            bg.save();
                                            Inevitable.task("entry-proc-post-pr", 500, () -> bg.postProcess(false));
                                        }
                                    } else {
                                        //existing entry, not needed
                                    }
                                } else {
                                    UserError.Log.e(TAG, "Could not parse a timestamp from: " + sg.toS());
                                }

                            } else {
                                UserError.Log.d(TAG, "SG is 0 (calibration missed?)");
                            }

                        } else {
                            UserError.Log.d(TAG, "SG DateTime is 0 (warmup phase?)");
                        }

                    } else {
                        UserError.Log.d(TAG, "SG DateTime is null (sensor expired?)");
                    }

                } else {
                    UserError.Log.d(TAG, "SG Entry is null!!!");
                }
            }
        }

        //2) Markers (if available)
        if(connectData.markers != null){

        }
    }
}