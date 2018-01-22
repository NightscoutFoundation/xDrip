
package com.eveningoutpost.dexdrip.ui;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;


/**
 * Created by jamorham on 20/09/2017.
 */

@Module
public class HomeShelfModule {

    @Provides
    @Singleton
    BaseShelf providesHomeShelf() {
        return new HomeShelfImpl();
    }


}
