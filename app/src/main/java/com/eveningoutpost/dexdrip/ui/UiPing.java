package com.eveningoutpost.dexdrip.ui;

import androidx.databinding.BaseObservable;
import androidx.databinding.ObservableInt;

/**
 * Created by jamorham on 11/03/2018.
 *
 * As a work-around for making observable methods which do not conform to java beans standard,
 * we can have an observable parameter which when notified of change causes the method to fire.
 */

public class UiPing extends BaseObservable {

    public final ObservableInt ping = new ObservableInt();

    public void bump() {
        ping.set(ping.get() + 1);
    }

}
