package com.eveningoutpost.dexdrip.dagger;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.ui.HomeShelfModule;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by jamorham on 20/09/2017.
 *
 * Interface requires method for every concrete class it is called from
 *
 */

@Singleton
@Component(modules = {HomeShelfModule.class})
public interface HomeShelfComponent {

    void inject(Home target);

}

