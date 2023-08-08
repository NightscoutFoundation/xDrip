package com.eveningoutpost.dexdrip.webservices;

import com.eveningoutpost.dexdrip.models.HeartRate;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import java.util.List;

/**
 * Created by jamorham on 06/01/2018.
 * <p>
 * allows recording of heart rate
 * <p>
 * eg http://127.0.0.1:17580/heart/set/123/1
 *
 * second digit is "accuracy" measure if available, use 1 if unknown
 * <p>
 */

public class WebServiceHeart extends BaseWebService {

    private static String TAG = "WebServiceHeart";

    // process the request and produce a response object
    public WebResponse request(String query) {

        query = stripFirstComponent(query);

        List<String> components = getUrlComponents(query);

        if (components.size() > 0) {
            UserError.Log.d(TAG, "Processing " + query);
            switch (components.get(0)) {

                case "set":
                    if (components.size() == 3) {
                        // sets heart data NOW, must be current heart BPM reading only
                        try {
                            final int data = Integer.parseInt(components.get(1));
                            final int accuracy = Integer.parseInt(components.get(2));
                            HeartRate.create(JoH.tsl(), data, accuracy);
                            return webOk("Updated heart rate: " + data);
                        } catch (NumberFormatException e) {
                            return webError("Couldn't parse Set value: " + components.get(1) + " " + components.get(2));
                        }
                    } else {
                        return webError("Incorrect parameters for Set", 400);
                    }
                    //break;
                default:
                    return webError("Unknown heart command: " + components.get(0));
            }

        } else {
            return webError("No heart command specified", 404);
        }
    }
}