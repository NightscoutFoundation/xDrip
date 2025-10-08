package com.eveningoutpost.dexdrip.cgm.carelinkfollow;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.DateUtil;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.ActiveNotification;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.Alarm;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.ClearedNotification;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.Marker;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.RecentData;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.SensorGlucose;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.TextMap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.models.BgReading.SPECIAL_FOLLOWER_PLACEHOLDER;
import static com.eveningoutpost.dexdrip.models.Treatments.pushTreatmentSyncToWatch;


/**
 * Medtronic CareLink Data Processor
 * - process CareLink data and convert to xDrip internal data
 * - update xDrip internal data
 */
public class CareLinkDataProcessor {


    private static final String TAG = "CareLinkFollowDP";
    private static final boolean D = false;

    private static final String SOURCE_CARELINK_FOLLOW = "CareLink Follow";

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static synchronized void processData(final RecentData recentData, final boolean live) {

        List<SensorGlucose> filteredSgList;
        List<Marker> filteredMarkerList;

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

        //SENSOR GLUCOSE (if available)
        if (recentData.sgs != null) {

            final BgReading lastBg = BgReading.lastNoSenssor();
            final long lastBgTimestamp = lastBg != null ? lastBg.timestamp : 0;

            //create filtered sortable SG list
            filteredSgList = new ArrayList<>();
            for (SensorGlucose sg : recentData.sgs) {
                //SG DateTime is null (sensor expired?)
                if (sg != null && sg.getDate() != null) {
                    filteredSgList.add(sg);
                }
            }

            if (filteredSgList.size() > 0) {

                final Sensor sensor = Sensor.createDefaultIfMissing();
                sensor.save();

                // place in order of oldest first
                Collections.sort(filteredSgList, (o1, o2) -> o1.getDate().compareTo(o2.getDate()));

                for (final SensorGlucose sg : filteredSgList) {

                    //Not EPOCH 0 (warmup?)
                    if (sg.getDate().getTime() > 1) {

                        //Not in the future
                        if (sg.getDate().getTime() < new Date().getTime() + 300_000) {

                            //Not 0 SG (not calibrated?)
                            if (sg.sg > 0) {

                                //newer than last BG
                                if (sg.getDate().getTime() > lastBgTimestamp) {

                                    if (sg.getDate().getTime() > 0) {

                                        //New entry
                                        if (BgReading.getForPreciseTimestamp(sg.getDate().getTime(), 10_000) == null) {
                                            UserError.Log.d(TAG, "NEW NEW NEW New entry: " + sg.toS());

                                            if (live) {
                                                final BgReading bg = new BgReading();
                                                bg.timestamp = sg.getDate().getTime();
                                                bg.calculated_value = (double) sg.sg;
                                                bg.raw_data = SPECIAL_FOLLOWER_PLACEHOLDER;
                                                bg.filtered_data = (double) sg.sg;
                                                bg.noise = "";
                                                bg.uuid = UUID.randomUUID().toString();
                                                bg.calculated_value_slope = 0;
                                                bg.sensor = sensor;
                                                bg.sensor_uuid = sensor.uuid;
                                                bg.source_info = SOURCE_CARELINK_FOLLOW;
                                                bg.save();
                                                bg.find_slope();
                                                Inevitable.task("entry-proc-post-pr", 500, () -> bg.postProcess(false));
                                            }
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
                        UserError.Log.d(TAG, "SG DateTime in future: " + sg.datetime);
                    }
                }
            }
        }


        //MARKERS (if available)
        if (recentData.markers != null) {

            //Filter markers
            filteredMarkerList = new ArrayList<>();
            for (Marker marker : recentData.markers) {
                if (marker != null && marker.type != null && marker.getDate() != null) {
                    filteredMarkerList.add(marker);
                }
            }

            if (filteredMarkerList.size() > 0) {
                //sort markers by time
                Collections.sort(filteredMarkerList, (o1, o2) -> o1.getDate().compareTo(o2.getDate()));

                //process markers one-by-one
                for (Marker marker : filteredMarkerList) {

                    //FINGER BG
                    if (marker.isBloodGlucose() && Pref.getBooleanDefaultFalse("clfollow_download_finger_bgs")) {
                        //check required values
                        if (marker.getBloodGlucose() != null && !marker.getBloodGlucose().equals(0)) {
                            //new blood test
                            if (BloodTest.getForPreciseTimestamp(marker.getDate().getTime(), 10000) == null) {
                                BloodTest.create(marker.getDate().getTime(), marker.getBloodGlucose(), SOURCE_CARELINK_FOLLOW);
                            }
                        }

                        //INSULIN, MEAL => Treatment
                    } else if ((marker.type.equals(Marker.MARKER_TYPE_INSULIN) && Pref.getBooleanDefaultFalse("clfollow_download_boluses"))
                            || (marker.type.equals(Marker.MARKER_TYPE_MEAL) && Pref.getBooleanDefaultFalse("clfollow_download_meals"))) {

                        //insulin, meal only for pumps and cgm (cgm = currently only Simplera, no value in case of Guardian CGM)
                        if (recentData.isNGP() || recentData.isCGM()) {

                            Treatments t;
                            double carbs = 0;
                            double insulin = 0;

                            //Extract treament infos (carbs, insulin)
                            //Insulin
                            if (marker.type.equals(Marker.MARKER_TYPE_INSULIN)) {
                                carbs = 0;
                                if (marker.getInsulinAmount() != null) {
                                    insulin = marker.getInsulinAmount();
                                }
                                //SKIP if insulin = 0
                                if (insulin == 0) continue;
                                //Carbs
                            } else if (marker.type.equals(Marker.MARKER_TYPE_MEAL)) {
                                if (marker.getCarbAmount() != null) {
                                    carbs = marker.getCarbAmount();
                                }
                                insulin = 0;
                                //SKIP if carbs = 0
                                if (carbs == 0) continue;
                            }

                            //new Treatment
                            if (newTreatment(carbs, insulin, marker.getDate().getTime())) {
                                t = Treatments.create(carbs, insulin, marker.getDate().getTime());
                                if (t != null) {
                                    t.enteredBy = SOURCE_CARELINK_FOLLOW;
                                    t.save();
                                    if (Home.get_show_wear_treatments())
                                        pushTreatmentSyncToWatch(t, true);
                                    UserError.Log.d(TAG, "NEW TREATMENT: " + treatmentToString(t));
                                }
                            }
                        }

                    }

                }
            }
        }

        //PUMP INFO (Pump Status)
        if (recentData.isNGP()) {
            PumpStatus.setReservoir(recentData.reservoirRemainingUnits);
            PumpStatus.setBattery(recentData.getDeviceBatteryLevel());
            if (recentData.activeInsulin != null)
                PumpStatus.setBolusIoB(recentData.activeInsulin.amount);
            PumpStatus.syncUpdate();
        }
		
        // LAST ALARM -> NOTE (only for GC)
        if (Pref.getBooleanDefaultFalse("clfollow_download_notifications")) {

            // Only Guardian Connect, NGP has all in notifications
            if (recentData.isGM() && recentData.lastAlarm != null) {
                //Add notification from alarm
                if (recentData.lastAlarm.datetimeAsDate != null && recentData.lastAlarm.kind != null)
                    addNotification(recentData.lastAlarm.datetimeAsDate, recentData.getDeviceFamily(), recentData.lastAlarm);
            }
        }


        //NOTIFICATIONS -> NOTE
        if (Pref.getBooleanDefaultFalse("clfollow_download_notifications")) {
            if (recentData.notificationHistory != null) {
                //Active Notifications
                if (recentData.notificationHistory.activeNotifications != null) {
                    for (ActiveNotification activeNotification : recentData.notificationHistory.activeNotifications) {
                        addNotification(activeNotification.dateTime, recentData.getDeviceFamily(), activeNotification.getMessageId(), activeNotification.faultId);
                    }
                }
                //Cleared Notifications
                if (recentData.notificationHistory.clearedNotifications != null) {
                    for (ClearedNotification clearedNotification : recentData.notificationHistory.clearedNotifications) {
                        Date notificationDate = clearedNotification.triggeredDateTime != null ? clearedNotification.triggeredDateTime : clearedNotification.dateTime;
                        addNotification(notificationDate, recentData.getDeviceFamily(), clearedNotification.getMessageId(), clearedNotification.faultId);
                    }
                }
            }
        }

    }

    //Check if treatment is new (no identical entry (timestamp, carbs, insulin) exists)
    protected static boolean newTreatment(double carbs, double insulin, long timestamp) {

        List<Treatments> treatmentsList;
        //Treatment with same timestamp and carbs + insulin exists?
        treatmentsList = Treatments.listByTimestamp(timestamp);
        if (treatmentsList != null) {
            for (Treatments treatments : treatmentsList) {
                if (treatments.carbs == carbs && treatments.insulin == insulin)
                    return false;
            }
        }
        return true;
    }


    //Create notification from CareLink messageId
    protected static boolean addNotification(Date date, String deviceFamily, String messageId, String faultId) {

        if (deviceFamily != null && messageId != null)
            return addNotification(date, TextMap.getNotificationMessage(deviceFamily, messageId, faultId));
        else
            return false;

    }

    //Create notification from CareLink Alarm
    protected static boolean addNotification(Date date, String deviceFamily, Alarm alarm) {

        if (deviceFamily != null && alarm != null && alarm.kind != null)
            return addNotification(date, TextMap.getAlarmMessage(deviceFamily, alarm));
        else
            return false;

    }

    //Create notification from CareLink note info
    protected static boolean addNotification(Date date, String noteText) {

        //Valid date
        if (date != null && noteText != null) {
            //New note
            if (newNote(noteText, date.getTime())) {
                //create_note in Treatment is not good, because of automatic link to other treatments in 5 mins range
                Treatments note = new Treatments();
                note.notes = noteText;
                note.timestamp = date.getTime();
                note.created_at = DateUtil.toISOString(note.timestamp);
                note.uuid = UUID.randomUUID().toString();
                note.enteredBy = SOURCE_CARELINK_FOLLOW;
                note.save();
                if (Home.get_show_wear_treatments())
                    pushTreatmentSyncToWatch(note, true);
                return true;
            }
        }

        return false;

    }


    //Check note is new
    protected static boolean newNote(String note, long timestamp) {

        List<Treatments> treatmentsList;
        //Treatment with same timestamp and note text exists?
        treatmentsList = Treatments.listByTimestamp(timestamp);
        if (treatmentsList != null) {
            for (Treatments treatments : treatmentsList) {
                if (treatments.notes.contains(note))
                    return false;
            }
        }

        return true;

    }

    protected static String treatmentToString(Treatments treatments){
        return DateUtil.toISOString(treatments.timestamp) + " - "
                + String.format("%.3f", treatments.insulin) + "U "
                + String.format("%.0f", treatments.carbs) + "g ";
    }

}