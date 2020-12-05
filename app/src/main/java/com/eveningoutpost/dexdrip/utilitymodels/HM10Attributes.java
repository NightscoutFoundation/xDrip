package com.eveningoutpost.dexdrip.utilitymodels;

/**
 * Created by Emma Black on 10/26/14.
 */
public class HM10Attributes {
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String HM_10_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String HM_RX_TX = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String HM_DEFAULT_BT_PIN = "000000";

    // Experimental support for "Transmiter PL" from Marek Macner @FPV-UAV
    public static final String TRANSMITER_PL_SERVICE = "c97433f0-be8f-4dc8-b6f0-5343e6100eb4";
    public static final String TRANSMITER_PL_RX_TX   = "c97433f1-be8f-4dc8-b6f0-5343e6100eb4";

    // Experimental support for rfduino from Tomasz Stachowicz
    public static final String HM_TX = "0000ffe2-0000-1000-8000-00805f9b34fb";

    //genric nordic support
    public static final String NRF_UART_SERVICE = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static final String NRF_UART_TX = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
    public static final String NRF_UART_RX = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";

    // blucon support from gregorybel
    public static final String BLUKON_SERVICE = "436a62c0-082e-4ce8-a08b-01d81f195b24";
    public static final String BLUKON_UART_TX = "436aa6e9-082e-4ce8-a08b-01d81f195b24";
    public static final String BLUKON_UART_RX = "436a0c82-082e-4ce8-a08b-01d81f195b24";

}
