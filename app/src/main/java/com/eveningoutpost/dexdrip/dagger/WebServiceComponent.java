package com.eveningoutpost.dexdrip.dagger;

import com.eveningoutpost.dexdrip.webservices.RouteFinder;
import com.eveningoutpost.dexdrip.webservices.WebServiceModule;
import com.eveningoutpost.dexdrip.webservices.WebServiceSgv;
import com.eveningoutpost.dexdrip.webservices.XdripWebService;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by jamorham on 20/09/2017.
 *
 * Interface requires method for every concrete class it is called from
 * placing a base class in the method will not generate a compile error but will get null at runtime
 *
 */

@Singleton
@Component(modules = {WebServiceModule.class})
public interface WebServiceComponent {

    void inject(XdripWebService target);
    void inject(RouteFinder target);
    void inject(WebServiceSgv target);

}
