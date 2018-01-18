package com.eveningoutpost.dexdrip.dagger;

import android.app.Activity;

import com.eveningoutpost.dexdrip.SystemStatusFragment;
import com.eveningoutpost.dexdrip.ui.MicroStatusModule;
import com.eveningoutpost.dexdrip.webservices.WebServicePebble;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by jamorham on 20/09/2017.
 *
 * Interface requires method for every concrete class it is called from
 *
 */

@Singleton
@Component(modules = {MicroStatusModule.class})
public interface MicroStatusComponent {

    void inject(SystemStatusFragment target);
    void inject(Activity target);
    void inject(WebServicePebble target);

}

