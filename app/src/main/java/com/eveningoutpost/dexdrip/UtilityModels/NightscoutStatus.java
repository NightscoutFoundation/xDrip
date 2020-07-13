package com.eveningoutpost.dexdrip.UtilityModels;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Models.DateUtil;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.NSBasal;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.annotations.Expose;

/*

Here is a typical string that we will have to parse. 

{
"_id": {
"$oid": "5cc4cd364d9a27000468793a"
},
"device": "openaps://openaps",
"openaps": {
"iob": {
    "iob": 0.393,
    "activity": 0.0008,
    "basaliob": 0.393,
    "bolusiob": 0,
    "netbasalinsulin": 0.4,
    "bolusinsulin": 0,
    "iobWithZeroTemp": {
        "iob": 0.393,
        "activity": 0.0008,
        "basaliob": 0.393,
        "bolusiob": 0,
        "netbasalinsulin": 0.4,
        "bolusinsulin": 0,
        "time": "2019-04-27T21:37:16.000Z"
    },
    "lastBolusTime": 1556005964000,
    "lastTemp": {
        "rate": 1.8,
        "timestamp": "2019-04-28T00:15:50+03:00",
        "started_at": "2019-04-27T21:15:50.000Z",
        "date": 1556399750000,
        "duration": 22.43
    },
    "timestamp": "2019-04-27T21:37:16.000Z"
},
"suggested": {
    "temp": "absolute",
    "bg": 213,
    "tick": "+7",
    "eventualBG": 217,
    "insulinReq": 1.27,
    "reservoir": "267.4\n",
    "deliverAt": "2019-04-27T21:38:03.439Z",
    "sensitivityRatio": 1,
    "predBGs": {
        "IOB": [
            213,
            219,
            223,
            228,
            231,
            234,
            236,
            238,
            239,
            240,
            240,
            239,
            238,
            237,
            236,
            235,
            234,
            233,
            232,
            231,
            230,
            229,
            228,
            227,
            226,
            225,
            224,
            223,
            223,
            222,
            221,
            220,
            220,
            219,
            219,
            218,
            218,
            217,
            217,
            216,
            216,
            215,
            215,
            215,
            215,
            214
        ],
        "ZT": [
            213,
            213,
            212,
            212,
            211,
            210,
            210,
            209,
            209,
            209,
            208,
            208,
            208,
            208,
            209,
            209
        ]
    },
    "COB": 0,
    "IOB": 0.393,
    "reason": "undefined11COB: 0, Dev: 39, BGI: 0, ISF: 90, CR: 14.92, Target: 100, minPredBG 214, minGuardBG 214, IOBpredBG 214; Eventual BG 217 >= 100, adj. req. rate: 3.2 to maxSafeBasal: 1.8, tempi2 1.8 >~ req 1.8U/hr. ",
    "timestamp": "2019-04-27T21:38:03.439Z"
},
"enacted": {
    "temp": "absolute",
    "bg": 192,
    "tick": "+1",
    "eventualBG": 198,
    "insulinReq": 1.09,
    "reservoir": "268.1\n",
    "deliverAt": "2019-04-27T21:16:19.850Z",
    "sensitivityRatio": 1,
    "COB": 0,
    "IOB": 0,
    "reason": "undefined11COB: 0, Dev: 6, BGI: 0, ISF: 90, CR: 14.92, Target: 100, minPredBG 198, minGuardBG 193, IOBpredBG 198; Eventual BG 198 >= 100, adj. req. rate: 2.85 to maxSafeBasal: 1.8, no tempi3, setting 1.8U/hr. . tzachi returning with good values 1.8",
    "duration": 30,
    "rate": 1.8,
    "received": true,
    "timestamp": "2019-04-27T21:16:24.885Z"
}
},
"pump": {
"clock": "2019-04-28T00:37:16+03:00",
"battery": {
    "voltage": 1.43,
    "string": "normal"
},
"reservoir": 267.4
},
"uploader": {
"batteryVoltage": 3210,
"battery": 2
},
"created_at": "2019-04-27T21:44:22.550Z"
}


// Code to try:

str1= {created_at:  "2019-04-27T21:44:22.550Z",
openaps: {enacted : {timestamp: "2019-04-27T21:16:24.885Z", recieved: true}}}

function toMoments (status) {
return {
when:  moment(status.mills), 
enacted: status.openaps.enacted && status.openaps.enacted.timestamp && (status.openaps.enacted.recieved || status.openaps.enacted.received) && moment(status.openaps.enacted.timestamp), 
notEnacted: status.openaps.enacted && status.openaps.enacted.timestamp && !(status.openaps.enacted.recieved || status.openaps.enacted.received) && moment(status.openaps.enacted.timestamp),
suggested: status.openaps.suggested && status.openaps.suggested.timestamp && moment(status.openaps.suggested.timestamp) ,
iob: status.openaps.iob && status.openaps.iob.timestamp && moment(status.openaps.iob.timestamp)
};
}
alert (toMoments(str1).enacted);


*/
// This should be like the js moment. have false or time. time of 0 means false.

