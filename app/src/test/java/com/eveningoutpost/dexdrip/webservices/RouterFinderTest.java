package com.eveningoutpost.dexdrip.webservices;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Created by jamorham on 17/01/2018.
 */
public class RouterFinderTest extends RobolectricTestWithConfig {

    private static void log(String msg) {
        System.out.println(msg);
    }


    @Before
    public void cleanup() {
      BgReading.deleteALL();
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
    }

    private void validResponse(String subroute, WebResponse response) {
        assertWithMessage(subroute + " instance null data response")
                .that(response)
                .isNotNull();

        log("\n\n" + subroute + " Result code: " + response.resultCode);
        log(HexDump.dumpHexString(response.bytes));

        assertWithMessage(subroute + " result code")
                .that(response.resultCode)
                .isEqualTo(200);
        assertWithMessage(subroute + " instance data length")
                .that(response.bytes.length)
                .isAtLeast(1);
    }
}