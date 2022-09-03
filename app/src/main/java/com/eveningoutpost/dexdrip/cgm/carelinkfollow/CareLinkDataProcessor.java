package com.eveningoutpost.dexdrip.cgm.carelinkfollow;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.ConnectData;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.RecentData;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.SensorGlucose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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


    static synchronized void processData(final RecentData recentData, final boolean live) {

        List<SensorGlucose> filteredSgList;

        UserError.Log.d(TAG, "Start processsing data...");

        //SKIP ALL IF EMPTY!!!
        if (recentData == null) {
            UserError.Log.e(TAG, "Recent data is null, processing stopped!");
            return;
        }

        if (recentData.sgs == null) UserError.Log.d(TAG, "SGs is null!");

        //SKIP DATA processing if NO PUMP CONNECTION (time shift seems to be different in this case, needs further analysis)
        if (recentData.isNGP() && !recentData.pumpCommunicationState) {
            UserError.Log.d(TAG, "Not connected to pump => time can be wrong, leave processing!");
            return;
        }

        //1 - SENSOR GLUCOSE (if available
        if (recentData.sgs != null) {

            final BgReading lastBg = BgReading.lastNoSenssor();
            final long lastBgTimestamp = lastBg != null ? lastBg.timestamp : 0;

            //create filtered sortable SG list
            filteredSgList = new ArrayList<>();
            for (SensorGlucose sg : recentData.sgs) {
                //SG DateTime is null (sensor expired?)
                if (sg != null & sg.datetimeAsDate != null) {
                        filteredSgList.add(sg);
                }
            }

            if(filteredSgList.size() > 0) {

                final Sensor sensor = Sensor.createDefaultIfMissing();
                sensor.save();

                // place in order of oldest first
                Collections.sort(recentData.sgs, (o1, o2) -> o1.datetimeAsDate.compareTo(o2.datetimeAsDate));

                for (final SensorGlucose sg : filteredSgList) {

                    //Not NULL SG (shouldn't happen?!)
                    if (sg != null) {

                        //Not NULL DATETIME (sensorchange?)
                        if (sg.datetimeAsDate != null) {

                            //Not EPOCH 0 (warmup?)
                            if (sg.datetimeAsDate.getTime() > 1) {

                                //Not 0 SG (not calibrated?)
                                if (sg.sg > 0) {

                                    //newer than last BG
                                    if (sg.datetimeAsDate.getTime() > lastBgTimestamp) {

                                        if (sg.datetimeAsDate.getTime() > 0) {

                                            final BgReading existing = BgReading.getForPreciseTimestamp(sg.datetimeAsDate.getTime(), 10_000);
                                            if (existing == null) {
                                                UserError.Log.d(TAG, "NEW NEW NEW New entry: " + sg.toS());

                                                if (live) {
                                                    final BgReading bg = new BgReading();
                                                    bg.timestamp = sg.datetimeAsDate.getTime();
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
                                                    bg.find_slope();
                                                    Inevitable.task("entry-proc-post-pr", 500, () -> bg.postProcess(false));
                                                }
                                            } else {
                                                //existing entry, not needed
                                            }
                                        } else {
                                            UserError.Log.e(TAG, "Could not parse a timestamp from: " + sg.toS());
                                        }
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
        }

        //2) Markers (if available)
        if(recentData.markers != null){

        }
    }
}