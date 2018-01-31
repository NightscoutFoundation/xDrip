package com.eveningoutpost.dexdrip.dagger;

import com.eveningoutpost.dexdrip.webservices.WebServiceModule;

import dagger.Component;

/**
 * Created by jamorham on 20/09/2017.
 *
 * Interface requires method for every concrete class it is called from
 * placing a base class in the method will not generate a compile error but will get null at runtime
 *
 * Android studio wont tell you when interface methods are no longer needed or are missing
 */

@javax.inject.Singleton
@Component(modules = {WebServiceModule.class})
public interface WebServiceComponent {

    void inject(Singleton target);

}
