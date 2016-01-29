package com.eveningoutpost.dexdrip.Models;

/**
 * Created by jamorham on 31/12/15.
 */

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.GoogleDriveInterface;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// TODO Switchable Carb models
// TODO Linear array timeline optimization

@Table(name = "Treatments", id = BaseColumns._ID)
public class Treatments extends Model {
    private final static String TAG = "jamorham " + Treatments.class.getSimpleName();
    public static double activityMultipler = 8.4; // somewhere between 8.2 and 8.8
    private static Treatments lastCarbs;
    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;
    @Expose
    @Column(name = "eventType")
    public String eventType;
    @Expose
    @Column(name = "enteredBy")
    public String enteredBy;
    @Expose
    @Column(name = "notes")
    public String notes;
    @Expose
    @Column(name = "uuid", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public String uuid;
    @Expose
    @Column(name = "carbs")
    public double carbs;
    @Expose
    @Column(name = "insulin")
    public double insulin;
    @Expose
    @Column(name = "created_at")
    public String created_at;

    public static synchronized Treatments create(double carbs, double insulin, long timestamp) {

        // TODO sanity check values
        Log.d(TAG, "Creating treatment: Insulin: " + Double.toString(insulin) + " / Carbs: " + Double.toString(carbs));

        if ((carbs == 0) && (insulin == 0)) return null;

        if (timestamp == 0) {
            timestamp = new Date().getTime();
        }

        Treatments Treatment = new Treatments();
        Treatment.enteredBy = "xdrip";
        Treatment.eventType = "<none>";
        Treatment.carbs = carbs;
        Treatment.insulin = insulin;
        Treatment.timestamp = timestamp;
        Treatment.uuid = UUID.randomUUID().toString();
        Treatment.save();
        GcmActivity.pushTreatmentAsync(Treatment);
        return Treatment;
    }

    public static Treatments last() {
        return new Select()
                .from(Treatments.class)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static Treatments byuuid(String uuid) {
        return new Select()
                .from(Treatments.class)
                .where("uuid = ?", uuid)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static void delete_all() {
        delete_all(false);
    }

    public static void delete_all(boolean from_interactive) {
        if (from_interactive) {
            GcmActivity.push_delete_all_treatments();
        }
        new Delete()
                .from(Treatments.class)
                .execute();
    }

    public static Treatments delete_last() {
        return delete_last(false);
    }

    public static void delete_by_uuid(String uuid) {
        Treatments thistreat = byuuid(uuid);
        if (thistreat != null) {
            thistreat.delete();
        }
    }

    public static Treatments delete_last(boolean from_interactive) {
        Treatments thistreat = last();
        if (thistreat != null) {

            if (from_interactive) {
                GcmActivity.push_delete_treatment(thistreat);
                GoogleDriveInterface gdrive = new GoogleDriveInterface();
                gdrive.deleteTreatmentAtRemote(thistreat.uuid);
            }
            thistreat.delete();
        }
        return null;
    }

    public static Treatments fromJSON(String json) {
        try {
            return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json,Treatments.class);
        } catch (Exception e) {
            Log.d(TAG, "Got exception parsing treatment json: " + e.toString());
            Home.toaststatic("Error on treatment, probably decryption key mismatch");
            return null;
        }
    }

    public static boolean pushTreatmentFromJson(String json) {
        Log.d(TAG, "converting treatment from json: ");
        Treatments mytreatment = fromJSON(json);
        if (mytreatment != null) {
            Log.d(TAG, "Saving pushed treatment: " + mytreatment.uuid);
            mytreatment.enteredBy = "sync";
            mytreatment.eventType = "<none>"; // should have a default
            mytreatment.save();
            long x = mytreatment.save();
            Log.d(TAG, "Saving treatment result: " + x);
            Home.staticRefreshBGCharts();
            return true;
        } else {
            return false;
        }
    }

    public static List<Treatments> latestForGraph(int number, double startTime) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1); // are there decimal points in the database??
        return new Select()
                .from(Treatments.class)
                .where("timestamp >= " + df.format(startTime))
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static long getTimeStampWithOffset(double offset) {
        //  optimisation instead of creating a new date each time?
        return (long) (new Date().getTime() - offset);
    }

    public static CobCalc cobCalc(Treatments treatment, double lastDecayedBy, double time) {

        double delay = 20; // minutes till carbs start decaying

        double delayms = delay * 60 * 1000;
        if (treatment.carbs > 0) {

            CobCalc thisCobCalc = new CobCalc();
            thisCobCalc.carbTime = treatment.timestamp;

            // no previous carb treatment? Set to our start time
            if (lastDecayedBy == 0) {
                lastDecayedBy = thisCobCalc.carbTime;
            }

            double carbs_hr = Profile.getCarbAbsorptionRate(time);
            double carbs_min = carbs_hr / 60;
            double carbs_ms = carbs_min / (60 * 1000);

            thisCobCalc.decayedBy = thisCobCalc.carbTime; // initially set to start time for this treatment

            double minutesleft = (lastDecayedBy - thisCobCalc.carbTime) / 1000 / 60;
            double how_long_till_carbs_start_ms = (lastDecayedBy - thisCobCalc.carbTime);
            thisCobCalc.decayedBy += (Math.max(delay, minutesleft) + treatment.carbs / carbs_min) * 60 * 1000;

            if (delay > minutesleft) {
                thisCobCalc.initialCarbs = treatment.carbs;
            } else {
                thisCobCalc.initialCarbs = treatment.carbs + minutesleft * carbs_min;
            }
            double startDecay = thisCobCalc.carbTime + (delay * 60 * 1000);

            if (time < lastDecayedBy || time > startDecay) {
                thisCobCalc.isDecaying = 1;
            } else {
                thisCobCalc.isDecaying = 0;
            }
            return thisCobCalc;

        } else {
            return null;
        }
    }

    public static Iob calcTreatment(Treatments treatment, double time, double lastDecayedby) {

        double dia = 3; // duration insulin action in hours
        double peak = 75; // minutes in based on a 3 hour DIA - scaled proportionally (orig 75)
        double sens = Profile.getSensitivity(time); // sensitivity currently in mmol
        double insulin_delay_minutes = 0;

        double insulin_timestamp = treatment.timestamp + (insulin_delay_minutes * 60 * 1000);

        Iob response = new Iob();

        double scaleFactor = 3.0 / dia, iobContrib = 0, activityContrib = 0;

        // only use treatments with insulin component which have already happened
        if ((treatment.insulin > 0) && (insulin_timestamp < time)) {
            double bolusTime = insulin_timestamp; // bit of a dupe
            double minAgo = scaleFactor * (((time - bolusTime) / 1000) / 60);

            if (minAgo < peak) {
                double x1 = minAgo / 5 + 1;
                iobContrib = treatment.insulin * (1 - 0.001852 * x1 * x1 + 0.001852 * x1);
                // units: BG (mg/dL)  = (BG/U) *    U insulin     * scalar
                activityContrib = sens * activityMultipler * treatment.insulin * (2 / dia / 60 / peak) * minAgo;

            } else if (minAgo < 180) {
                double x2 = (minAgo - peak) / 5;
                iobContrib = treatment.insulin * (0.001323 * x2 * x2 - .054233 * x2 + .55556);
                activityContrib = sens * activityMultipler * treatment.insulin * (2 / dia / 60 - (minAgo - peak) * 2 / dia / 60 / (60 * dia - peak));
            }

        }
        response.iob = iobContrib;
        response.activity = activityContrib;
        return response;
    }


    public static List<Iob> ioBForGraph(int number, double startTime) {

        Log.d(TAG, "Processing iobforgraph: main  ");
        // get all treatments from 24 hours earlier than our current time
        List<Treatments> theTreatments = latestForGraph(2000, startTime - 86400000);
        Map<String, Boolean> carbsEaten = new HashMap<String, Boolean>();
        // this could be much more optimized with linear array instead of loops

        final double dontLookThisFar = 10 * 60 * 60 * 1000; // 10 hours max look

        double stomachCarbs = 0;

        final double step_minutes = 10;
        final double stepms = step_minutes * 60 * 1000; // 600s = 10 mins

        if (theTreatments.size() == 0) return null;

        Map ioblookup = new HashMap<Double, Double>(); // store for iob total vs time

        List<Iob> responses = new ArrayList<Iob>();
        Iob calcreply;

        double mytime = startTime;
        double lastmytime = mytime;
        double max_look_time = startTime + (30 * 60 * 60 * 1000);
        int counter = 0;
        // 30 hours max look at
        while ((responses.size() < number) && (mytime < max_look_time)) {

            double lastDecayedBy = 0, isDecaying = 0, delayMinutes = 0; // reset per time slot
            double totalIOB = 0, totalCOB = 0, totalActivity = 0;
            // per treatment per timeblock
            for (Treatments thisTreatment : theTreatments) {
                // early optimisation exclusion
                if ((thisTreatment.timestamp <= mytime) && (mytime - thisTreatment.timestamp) < dontLookThisFar) {
                    calcreply = calcTreatment(thisTreatment, mytime, lastDecayedBy); // was last decayed by but that offset wrongly??
                    totalIOB += calcreply.iob;
                    //totalCOB += calcreply.cob;
                    totalActivity += calcreply.activity;
                } // endif excluding a treatment
            } // per treatment

            //
            ioblookup.put(mytime, totalIOB);
            if (ioblookup.containsKey(lastmytime)) {
                double iobdiff = (double) ioblookup.get(lastmytime) - totalIOB;
                if (iobdiff < 0) iobdiff = 0;
                if ((iobdiff != 0) || (totalActivity != 0)) {
                    Log.d(TAG, "New IOB diffi @: " + JoH.qs(mytime) + " = " + JoH.qs(iobdiff) + " old activity: " + JoH.qs(totalActivity));
                }
                totalActivity = iobdiff; // WARNING OVERRIDE
            }

            double stomachDiff = ((Profile.getCarbAbsorptionRate(mytime) * stepms) / (60 * 60 * 1000));
            double newdelayedCarbs = (totalActivity * Profile.getLiverSensRatio(mytime) / Profile.getSensitivity(mytime)) * Profile.getCarbRatio(mytime);

            // calculate carbs
            for (Treatments thisTreatment : theTreatments) {
                // early optimisation exclusion
                if ((thisTreatment.timestamp <= mytime) && (mytime - thisTreatment.timestamp) < dontLookThisFar) {
                    if ((thisTreatment.carbs > 0) && (thisTreatment.timestamp < mytime)) {
                        // factor carbs delay in above when complete
                        if (!carbsEaten.containsKey(thisTreatment.uuid)) {
                            carbsEaten.put(thisTreatment.uuid, true);
                            stomachCarbs = stomachCarbs + thisTreatment.carbs;
                            stomachCarbs = stomachCarbs + stomachDiff; // offset first subtraction
                            // pre-subtract for granularity or just reduce granularity
                            Log.d(TAG, "newcarbs: " + thisTreatment.carbs + " " + thisTreatment.uuid + " @ " + thisTreatment.timestamp + " mytime: " + JoH.qs(mytime) + " diff: " + JoH.qs((thisTreatment.timestamp - mytime) / 1000) + " stomach: " + JoH.qs(stomachCarbs));
                        }
                        lastCarbs = thisTreatment;
                        CobCalc cCalc = cobCalc(thisTreatment, lastDecayedBy, mytime); // need to handle last decayedby shunting
                        double decaysin_hr = (cCalc.decayedBy - mytime) / 1000 / 60 / 60;
                        if (decaysin_hr > -10) {
                            // units: BG
                            double avgActivity = totalActivity;
                            // units:  g     =       BG      *      scalar     /          BG / U                           *     g / U
                            double delayedCarbs = (avgActivity * Profile.getLiverSensRatio(mytime) / Profile.getSensitivity(mytime)) * Profile.getCarbRatio(mytime);

                            delayMinutes = Math.round(delayedCarbs / (Profile.getCarbAbsorptionRate(mytime) / 60));
                            Log.d(TAG, "Avg activity: " + JoH.qs(avgActivity) + " Decaysin_hr: " + JoH.qs(decaysin_hr) + " delay minutes: " + JoH.qs(delayMinutes) + " delayed carbs: " + JoH.qs(delayedCarbs));
                            if (delayMinutes > 0) {
                                Log.d(TAG, "Delayed Carbs: " + JoH.qs(delayedCarbs) + " Delay minutes: " + JoH.qs(delayMinutes) + " Average activity: " + JoH.qs(avgActivity));
                                cCalc.decayedBy += delayMinutes * 60 * 1000;
                                decaysin_hr = (cCalc.decayedBy - mytime) / 1000 / 60 / 60;
                            }
                        }

                        lastDecayedBy = cCalc.decayedBy;

                        if (decaysin_hr > 0) {
                            Log.d(TAG, "cob: Adding " + JoH.qs(delayMinutes) + " minutes to decay of " + JoH.qs(thisTreatment.carbs) + "g bolus at " + JoH.qs(thisTreatment.timestamp));
                            totalCOB += Math.min(thisTreatment.carbs, decaysin_hr * Profile.getCarbAbsorptionRate(thisTreatment.timestamp));
                            Log.d(TAG, "cob: " + JoH.qs(Math.min(cCalc.initialCarbs, decaysin_hr * Profile.getCarbAbsorptionRate(thisTreatment.timestamp)))
                                    + " inital carbs:" + JoH.qs(cCalc.initialCarbs) + " decaysin_hr:" + JoH.qs(decaysin_hr) + " absorbrate:" + JoH.qs(Profile.getCarbAbsorptionRate(thisTreatment.timestamp)));
                            isDecaying = cCalc.isDecaying;
                        } else {
                            //    totalCOB = 0; //nix this?
                        }
                    } // if this treatment has carbs
                } // end if processing this treatment
            } // per carb treatment

            if (stomachCarbs > 0) {

                Log.d(TAG, "newcarbs Stomach Diff: " + JoH.qs(stomachDiff) + " Old total: " + JoH.qs(stomachCarbs) + " Delayed carbs: " + JoH.qs(newdelayedCarbs));

                stomachCarbs = stomachCarbs - stomachDiff;
                if (newdelayedCarbs > 0) {
                    double maximpact = stomachDiff * Profile.maxLiverImpactRatio(mytime);
                    if (newdelayedCarbs > maximpact) newdelayedCarbs = maximpact;
                    stomachCarbs = stomachCarbs + newdelayedCarbs; // add back on liverfactor ones
                }
                if (stomachCarbs < 0) stomachCarbs = 0;
            }

            if ((totalIOB > Profile.minimum_shown_iob) || (totalCOB > Profile.minimum_shown_cob) || (stomachCarbs > Profile.minimum_shown_cob)) {
                Iob thisrecord = new Iob();

                thisrecord.timestamp = (long) mytime;
                thisrecord.iob = totalIOB;
                thisrecord.activity = totalActivity; // hacky cruft
                thisrecord.cob = stomachCarbs;
                thisrecord.jCarbImpact = 0; // calculated below
                thisrecord.rawCarbImpact = (isDecaying * Profile.getSensitivity(mytime)) / Profile.getCarbRatio(mytime) * Profile.getCarbAbsorptionRate(mytime) / 60;

                // don't get confused with cob totals from previous treatments
                if ((responses.size() > 0) && (Math.abs(responses.get(responses.size() - 1).timestamp - thisrecord.timestamp) <= stepms)) {
                    double cobdiff = responses.get(responses.size() - 1).cob - thisrecord.cob;
                    if (cobdiff > 0) {
                        thisrecord.jCarbImpact = (cobdiff / Profile.getCarbRatio(mytime)) * Profile.getSensitivity(mytime);
                    }

                    double iobdiff = responses.get(responses.size() - 1).iob - totalIOB;
                    if (iobdiff > 0) {
                        thisrecord.jActivity = (iobdiff * Profile.getSensitivity(mytime));
                    }
                }

                Log.d(TAG, "added record: cob raw impact: " + Double.toString(thisrecord.rawCarbImpact) + " Isdecaying: "
                        + JoH.qs(isDecaying) + " jCarbImpact: " + JoH.qs(thisrecord.jCarbImpact) +
                        " jActivity: " + JoH.qs(thisrecord.jActivity) + " old activity: " + JoH.qs(thisrecord.activity));

                responses.add(thisrecord);
            }
            lastmytime = mytime;
            mytime = mytime + stepms;
            counter++;
        } // while time period in range

        Log.d(TAG, "Finished Processing iobforgraph: main - processed:  " + Integer.toString(counter) + " Timeslot records");
        return responses;
    }

    public String toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uuid", uuid);
            jsonObject.put("insulin", insulin);
            jsonObject.put("carbs", carbs);
            jsonObject.put("timestamp", timestamp);
            return jsonObject.toString();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
    }
}