public class NightscoutStatus {

    private static final String TAG = "NightscoutStatus";

    private static final String NS_STATUS_KEY = "ns-status-key";

    private static double iobFromJson(JSONObject json) {
        if (json == null) {
            return 0;
        }
        /*
         * JSONObject iobJson = getJSONObjectNull(json, "iob"); if(iobJson ==
         * null) { return 0; }
         */
        return getJSONSDoubleOrZero(json, "IOB");
    }

    private static double cobFromJson(JSONObject json) {
        if (json == null) {
            return 0;
        }
        /*
         * JSONObject iobJson = getJSONObjectNull(json, "COB"); if(iobJson ==
         * null) { return 0; }
         */
        return getJSONSDoubleOrZero(json, "COB");
    }

    // Get the iob based on
    // https://github.com/nightscout/cgm-remote-monitor/blob/dc7ea1bf471b5043355806dc24ae317a1412ea5f/lib/plugins/openaps.js#L327
    // and on
    // https://github.com/nightscout/cgm-remote-monitor/blob/dc7ea1bf471b5043355806dc24ae317a1412ea5f/lib/plugins/iob.js#L105
    private static OApsStatus createOapsStatus(JSONObject tr) throws Exception {
        JSONObject openaps = getJSONObjectNull(tr, "openaps");
        if (openaps == null) {
            return null;
        }
        OApsStatus oApsStatus = new OApsStatus();
        long enacted_time = 0;
        long suggested_time = 0;
        // Calculate the time
        JSONObject enacted_json = getJSONObjectNull(openaps, "enacted");

        if (enacted_json != null) {
            String enacted_timestamp = getJSONStringNull(enacted_json, "timestamp");
            if (enacted_timestamp != null && enacted_timestamp.length() > 0) {
                enacted_time = DateUtil.tolerantFromISODateString(enacted_timestamp).getTime();
            }
        }
        JSONObject suggested_json = getJSONObjectNull(openaps, "suggested");
        if (suggested_json != null) {
            String suggested_timestamp = getJSONStringNull(suggested_json, "timestamp");
            if (suggested_timestamp != null && suggested_timestamp.length() > 0) {
                suggested_time = DateUtil.tolerantFromISODateString(suggested_timestamp).getTime();
            }
        }
        oApsStatus.lastLoopMoment = Math.max(suggested_time, enacted_time);

        JSONObject iob_json = getJSONObjectNull(openaps, "iob");

        if (iob_json != null) {
            oApsStatus.iob = getJSONSDoubleOrZero(iob_json, "iob");
            String iobTimeString = getJSONStringNull(iob_json, "timestamp");
            if (iobTimeString != null) {
                oApsStatus.iobTime = DateUtil.tolerantFromISODateString(iobTimeString).getTime();
            }
        }

        if (suggested_time > 0 && enacted_time > 0) {
            if (suggested_time > enacted_time) {
                oApsStatus.cob = cobFromJson(suggested_json);
                oApsStatus.cobTime = suggested_time;
            } else {
                oApsStatus.cob = cobFromJson(enacted_json);
                oApsStatus.cobTime = enacted_time;
            }
        } else if (enacted_time > 0) {
            oApsStatus.cob = cobFromJson(enacted_json);
            oApsStatus.cobTime = enacted_time;
        } else if (suggested_time > 0) {
            oApsStatus.cob = cobFromJson(suggested_json);
            oApsStatus.cobTime = suggested_time;
        }

        return oApsStatus;
    }

