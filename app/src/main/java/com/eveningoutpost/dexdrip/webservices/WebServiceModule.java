package com.eveningoutpost.dexdrip.webservices;

import android.util.Log;

import com.eveningoutpost.dexdrip.Models.UserError;

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

    private static final String TAG = "WebServiceModule-INJECT";

    @Provides
    @Singleton
    @Named("RouteFinder")
    RouteFinder providesRouteFinder() {
        UserError.Log.d(TAG, "creating RouteFinder");
        return new RouteFinder();
    }

    @Provides
    @Singleton
    @Named("WebServicePebble")
    BaseWebService providesWebServicePebble() {
        UserError.Log.d(TAG, "creating WebServicePebble");
        return new WebServicePebble();
    }

    @Provides
    @Singleton
    @Named("WebServiceSgv")
    BaseWebService providesWebServiceSgv() {
        UserError.Log.d(TAG, "creating WebServiceSgv");
        return new WebServiceSgv();
    }

    @Provides
    @Singleton
    @Named("WebServiceTreatments")
    BaseWebService providesWebServiceTreatments() {
        UserError.Log.d(TAG, "creating WebServiceTreatments");
        return new WebServiceTreatments();
    }

    @Provides
    @Singleton
    @Named("WebServiceStatus")
    BaseWebService providesWebServiceStatus() {
        UserError.Log.d(TAG, "creating WebServiceStatus");
        return new WebServiceStatus();
    }

    @Provides
    @Singleton
    @Named("WebServiceTasker")
    BaseWebService providesWebServiceTasker() {
        UserError.Log.d(TAG, "creating WebServiceTasker");
        return new WebServiceTasker();
    }

    @Provides
    @Singleton
    @Named("WebServiceSteps")
    BaseWebService providesWebServiceSteps() {
        UserError.Log.d(TAG, "creating WebServiceSteps");
        return new WebServiceSteps();
    }

    @Provides
    @Singleton
    @Named("WebServiceHeart")
    BaseWebService providesWebServiceHeart() {
        UserError.Log.d(TAG, "creating WebServiceHeart");
        return new WebServiceHeart();
    }

    @Provides
    @Singleton
    @Named("WebServiceSync")
    BaseWebService providesWebServiceSync() {
        UserError.Log.d(TAG, "creating WebServiceSync");
        return new WebServiceSync();
    }

    @Provides
    @Singleton
    @Named("Libre2ConnectCode")
    BaseWebService providesLibre2ConnectCode() {
        UserError.Log.d(TAG, "creating Libre2ConnectCode");
        return new WebLibre2ConnectCode();
    }

}
