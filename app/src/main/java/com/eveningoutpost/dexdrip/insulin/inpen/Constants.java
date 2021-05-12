package com.eveningoutpost.dexdrip.insulin.inpen;

import java.util.UUID;

import static com.eveningoutpost.dexdrip.utils.bt.Helper.INFO_FIRMWARE_VERSION;
import static com.eveningoutpost.dexdrip.utils.bt.Helper.INFO_HARDWARE_VERSION;
import static com.eveningoutpost.dexdrip.utils.bt.Helper.INFO_MANUFACTURER;
import static com.eveningoutpost.dexdrip.utils.bt.Helper.INFO_MODEL_NUMBER;
import static com.eveningoutpost.dexdrip.utils.bt.Helper.INFO_PNP_ID;
import static com.eveningoutpost.dexdrip.utils.bt.Helper.INFO_REGULATORY_ID;
import static com.eveningoutpost.dexdrip.utils.bt.Helper.INFO_SERIAL_NUMBER;
import static com.eveningoutpost.dexdrip.utils.bt.Helper.INFO_SOFTWARE_VERSION;
import static com.eveningoutpost.dexdrip.utils.bt.Helper.INFO_SYSTEM_ID;

// jamorham

public class Constants {

    static final String SCAN_SERVICE_UUID = "0000bfd0-0000-1000-8000-00805f9b34fb";

    static final UUID DEVICE_INFO_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    static final UUID BATTERY = UUID.fromString("c8a1bfdd-bc3f-493c-af8d-ae9824f583c6");
    static final UUID PEN_ATTACH_TIME = UUID.fromString("c8a1bfd2-bc3f-493c-af8d-ae9824f583c6");
    static final UUID PEN_TIME = UUID.fromString("c8a1bfd4-bc3f-493c-af8d-ae9824f583c6");
    static final UUID AUTHENTICATION = UUID.fromString("c8a1bfe3-bc3f-493c-af8d-ae9824f583c6");
    static final UUID KEEPALIVE = UUID.fromString("c8a1bfdf-bc3f-493c-af8d-ae9824f583c6");
    static final UUID BONDCONTROL = UUID.fromString("c8a1bfdb-bc3f-493c-af8d-ae9824f583c6");
    static final UUID RECORD_INDEX = UUID.fromString("c8a1bfde-bc3f-493c-af8d-ae9824f583c6");
    static final UUID REMAINING_INDEX = UUID.fromString("c8a1bfe0-bc3f-493c-af8d-ae9824f583c6");
    static final UUID RECORD_REQUEST = UUID.fromString("c8a1bfda-bc3f-493c-af8d-ae9824f583c6");
    static final UUID RECORD_START = UUID.fromString("c8a1bfe1-bc3f-493c-af8d-ae9824f583c6");
    static final UUID RECORD_END = UUID.fromString("c8a1bfe2-bc3f-493c-af8d-ae9824f583c6");
    static final UUID RECORD_INDICATE = UUID.fromString("c8a1bfd1-bc3f-493c-af8d-ae9824f583c6");

    static final UUID[] INFO_CHARACTERISTICS = {INFO_MODEL_NUMBER,
            INFO_FIRMWARE_VERSION, INFO_HARDWARE_VERSION, INFO_SOFTWARE_VERSION, INFO_MANUFACTURER,
            INFO_REGULATORY_ID, INFO_PNP_ID, INFO_SERIAL_NUMBER, INFO_SYSTEM_ID};

    static final UUID[] PRINTABLE_INFO_CHARACTERISTICS = {INFO_MODEL_NUMBER, INFO_FIRMWARE_VERSION,
            INFO_HARDWARE_VERSION, INFO_MANUFACTURER};

    static final UUID[] HEXDUMP_INFO_CHARACTERISTICS = {INFO_MODEL_NUMBER, INFO_SERIAL_NUMBER, INFO_SYSTEM_ID,
            INFO_FIRMWARE_VERSION, INFO_HARDWARE_VERSION, INFO_MANUFACTURER};

    public static final float MULTIPLIER = 2.0f;
    public static final int COUNTER_START = 0;
    public static final int COUNTER_ID = 195850905;

}
