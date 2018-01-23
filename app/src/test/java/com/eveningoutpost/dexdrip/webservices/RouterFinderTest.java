package com.eveningoutpost.dexdrip.webservices;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Created by jamorham on 17/01/2018.
 */

@RunWith(RobolectricTestRunner.class)

//@Config(constants = BuildConfig.class, manifest = "../../../../app/src/test/java/com/eveningoutpost/dexdrip/TestingManifest.xml") // use this config inside android studio 3 or set Android JUnit default working directory to $MODULE_DIR$
@Config(constants = BuildConfig.class, manifest = "../../../../../src/test/java/com/eveningoutpost/dexdrip/TestingManifest.xml")
// use this config for CI test hosts

public class RouterFinderTest {

    private static void log(String msg) {
        System.out.println(msg);
    }

    @Before
    public void setUp() throws Exception {
        xdrip.checkAppContext(RuntimeEnvironment.application);
    }


    @Test
    public void test_RouteFinder() throws Exception {

        WebResponse response;

        final RouteFinder routeFinder = new RouteFinder();

        Truth.assertWithMessage("RouteFinder instance").that(routeFinder).isNotNull();

        // test routes

        response = routeFinder.handleRoute("bogus route");
        Truth.assertWithMessage("bogus route not found").that(response.resultCode).isEqualTo(404);

        response = routeFinder.handleRoute("pebble");

        Truth.assertWithMessage("Pebble instance null data response").that(response).isNull();
        // TODO create some record data for pebble to use, check without it also

        String subroute;

        // sgv
        subroute = "sgv.json";
        response = routeFinder.handleRoute(subroute);
        validResponse(subroute, response);
        Truth.assertWithMessage(subroute + " instance data format").that(response.bytes[0] == '[').isTrue();

        // tasker
        subroute = "tasker/snooze";
        response = routeFinder.handleRoute(subroute);
        validResponse(subroute, response);
        Truth.assertWithMessage("Contains forwarded to").that(new String(response.bytes).startsWith("Forwarded to")).isTrue();

        // heart
        subroute = "heart/set/124/1";
        response = routeFinder.handleRoute(subroute);
        validResponse(subroute, response);
        Truth.assertWithMessage("Contains updated").that(new String(response.bytes).startsWith("Updated")).isTrue();

        // steps
        subroute = "steps/set/123";
        response = routeFinder.handleRoute(subroute);
        validResponse(subroute, response);
        Truth.assertWithMessage("Contains updated").that(new String(response.bytes).startsWith("Updated")).isTrue();

        // sgv combined
        subroute = "sgv.json?steps=1234&heart=123&tasker=osnooze";
        response = routeFinder.handleRoute(subroute);
        validResponse(subroute, response);
        // TODO look for output markers
    }

    private void validResponse(String subroute, WebResponse response) {
        Truth.assertWithMessage(subroute + " instance null data response").that(response).isNotNull();
        log("\n\n" + subroute + " Result code: " + response.resultCode);
        log(HexDump.dumpHexString(response.bytes));

        Truth.assertWithMessage(subroute + " result code").that(response.resultCode == 200).isTrue();
        Truth.assertWithMessage(subroute + " instance data length").that(response.bytes.length > 1).isTrue();

    }

}
