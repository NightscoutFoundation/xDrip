package com.eveningoutpost.dexdrip.webservices;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.DateUtil;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.UtilityModels.NanoStatus;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.SensorStatus;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Created by jamorham on 17/01/2018.
 */
public class RouteFinderTest extends RobolectricTestWithConfig {

    private static void log(String msg) {
        System.out.println(msg);
    }

    @Before
    public void cleanup() {
      BgReading.deleteALL();
      Treatments.delete_all();
      Sensor.createDefaultIfMissing();
    }

    @Test
    public void test_RouteFinder() {
        WebResponse response;
        final RouteFinder routeFinder = new RouteFinder();

        assertWithMessage("RouteFinder instance")
                .that(routeFinder)
                .isNotNull();

        // test routes

        response = routeFinder.handleRoute("bogus route");
        assertWithMessage("bogus route not found")
                .that(response.resultCode)
                .isEqualTo(404);

        response = routeFinder.handleRoute("pebble");

        assertWithMessage("Pebble instance null data response")
                .that(response)
                .isNull();
        // TODO create some record data for pebble to use, check without it also

        String subroute;

        // sgv
        subroute = "sgv.json";
        response = routeFinder.handleRoute(subroute);
        validResponse(subroute, response);
        assertWithMessage(subroute + " instance data format")
                .that(response.bytes[0])
                .isEqualTo('[');

        // treatments
        subroute = "treatments.json";
        response = routeFinder.handleRoute(subroute);
        validResponse(subroute, response);
        assertWithMessage(subroute + " instance data format")
                .that(response.bytes[0])
                .isEqualTo('[');


        // status
        subroute = "status.json";
        response = routeFinder.handleRoute(subroute);
        validResponse(subroute, response);
        assertWithMessage(subroute + " instance data format")
                .that(response.bytes[0])
                .isEqualTo('{');


        // tasker
        subroute = "tasker/snooze";
        response = routeFinder.handleRoute(subroute);
        validResponse(subroute, response);
        assertWithMessage("Contains forwarded to")
                .that(new String(response.bytes))
                .startsWith("Forwarded to");

        // heart
        subroute = "heart/set/124/1";
        response = routeFinder.handleRoute(subroute);
        validResponse(subroute, response);
        assertWithMessage("Contains updated")
                .that(new String(response.bytes))
                .startsWith("Updated");

        // steps
        subroute = "steps/set/123";
        response = routeFinder.handleRoute(subroute);
        validResponse(subroute, response);
        assertWithMessage("Contains updated")
                .that(new String(response.bytes))
                .startsWith("Updated");

        // sgv combined
        subroute = "sgv.json?steps=1234&heart=123&tasker=osnooze";
        response = routeFinder.handleRoute(subroute);
        validResponse(subroute, response);
        // TODO look for output markers

        // sgv with server exception
        subroute = "sgv.json?test_trigger_exception=1";
        response = routeFinder.handleRoute(subroute);
        responseWithStatusCode(subroute, response, 500);
        assertWithMessage(subroute + " instance error text")
                .that(new String(response.bytes))
                .startsWith("Exception in "+WebServiceSgv.class.toString());
    }

