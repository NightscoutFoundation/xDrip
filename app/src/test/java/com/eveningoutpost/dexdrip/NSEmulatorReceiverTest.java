package com.eveningoutpost.dexdrip;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertWithMessage;

import android.content.BroadcastReceiver;
import android.content.Intent;

// jamorham

public class NSEmulatorReceiverTest extends RobolectricTestWithConfig {

    @Test
    public void onReceiveTest() {
        // TODO
    }

    @Test
    public void bgReadingInsertFromDataTest() throws JSONException {

        // test data courtesy of philipgo - thanks
        final JSONObject json_object = new JSONObject();
        json_object.put("date", 1526717691000.0);
        json_object.put("sgv", 148);
        json_object.put("direction", "SingleUp");

        final BgReading result = NSEmulatorReceiver.bgReadingInsertFromData(json_object.getLong("date"),
                json_object.getDouble("sgv"),
                BgReading.slopefromName(json_object.getString("direction")),
                true);

        assertWithMessage("result not null").that(result).isNotNull();
        assertWithMessage("direction matches").that(result.slopeName()).isEqualTo("SingleUp");
        assertWithMessage("timestamp matches").that(result.timestamp).isEqualTo(1526717691000L);
        assertWithMessage("sgv matches 1").that(result.calculated_value).isEqualTo(148d);
        assertWithMessage("sgv matches 2").that(result.filtered_calculated_value).isEqualTo(148d);
        assertWithMessage("sgv matches 3").that(result.raw_data).isEqualTo(148d);
        assertWithMessage("sgv matches 4").that(result.filtered_data).isEqualTo(148d);

        //System.out.println(result.toJSON(true));
        //System.out.println(result.slopeName());

    }

    // This unit test below also shows how to construct a broadcast intent to push sensor data in to xDrip
    // this can be used by third party apps which wish to work as a data source
    @Test
    public void bgReadingExampleBroadcast() throws JSONException {

        // INTENT CREATION EXAMPLE

        // the intent action is
        Intent intent = new Intent(Intents.XDRIP_PLUS_NS_EMULATOR); // "com.eveningoutpost.dexdrip.NS_EMULATOR"

        intent.setPackage("com.eveningoutpost.dexdrip"); // when sending an intent name the package for reliability

        // we indicate to use the entries collection
        intent.putExtra("collection", "entries");

        // create an array for multiple readings
        final JSONArray sgv_array = new JSONArray();

        // create an element for the reading
        final JSONObject sgv_object = new JSONObject();
        sgv_object.put("type", "sgv"); // sensor glucose value record
        sgv_object.put("date", 1526817691000.0); // time since unix epoch in milliseconds
        sgv_object.put("sgv", 158); // value in mgdl
        sgv_object.put("direction", "SingleUp"); // represents change per minute
        // see BgReading.slopefromName() for the values which convert to mgdl per minute boundaries
        // if this is not fine grained enough we can add ability to receive raw slope value

        sgv_array.put(sgv_object); // put the sgv in to the array

        intent.putExtra("data", sgv_array.toString()); // convert to json string

        // INTENT CREATION COMPLETED

        // set up our test environment to be clean and ready to receive
        DexCollectionType.setDexCollectionType(DexCollectionType.NSEmulator); // select the collector type
        Sensor.create(1000); // create a dummy sensor
        BgReading.deleteALL(); // delete any bg readings in database

        // for test we push value directly in to the receiver
        // from another app you would use sendBroadcast() with the intent
        BroadcastReceiver br = new NSEmulatorReceiver(); // create test receiver
        br.onReceive(RuntimeEnvironment.application, intent); // push our intent in

        // Processing occurs on a thread so we wait to see if its processed up to 10 seconds
        // null failures here could be due to slow processing
        BgReading result = null;
        for (int loop = 0; loop < 20; loop++) {
            result = BgReading.lastNoSenssor();
            if (result != null) {
                break;
            } else {
                JoH.threadSleep(500);
            }
        }

        Pref.removeItem(DexCollectionType.DEX_COLLECTION_METHOD); // clean up collector type

        // Validate record is correct
        assertWithMessage("result not null").that(result).isNotNull();
        assertWithMessage("direction matches").that(result.slopeName()).isEqualTo("SingleUp");
        assertWithMessage("timestamp matches").that(result.timestamp).isEqualTo(1526817691000L);
        assertWithMessage("sgv matches 1").that(result.calculated_value).isEqualTo(158d);
        assertWithMessage("sgv matches 2").that(result.filtered_calculated_value).isEqualTo(158d);
        assertWithMessage("sgv matches 3").that(result.raw_data).isEqualTo(158d);
        assertWithMessage("sgv matches 4").that(result.filtered_data).isEqualTo(158d);

    }

}