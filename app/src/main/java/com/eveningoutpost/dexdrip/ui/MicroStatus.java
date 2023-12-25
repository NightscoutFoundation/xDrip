package com.eveningoutpost.dexdrip.ui;

/**
 * Created by jamorham on 20/09/2017.
 */

public interface MicroStatus {

    String gs(String id);

    boolean bluetooth();

    boolean xmitterBattery();

    boolean resettableCals (); // This is false when native calibration is strongly advised like Firefly or G7.

}