    private static boolean UpdateCurrentStatus(OApsStatus curentStatus, OApsStatus lastReading) {
        boolean new_data = false;
        if (lastReading.lastLoopMoment > curentStatus.lastLoopMoment) {
            curentStatus.lastLoopMoment = lastReading.lastLoopMoment;
            new_data = true;
        }

        if (lastReading.iobTime > curentStatus.iobTime) {
            curentStatus.iobTime = lastReading.iobTime;
            curentStatus.iob = lastReading.iob;
            new_data = true;
        }

        if (lastReading.cobTime > curentStatus.cobTime) {
            curentStatus.cobTime = lastReading.cobTime;
            curentStatus.cob = lastReading.cob;
            new_data = true;
        }
        return new_data;
    }

    public static boolean processDeviceStatusResponse(final String response) throws Exception {
        boolean new_data = false;

        Log.i(TAG, "Starting processDeviceStatusResponse " + response);
        String last_modified_string = PersistentStore.getString(NS_STATUS_KEY);
        OApsStatus curentStatus = OApsStatus.fromJson(last_modified_string);
        if (curentStatus == null) {
            curentStatus = new OApsStatus();
        }

        final JSONArray jsonArray = new JSONArray(response);
        for (int i = 0; i < jsonArray.length(); i++) {
            final JSONObject tr = (JSONObject) jsonArray.get(i);
            OApsStatus oApsStatus = createOapsStatus(tr);
            if (oApsStatus == null) {
                continue;
            }
            //Log.e(TAG, "oApsStatus = " + oApsStatus.toJson());
            new_data |= UpdateCurrentStatus(curentStatus, oApsStatus);
        }
        if(new_data) {
            PersistentStore.setString(NS_STATUS_KEY, curentStatus.toJson());
            GcmActivity.pushNsStatus(curentStatus.toJson());
        }
        return new_data;
    }

    public static OApsStatus getLatestStatus() {
        String last_modified_string = PersistentStore.getString(NS_STATUS_KEY);
        if(last_modified_string == null || last_modified_string.length() == 0) {
            return null;
        }
        OApsStatus curentStatus = OApsStatus.fromJson(last_modified_string);
        //Log.e("xxx", "returning status = " + curentStatus.toJson());
        return curentStatus;
    }
    
    public static void addFromJson(String json) {
        if (json == null) {
            return;
        }
        // Actually the follownig line was enough, but we are doing some sanity checks.
        //PersistentStore.setString(NS_STATUS_KEY, json);
        OApsStatus curentStatus = OApsStatus.fromJson(json);
        if (curentStatus == null) {
            return;
        }
        PersistentStore.setString(NS_STATUS_KEY, curentStatus.toJson());
    }

    // This is a version that returns null instead of throwing.
    static JSONObject getJSONObjectNull(JSONObject jo, String name) {
        JSONObject ret = null;
        try {
            if(!jo.has(name)) {
                return null;
            }
            ret = jo.getJSONObject(name);
        } catch (JSONException e) {
            Log.e(TAG, "getJSONObjectNull exception", e);
            return null;
        }
        return ret;
    }

    // This is a version that returns null instead of throwing.
    static String getJSONStringNull(JSONObject jo, String name) {
        String ret = null;
        try {
            if(!jo.has(name)) {
                return null;
            }
            ret = jo.getString(name);
        } catch (JSONException e) {
            Log.e(TAG, "getJSONStringNull exception", e);
            return null;
        }
        return ret;
    }

