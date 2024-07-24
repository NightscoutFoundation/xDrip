package com.eveningoutpost.dexdrip.webservices;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.StepCounter;
import com.eveningoutpost.dexdrip.models.UserError;

import java.util.List;

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
                            final StepCounter pm = StepCounter.createEfficientRecord(JoH.tsl(), data);
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
