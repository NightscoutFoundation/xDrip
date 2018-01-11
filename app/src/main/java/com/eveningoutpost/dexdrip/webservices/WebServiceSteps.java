package com.eveningoutpost.dexdrip.webservices;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.PebbleMovement;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.localeTasker.bundle.PluginBundleManager;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.List;

import static com.eveningoutpost.dexdrip.BuildConfig.APPLICATION_ID;
import static com.eveningoutpost.dexdrip.localeTasker.bundle.PluginBundleManager.BUNDLE_EXTRA_INT_VERSION_CODE;

/**
 * Created by jamorham on 06/01/2018.
 * <p>
 * allows setting of step count
 * <p>
 * eg http://127.0.0.1:17580/steps/set/12345
 * <p>
 */

public class WebServiceSteps extends BaseWebService {

    private static String TAG = "WebServiceSteps";
    private static volatile WebServiceSteps instance;


    @NonNull
    public static WebServiceSteps getInstance() {
        if (instance == null) {
            instance = new WebServiceSteps();
        }
        return instance;
    }

    // process the request and produce a response object
    public WebResponse request(String query) {

        query = stripFirstComponent(query);

        List<String> components = getUrlComponents(query);

        if (components.size() > 0) {
            UserError.Log.d(TAG, "Processing " + query);
            switch (components.get(0)) {

                case "set":
                    if (components.size() == 2) {
                        // sets pebble movement data for steps NOW, must be current step counter reading only
                        try {
                            int data = Integer.parseInt(components.get(1));
                            final PebbleMovement pm = PebbleMovement.createEfficientRecord(JoH.tsl(), data);
                            return webOk("Updated step counter: " + data);
                        } catch (NumberFormatException e) {
                            return webError("Couldn't parse Set value: " + components.get(1));
                        }
                    } else {
                        return webError("Incorrect parameters for Set", 400);
                    }
                    //break;
                default:
                    return webError("Unknown steps command: " + components.get(0));
            }

        } else {
            return webError("No steps command specified", 404);
        }
    }
}