    // This is a version that returns null instead of throwing.
    static double getJSONSDoubleOrZero(JSONObject jo, String name) {
        
        double ret = 0;
        try {
            if(!jo.has(name)) {
                return 0;
            }
            ret = jo.getDouble(name);
        } catch (JSONException e) {
            Log.e(TAG, "getJSONSDoubleOrZero exception", e);
            return 0;
        }
        return ret;
    }

    // This is a version that returns false instead of throwing.
    static boolean getJSONBooleanFalse(JSONObject jo, String name) {
        boolean ret = false;
        try {
            if(!jo.has(name)) {
                return false;
            }
            ret = jo.getBoolean(name);
        } catch (JSONException e) {
            Log.e(TAG, "getJSONBooleanFalse exception", e);
            return false;
        }
        return ret;
    }

    public static void test() {
        String str1 = "[{created_at:  \"2019-04-27T21:44:22.550Z\", openaps: {enacted : {timestamp: \"2019-04-27T21:16:24.885Z\", recieved: true}}}]";
        Log.e(TAG, "Starting test " + str1);
        try {
            processDeviceStatusResponse(str1);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

class OApsStatus {
    @Expose
    long lastLoopMoment = 0;

    @Expose
    double iob = 0;

    @Expose
    long iobTime;

    @Expose
    double cob = 0;

    @Expose
    long cobTime;
    // basal will be drawed so not here.

    static OApsStatus fromJson(String json) {
        if (json == null) {
            return new OApsStatus();
        }
        try {
            return JoH.defaultGsonInstance().fromJson(json, OApsStatus.class);
        } catch (Exception e) {
            Log.e("NightscoutStatus", "OApsStatus Got exception processing json msg: " + e );
            return null;
        }
    }

    String toJson() {
        return JoH.defaultGsonInstance().toJson(this);
    }
}

// Looking for: lastLoopMoment
/*
 * 
 * _.forEach(recentData, function eachStatus (status) { var device =
 * getDevice(status);
 * 
 * var moments = toMoments(status);
 * 
 * 
 * var enacted = status.openaps && status.openaps.enacted; if (enacted &&
 * moments.enacted && (!result.lastEnacted ||
 * moments.enacted.isAfter(result.lastEnacted.moment))) { result.lastEnacted =
 * enacted; }
 * 
 * if (enacted && moments.notEnacted && (!result.lastNotEnacted ||
 * moments.notEnacted.isAfter(result.lastNotEnacted.moment))) {
 * result.lastNotEnacted = enacted; }
 * 
 * var suggested = status.openaps && status.openaps.suggested; if (suggested &&
 * moments.suggested && (!result.lastSuggested ||
 * moments.suggested.isAfter(result.lastSuggested.moment))) { suggested.moment =
 * moment(suggested.timestamp); result.lastSuggested = suggested; }
 * 
 * 
 * });
 * 
 * if (result.lastEnacted && result.lastSuggested) { if
 * (result.lastEnacted.moment.isAfter(result.lastSuggested.moment)) {
 * result.lastLoopMoment = result.lastEnacted.moment; result.lastEventualBG =
 * result.lastEnacted.eventualBG; } else { result.lastLoopMoment =
 * result.lastSuggested.moment; result.lastEventualBG =
 * result.lastSuggested.eventualBG; } } else if (result.lastEnacted &&
 * result.lastEnacted.moment) { result.lastLoopMoment =
 * result.lastEnacted.moment; result.lastEventualBG =
 * result.lastEnacted.eventualBG; } else if (result.lastSuggested &&
 * result.lastSuggested.moment) { result.lastLoopMoment =
 * result.lastSuggested.moment; result.lastEventualBG =
 * result.lastSuggested.eventualBG; }
 * 
 * };
 * 
 * 
 */