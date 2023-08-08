package com.eveningoutpost.dexdrip;

import com.eveningoutpost.dexdrip.models.BgReading;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static com.google.common.truth.Truth.assertWithMessage;

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
}