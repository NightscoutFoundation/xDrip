package com.eveningoutpost.dexdrip.webservices;

/**
 * Created by jamorham on 09/01/2018.
 *
 * Calls the WebService module associated with the route
 *
 */

class RouteFinder {

    static WebResponse handleRoute(String route) {

        WebResponse response;
        // find a module based on our query string
        if (route.startsWith("pebble")) {
            // support for pebble nightscout watchface emulates /pebble Nightscout endpoint
            response = WebServicePebble.getInstance().request(route);
        } else if (route.startsWith("tasker/")) {
            // forward the request to tasker interface
            response = WebServiceTasker.getInstance().request(route);
        } else if (route.startsWith("sgv.json")) {
            // support for nightscout style sgv.json endpoint
            response = WebServiceSgv.getInstance().request(route);
        } else if (route.startsWith("steps/")) {
            // support for working with step counter
            response = WebServiceSteps.getInstance().request(route);
        } else {
            // error not found
            response = new WebResponse("Path not found: " + route + "\r\n", 404, "text/plain");
        }

        return response;
    }

}
