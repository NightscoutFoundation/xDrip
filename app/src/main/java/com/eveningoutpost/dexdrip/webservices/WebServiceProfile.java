package com.eveningoutpost.dexdrip.webservices;

import org.json.JSONArray;

/**
 * @author James Woglom (j@wogloms.net)
 */

public class WebServiceProfile extends BaseWebService {

    private static String TAG = "WebServiceProfile";

    public WebResponse request(String query) {
        final JSONArray wrapper = new JSONArray();

        return new WebResponse(wrapper.toString());
    }


}
