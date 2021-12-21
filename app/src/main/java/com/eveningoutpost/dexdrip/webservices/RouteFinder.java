package com.eveningoutpost.dexdrip.webservices;

import com.eveningoutpost.dexdrip.dagger.Singleton;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

/**
 * Created by jamorham on 17/01/2018.
 *
 * Calls the WebService module associated with the route
 *
 */

public class RouteFinder {

    private final List<RouteInfo> routes = new ArrayList<>();


    RouteFinder() {
        // route url starts with , class name to process it

        // support for desert sync
        routes.add(new RouteInfo("sync/", "WebServiceSync").useRaw());

        // support for pebble nightscout watchface emulates /pebble Nightscout endpoint
        routes.add(new RouteInfo("pebble", "WebServicePebble"));

        // support for nightscout style sgv.json endpoint
        routes.add(new RouteInfo("sgv.json", "WebServiceSgv"));
        routes.add(new RouteInfo("api/v1/entries/sgv.json", "WebServiceSgv"));

        // support for nightscout style treatments.json endpoint
        routes.add(new RouteInfo("treatments.json", "WebServiceTreatments"));
        routes.add(new RouteInfo("api/v1/treatments.json", "WebServiceTreatments"));

        // support for nightscout style barebones status.json endpoint
        routes.add(new RouteInfo("status.json", "WebServiceStatus"));

        // support for working with step counter
        routes.add(new RouteInfo("steps/", "WebServiceSteps"));

        // support for working with heart monitor
        routes.add(new RouteInfo("heart/", "WebServiceHeart"));

        // tasker interface
        routes.add(new RouteInfo("tasker/", "WebServiceTasker"));

        // libre2 start connection code.
        routes.add(new RouteInfo("Libre2ConnectCode.json", "Libre2ConnectCode"));
    }

    // process a received route
    WebResponse handleRoute(final String route) {
        return handleRoute(route, null);
    }

    // process a received route with source details
    WebResponse handleRoute(final String route, final InetAddress source) {

        for (final RouteInfo routeEntry : routes) {
            if (route.startsWith(routeEntry.path)) {
                return routeEntry.processRequest(route, source);
            }
        }
        // unknown service error reply
        return new WebResponse("Path not found: " + route + "\r\n", 404, "text/plain");
    }


    @RequiredArgsConstructor
    private static final class RouteInfo {
        public final String path;
        public final String module;
        boolean raw = false;

        RouteInfo useRaw() {
            raw = true;
            return this;
        }

        BaseWebService getService() {
            return (BaseWebService) Singleton.get(module);
        }

        WebResponse processRequest(final String route, final InetAddress source) {
            try {
                return getService().request(raw ? route : URLDecoder.decode(route, "UTF-8"), source);
            } catch (UnsupportedEncodingException e) {
                return new WebResponse("Decoding error", 500, "text/plain");
            }
        }
    }
}