    @Test
    public void test_WebServiceTreatments() {
        final RouteFinder routeFinder = new RouteFinder();

        // Test treatments.json with no data
        WebResponse response = routeFinder.handleRoute("treatments.json");
        validResponse("empty treatments", response);
        assertWithMessage("empty treatments contains expected JSON")
                .that(new String(response.bytes))
                .isEqualTo("[]");

        // Test treatments.json?no_empty=1 with no data
        response = routeFinder.handleRoute("treatments.json?no_empty=1");
        // Do not check validResponse since the intended result is 0 bytes
        assertWithMessage("empty treatments with no_empty contains expected JSON")
                .that(new String(response.bytes))
                .isEqualTo("");

        // Create example treatments (Treatments.create uses epoch seconds)
        long time = Instant.now().getEpochSecond();
        Treatments treatmentA = Treatments.create(6, 1, time);
        Treatments treatmentB = Treatments.create(12, 2, time - 1000);
        Treatments treatmentC = Treatments.create(18, 3, time - 2000);

        // Test treatments.json?count=1
        JSONArray expectedResponse = new JSONArray();
        expectedResponse.put(buildExpectedTreatmentJSON(treatmentA));

        response = routeFinder.handleRoute("treatments.json?count=1");
        validResponse("treatments with count 1", response);
        assertWithMessage("treatments with count 1 contains expected JSON")
                .that(new String(response.bytes))
                .isEqualTo(expectedResponse.toString());

        // Test treatments.json?count=2
        expectedResponse.put(buildExpectedTreatmentJSON(treatmentB));

        response = routeFinder.handleRoute("treatments.json?count=2");
        validResponse("treatments with count 1", response);
        assertWithMessage("treatments with count 2 contains expected JSON")
                .that(new String(response.bytes))
                .isEqualTo(expectedResponse.toString());

        // Test treatments.json with all 3 treatments
        expectedResponse.put(buildExpectedTreatmentJSON(treatmentC));

        response = routeFinder.handleRoute("treatments.json");
        validResponse("treatments with no count", response);
        assertWithMessage("treatments with no count contains expected JSON")
                .that(new String(response.bytes))
                .isEqualTo(expectedResponse.toString());

        // Ensure that all 3 treatments were cached
        assertWithMessage("all 3 treatments were cached")
                .that(WebServiceTreatments.cachedTreatments)
                .hasSize(3);

        // Add a new treatment that is newer than the others
        Treatments treatmentD = Treatments.create(24, 4, time + 1000);

        // Test treatments.json with all 4 treatments
        expectedResponse = new JSONArray();
        expectedResponse.put(buildExpectedTreatmentJSON(treatmentD));
        expectedResponse.put(buildExpectedTreatmentJSON(treatmentA));
        expectedResponse.put(buildExpectedTreatmentJSON(treatmentB));
        expectedResponse.put(buildExpectedTreatmentJSON(treatmentC));

        response = routeFinder.handleRoute("treatments.json");
        validResponse("treatments with newer treatment", response);
        assertWithMessage("treatments with newer treatment contains expected JSON")
                .that(new String(response.bytes))
                .isEqualTo(expectedResponse.toString());

        // Ensure that all 4 treatments were cached
        assertWithMessage("all 4 treatments were cached")
                .that(WebServiceTreatments.cachedTreatments)
                .hasSize(4);
    }

    private JSONObject buildExpectedTreatmentJSON(Treatments treatment) {
        try {
            JSONObject item = new JSONObject();
            item.put("_id", treatment.uuid);
            item.put("created_at", treatment.timestamp);
            item.put("eventType", treatment.eventType);
            item.put("enteredBy", treatment.enteredBy);
            item.put("notes", treatment.notes);
            item.put("carbs", treatment.carbs);
            item.put("insulin", treatment.insulin);
            return item;
        } catch (JSONException e) {
            return null;
        }
    }

