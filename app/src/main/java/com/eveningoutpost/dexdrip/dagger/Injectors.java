package com.eveningoutpost.dexdrip.dagger;

/**
 * Created by jamorham on 20/09/2017.
 * <p>
 * Injector Component singletons
 */

public class Injectors {

    private static MicroStatusComponent msComponent;
    private static HomeShelfComponent hsComponent;

    public static MicroStatusComponent getMicroStatusComponent() {
        if (msComponent == null) {
            msComponent = DaggerMicroStatusComponent.create();
        }
        return msComponent;
    }

    public static HomeShelfComponent getHomeShelfComponent() {
        if (hsComponent == null) {
            hsComponent = DaggerHomeShelfComponent.create();
        }
        return hsComponent;
    }

}
