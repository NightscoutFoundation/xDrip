package com.eveningoutpost.dexdrip.UtilityModels;

/**
 * Created by stephenblack on 10/26/14.
 */
public class HM10Attributes {
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String HM_10_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String HM_RX_TX = "0000ffe1-0000-1000-8000-00805f9b34fb";

    // Experimental support for "Transmiter PL" from Marek Macner @FPV-UAV
    public static final String TRANSMITER_PL_SERVICE = "c97433f0-be8f-4dc8-b6f0-5343e6100eb4";
    public static final String TRANSMITER_PL_RX_TX   = "c97433f1-be8f-4dc8-b6f0-5343e6100eb4";

    // Experimental support for rfduino from Tomasz Stachowicz
    public static final String HM_TX = "0000ffe2-0000-1000-8000-00805f9b34fb";

}
