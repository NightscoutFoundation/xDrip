package com.eveningoutpost.dexdrip.glucosemeter.glucomen.devices;


/**
 * JamOrHam
 * GlucoMen Aero 2K device
 */

public class Aero2kDevice extends BaseDevice {

    {
        serialOffset = 6;
        indexOffset = 8;
        glucoseStart = 209;
        glucoseSize = 2;
        ketoneStart = 9;
        ketoneSize = 2;
        known = true;
    }
}
