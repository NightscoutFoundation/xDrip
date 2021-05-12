package com.eveningoutpost.dexdrip.cgm.medtrum;

// jamorham

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum SensorState {

    ErrorUnknown("Error Unknown"),
    NotConnected("Not Connected"),
    WarmingUp1("Warming Up 1"),
    WarmingUp2("Warming Up 2"),
    NotCalibrated("Not Calibrated"),
    Ok("Ok");

    @Getter
    String description;
}

