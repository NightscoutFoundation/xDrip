package com.eveningoutpost.dexdrip.webservices;

import android.util.Log;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;


/**
 * Created by jamorham on 20/12/2017.
 *
 * dagger module
 *
 */

@Module
public class WebServiceModule {

    private static final boolean d = true;

    @Provides
    @Singleton
    @Named("RouteFinder")
    RouteFinder providesRouteFinder() {
        if (d) Log.d("INJECT", "creating RouteFinder");
        return new RouteFinder();
    }

    @Provides
    @Singleton
    @Named("WebServicePebble")
    BaseWebService providesWebServicePebble() {
        if (d) Log.d("INJECT", "creating WebServicePebble");
        return new WebServicePebble();
    }

    @Provides
    @Singleton
    @Named("WebServiceSgv")
    BaseWebService providesWebServiceSgv() {
        if (d) Log.d("INJECT", "creating WebServiceSgv");
        return new WebServiceSgv();
    }

    @Provides
    @Singleton
    @Named("WebServiceStatus")
    BaseWebService providesWebServiceStatus() {
        if (d) Log.d("INJECT", "creating WebServiceStatus");
        return new WebServiceStatus();
    }

    @Provides
    @Singleton
    @Named("WebServiceTasker")
    BaseWebService providesWebServiceTasker() {
        if (d) Log.d("INJECT", "creating WebServiceTasker");
        return new WebServiceTasker();
    }

    @Provides
    @Singleton
    @Named("WebServiceSteps")
    BaseWebService providesWebServiceSteps() {
        if (d) Log.d("INJECT", "creating WebServiceSteps");
        return new WebServiceSteps();
    }

    @Provides
    @Singleton
    @Named("WebServiceHeart")
    BaseWebService providesWebServiceHeart() {
        if (d) Log.d("INJECT", "creating WebServiceHeart");
        return new WebServiceHeart();
    }

    @Provides
    @Singleton
    @Named("WebServiceSync")
    BaseWebService providesWebServiceSync() {
        if (d) Log.d("INJECT", "creating WebServiceSync");
        return new WebServiceSync();
    }



}
