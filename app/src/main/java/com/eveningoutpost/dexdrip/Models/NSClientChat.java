package com.eveningoutpost.dexdrip.Models;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;

import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.xdrip;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by jamorham on 23/02/2016.
 */
public class NSClientChat {

    private static final String TAG = "jamorham nsclient";


    public static void pushTreatmentAsync(final Treatments thistreatment) {
        Thread testAddTreatment = new Thread() {
            @Override
            public void run() {

                try {
                    Context context = xdrip.getAppContext();
                    JSONObject data = new JSONObject();
                    if (thistreatment.carbs > 0) {
                        if (thistreatment.insulin > 0) {
                            data.put("eventType", "Meal Bolus");
                        } else {
                            data.put("eventType", "Carb Correction");
                        }
                    } else {
                        if (thistreatment.insulin > 0) {
                            data.put("eventType", "Correction Bolus");
                        } else {
                            if ((thistreatment.notes != null) && (thistreatment.notes.length() > 1)) {
                                data.put("eventType", "Note");
                            } else {
                                data.put("eventType", "<None>");
                            }
                        }
                    }

                    data.put("insulin", thistreatment.insulin);
                    if (thistreatment.insulinJSON != null) {
                        data.put("insulinInjections", thistreatment.insulinJSON);
                    }
                    data.put("carbs", thistreatment.carbs);
                    if (thistreatment.notes != null) {
                        data.put("notes", thistreatment.notes);
                    }
                    //  data.put("_id", thistreatment.uuid.replace("-",""));
                    //data.put("uuid",thistreatment.uuid);
                    data.put("created_at", DateUtil.toISOString(thistreatment.timestamp));
                    // data.put("NSCLIENTTESTRECORD", "NSCLIENTTESTRECORD");
                    Bundle bundle = new Bundle();
                    bundle.putString("action", "dbAdd");
                    bundle.putString("collection", "treatments"); // "treatments" || "entries" || "devicestatus" || "profile" || "food"
                    bundle.putString("data", data.toString());
                    Intent intent = new Intent(Intents.ACTION_DATABASE);
                    intent.putExtras(bundle);
                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    context.sendBroadcast(intent);
                    List<ResolveInfo> q = context.getPackageManager().queryBroadcastReceivers(intent, 0);
                    if (q.size() < 1) {
                        Log.e(TAG, "DBADD No receivers");
                    } else Log.e(TAG, "DBADD dbAdd " + q.size() + " receivers");
                } catch (JSONException e) {
                    Log.e(TAG, "Got exception with parsing: " + e.toString());
                }
            }
        };
        testAddTreatment.start();
    }

}
