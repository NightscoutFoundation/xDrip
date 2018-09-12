package com.eveningoutpost.dexdrip.UtilityModels;

import com.eveningoutpost.dexdrip.G5Model.TransmitterStatus;

/**
 * Abstract representation of information about a transmitter battery and
 * the states of each specific parameter.
 *
 * @author James Woglom (j@wogloms.net)
 */
public interface TransmitterBattery {
    TransmitterStatus status();
    int days();
    int voltageA();

    enum VoltageStatus {
        GOOD,
        WARNING
    }
    VoltageStatus voltageAStatus();
    int voltageB();
    VoltageStatus voltageBStatus();
    int resistance();

    enum ResistanceStatus {
        GOOD,
        NORMAL,
        NOTICE,
        BAD
    }
    ResistanceStatus resistanceStatus();
    int temperature();

    String battery();
}