    @Test
    public void test_WebServiceSgv() {
        final RouteFinder routeFinder = new RouteFinder();

        // Test sgv.json with no data
        WebResponse response = routeFinder.handleRoute("sgv.json");
        validResponse("empty sgv", response);

        assertWithMessage("empty sgv contains expected JSON")
                .that(new String(response.bytes))
                .isEqualTo("[]");

        // Test sgv.json?no_empty=1 with no data
        response = routeFinder.handleRoute("sgv.json?no_empty=1");
        // Do not check validResponse since the intended result is 0 bytes
        assertWithMessage("empty sgv with no_empty contains expected JSON")
                .that(new String(response.bytes))
                .isEqualTo("");

        // Create example SGVs (bgReadingInsertFromG5 uses epoch milliseconds)
        long time = System.currentTimeMillis();
        BgReading readingA = BgReading.bgReadingInsertFromG5(120, time);
        BgReading readingB = BgReading.bgReadingInsertFromG5(115, time - 300000);
        BgReading readingC = BgReading.bgReadingInsertFromG5(110, time - 600000);

        // Test sgv.json?count=1
        JSONArray expectedResponse = new JSONArray();
        expectedResponse.put(buildExpectedBgReadingJSON(readingA, false, true));

        response = routeFinder.handleRoute("sgv.json?count=1");
        validResponse("sgv with count 1", response);
        assertWithMessage("sgv with count 1 contains expected JSON")
                .that(new String(response.bytes))
                .isEqualTo(expectedResponse.toString());

        // Ensure that 1 BgReading was cached
        assertWithMessage("1 bgreading was cached")
                .that(WebServiceSgv.cachedReadings)
                .hasSize(1);

        assertWithMessage("JSON for only 1 sgv was cached")
                .that(WebServiceSgv.cachedJson)
                .hasSize(1);

        // Test sgv.json?brief_mode=1
        expectedResponse = new JSONArray();
        expectedResponse.put(buildExpectedBgReadingJSON(readingA, true, true));
        expectedResponse.put(buildExpectedBgReadingJSON(readingB, true, false));
        expectedResponse.put(buildExpectedBgReadingJSON(readingC, true, false));

        response = routeFinder.handleRoute("sgv.json?brief_mode=1");
        validResponse("sgv with brief mode", response);
        assertWithMessage("sgv with brief mode contains expected JSON")
                .that(new String(response.bytes))
                .isEqualTo(expectedResponse.toString());

        // Ensure that all 3 BgReadings were cached
        assertWithMessage("all 3 bgreadings were cached")
                .that(WebServiceSgv.cachedReadings)
                .hasSize(3);

        // Ensure that JSON for no more SGVs have been cached
        // (with brief_mode, JSON is not cached)
        assertWithMessage("brief_mode did not add to cached sgv JSON")
                .that(WebServiceSgv.cachedJson)
                .hasSize(1);

        // Test sgv.json with all 3 readings
        expectedResponse = new JSONArray();
        expectedResponse.put(buildExpectedBgReadingJSON(readingA, false, true));
        expectedResponse.put(buildExpectedBgReadingJSON(readingB, false, false));
        expectedResponse.put(buildExpectedBgReadingJSON(readingC, false, false));

        response = routeFinder.handleRoute("sgv.json");
        validResponse("sgv with no count", response);
        assertWithMessage("sgv with no count contains expected JSON")
                .that(new String(response.bytes))
                .isEqualTo(expectedResponse.toString());

        // Ensure that JSON for all 3 sgvs were cached
        assertWithMessage("JSON for all 3 sgvs were cached")
                .that(WebServiceSgv.cachedJson)
                .hasSize(3);

        // Test sgv.json?collector=1
        expectedResponse = new JSONArray();
        JSONObject reading = buildExpectedBgReadingJSON(readingA, false, true);
        try {
            reading.put("collector_status", NanoStatus.nanoStatus("collector"));
        } catch (JSONException e) {
            // ignore
        }
        expectedResponse.put(reading);
        expectedResponse.put(buildExpectedBgReadingJSON(readingB, false, false));
        expectedResponse.put(buildExpectedBgReadingJSON(readingC, false, false));

        response = routeFinder.handleRoute("sgv.json?collector=1");
        validResponse("sgv with collector mode", response);
        assertWithMessage("sgv with collector mode contains expected JSON")
                .that(new String(response.bytes))
                .isEqualTo(expectedResponse.toString());

        // Test sgv.json?sensor=1
        expectedResponse = new JSONArray();
        reading = buildExpectedBgReadingJSON(readingA, false, true);
        try {
            reading.put("sensor_status", SensorStatus.status());
        } catch (JSONException e) {
            // ignore
        }
        expectedResponse.put(reading);
        expectedResponse.put(buildExpectedBgReadingJSON(readingB, false, false));
        expectedResponse.put(buildExpectedBgReadingJSON(readingC, false, false));

        response = routeFinder.handleRoute("sgv.json?sensor=1");
        validResponse("sgv with collector mode", response);
        assertWithMessage("sgv with collector mode contains expected JSON")
                .that(new String(response.bytes))
                .isEqualTo(expectedResponse.toString());

    }

    private JSONObject buildExpectedBgReadingJSON(BgReading reading, boolean brief, boolean unitsHint) {
        final String collector_device = DexCollectionType.getBestCollectorHardwareName();
        JSONObject item = new JSONObject();

        try {
            if (!brief) {
                item.put("_id", reading.uuid);
                item.put("device", collector_device);
                item.put("dateString", DateUtil.toNightscoutFormat(reading.timestamp));
                item.put("sysTime", DateUtil.toNightscoutFormat(reading.timestamp));
            }

            item.put("date", reading.timestamp);
            item.put("sgv", (int) reading.getDg_mgdl());
            item.put("delta", new BigDecimal(reading.getDg_slope() * 5 * 60 * 1000).setScale(3, BigDecimal.ROUND_HALF_UP));
            item.put("direction", reading.getDg_deltaName());
            item.put("noise", reading.noiseValue());

            if (!brief) {
                item.put("filtered", (long) (reading.filtered_data * 1000));
                item.put("unfiltered", (long) (reading.raw_data * 1000));
                item.put("rssi", 100);
                item.put("type", "sgv");
            }

            // The units hint only appears on the first entry.
            if (unitsHint) {
                item.put("units_hint", Pref.getString("units", "mgdl"));
            }

            return item;
        } catch (JSONException | NumberFormatException e) {
            return null;
        }
    }

    private void validResponse(String subroute, WebResponse response) {
        responseWithStatusCode(subroute, response, 200);
    }
    private void responseWithStatusCode(String subroute, WebResponse response, int status) {
        assertWithMessage(subroute + " instance null data response")
                .that(response)
                .isNotNull();

        log("\n\n" + subroute + " Result code: " + response.resultCode);
        log(HexDump.dumpHexString(response.bytes));

        assertWithMessage(subroute + " result code")
                .that(response.resultCode)
                .isEqualTo(status);
        assertWithMessage(subroute + " instance data length")
                .that(response.bytes.length)
                .isAtLeast(1);
    }
}