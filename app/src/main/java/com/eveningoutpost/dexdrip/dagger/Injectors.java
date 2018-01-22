package com.eveningoutpost.dexdrip.dagger;

/**
 * Created by jamorham on 20/09/2017.
 *
 * Injector Component singletons
 */

public class Injectors {

    private static MicroStatusComponent microStatusComponent;
    private static HomeShelfComponent homeShelfComponent;
    private static WebServiceComponent webServiceComponent;

    public static MicroStatusComponent getMicroStatusComponent() {
        if (microStatusComponent == null) {
            microStatusComponent = DaggerMicroStatusComponent.create();
        }
        return microStatusComponent;
    }

    public static HomeShelfComponent getHomeShelfComponent() {
        if (homeShelfComponent == null) {
            homeShelfComponent = DaggerHomeShelfComponent.create();
        }
        return homeShelfComponent;
    }

    public static WebServiceComponent getWebServiceComponent() {
        if (webServiceComponent == null) {
            webServiceComponent = DaggerWebServiceComponent.create();
        }
        return webServiceComponent;
    }}
