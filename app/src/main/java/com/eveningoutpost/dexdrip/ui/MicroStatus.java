package com.eveningoutpost.dexdrip.ui;

/**
 * Created by jamorham on 20/09/2017.
 */

public interface MicroStatus {

    String gs(String id);

    boolean bluetooth();

    boolean xmitterBattery();

    boolean sessionStartTime(); // Show session start time on the classic status page only when true (not G7)

}
