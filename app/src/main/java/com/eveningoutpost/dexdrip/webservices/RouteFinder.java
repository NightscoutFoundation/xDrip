package com.eveningoutpost.dexdrip.webservices;

import com.eveningoutpost.dexdrip.dagger.Injectors;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;

/**
 * Created by jamorham on 17/01/2018.
 *
 * Calls the WebService module associated with the route
 *
 */

public class RouteFinder {

    @Inject
    @Named("WebServicePebble")
    Lazy<BaseWebService> pebbleService;

    @Inject
    @Named("WebServiceTasker")
    Lazy<BaseWebService> taskerService;

    @Inject
    @Named("WebServiceSgv")
    Lazy<BaseWebService> sgvService;

    @Inject
    @Named("WebServiceSteps")
    Lazy<BaseWebService> stepsService;

    @Inject
    @Named("WebServiceHeart")
    Lazy<BaseWebService> heartService;

    RouteFinder() {
        Injectors.getWebServiceComponent().inject(this);
    }

    // process a received route
    WebResponse handleRoute(final String route) {

        BaseWebService service = null;

        // pick the appropriate service for the route
        if (route.startsWith("pebble")) {
            // support for pebble nightscout watchface emulates /pebble Nightscout endpoint
            service = pebbleService.get();
        } else if (route.startsWith("tasker/")) {
            // tasker interface
            service = taskerService.get();
        } else if (route.startsWith("sgv.json")) {
            // support for nightscout style sgv.json endpoint
            service = sgvService.get();
        } else if (route.startsWith("steps/")) {
            // support for working with step counter
            service = stepsService.get();
        } else if (route.startsWith("heart/")) {
            // support for working with step counter
            service = heartService.get();
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


