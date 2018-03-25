package com.eveningoutpost.dexdrip.webservices;

import android.util.Pair;

import com.eveningoutpost.dexdrip.dagger.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jamorham on 17/01/2018.
 *
 * Calls the WebService module associated with the route
 *
 */

public class RouteFinder {

    private final List<Pair<String, String>> routes = new ArrayList<>();

    RouteFinder() {
        // route url starts with , class name to process it

        // support for pebble nightscout watchface emulates /pebble Nightscout endpoint
        routes.add(new Pair<>("pebble", "WebServicePebble"));

        // tasker interface
        routes.add(new Pair<>("tasker/", "WebServiceTasker"));

        // support for nightscout style sgv.json endpoint
        routes.add(new Pair<>("sgv.json", "WebServiceSgv"));

        // support for nightscout style barebones status.json endpoint
        routes.add(new Pair<>("status.json", "WebServiceStatus"));

        // support for working with step counter
        routes.add(new Pair<>("steps/", "WebServiceSteps"));

        // support for working with heart monitor
        routes.add(new Pair<>("heart/", "WebServiceHeart"));
    }

    // process a received route
    WebResponse handleRoute(final String route) {

        BaseWebService service = null;

        for (final Pair<String, String> routeEntry : routes) {
            if (route.startsWith(routeEntry.first)) {
                service = (BaseWebService) Singleton.get(routeEntry.second);
                break;
            }
        }

        if (service != null) {
            // get the response from the service for the route
            return service.request(route);
        } else {
            // unknown service error reply
            return new WebResponse("Path not found: " + route + "\r\n", 404, "text/plain");
        }

    }
}
