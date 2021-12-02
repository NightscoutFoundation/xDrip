package com.eveningoutpost.dexdrip.ImportedLibraries.dexcom;

// This code and this particular library are from the NightScout android uploader
// Check them out here: https://github.com/nightscout/android-uploader
// Some of this code may have been modified for use in this project

public class Dex_Constants {

    public final static int NULL = 0;
    public final static int ACK = 1;
    public final static int NAK = 2;
    public final static int INVALID_COMMAND = 3;
    public final static int INVALID_PARAM = 4;
    public final static int INCOMPLETE_PACKET_RECEIVED = 5;
    public final static int RECEIVER_ERROR = 6;
    public final static int INVALID_MODE = 7;
    public final static int PING = 10;
    public final static int READ_FIRMWARE_HEADER = 11;
    public final static int READ_DATABASE_PARTITION_INFO = 15;
    public final static int READ_DATABASE_PAGE_RANGE = 16;
    public final static int READ_DATABASE_PAGES = 17;
    public final static int READ_DATABASE_PAGE_HEADER = 18;
    public final static int READ_TRANSMITTER_ID = 25;
    public final static int WRITE_TRANSMITTER_ID = 26;
    public final static int READ_LANGUAGE = 27;
    public final static int WRITE_LANGUAGE = 28;
    public final static int READ_DISPLAY_TIME_OFFSET = 29;
    public final static int WRITE_DISPLAY_TIME_OFFSET = 30;
    public final static int READ_RTC = 31;
    public final static int RESET_RECEIVER = 32;
    public final static int READ_BATTERY_LEVEL = 33;
    public final static int READ_SYSTEM_TIME = 34;
    public final static int READ_SYSTEM_TIME_OFFSET = 35;
    public final static int WRITE_SYSTEM_TIME = 36;
    public final static int READ_GLUCOSE_UNIT = 37;
    public final static int WRITE_GLUCOSE_UNIT = 38;
    public final static int READ_BLINDED_MODE = 39;
    public final static int WRITE_BLINDED_MODE = 40;
    public final static int READ_CLOCK_MODE = 41;
    public final static int WRITE_CLOCK_MODE = 42;
    public final static int READ_DEVICE_MODE = 43;
    public final static int ERASE_DATABASE = 45;
    public final static int SHUTDOWN_RECEIVER = 46;
    public final static int WRITE_PC_PARAMETERS = 47;
    public final static int READ_BATTERY_STATE = 48;
    public final static int READ_HARDWARE_BOARD_ID = 49;
    public final static int READ_FIRMWARE_SETTINGS = 54;
    public final static int READ_ENABLE_SETUP_WIZARD_FLAG = 55;
    public final static int READ_SETUP_WIZARD_STATE = 57;
    public final static int MAX_COMMAND = 59;
    public final static int MAX_POSSIBLE_COMMAND = 255;
    public final static int EGV_VALUE_MASK = 1023;
    public final static int EGV_DISPLAY_ONLY_MASK = 32768;
    public final static int EGV_TREND_ARROW_MASK = 15;
    public final static int EGV_NOISE_MASK = 112;
    public final static float MG_DL_TO_MMOL_L = 0.05556f;
    public final static int CRC_LEN = 2;
    public static final int TRANSMITTER_BATTERY_LOW = 210;
    public static final int TRANSMITTER_BATTERY_EMPTY = 207;

    public enum BATTERY_STATES {
        NONE,
        CHARGING,
        NOT_CHARGING,
        NTC_FAULT,
        BAD_BATTERY
    }

    public enum RECORD_TYPES {
        MANUFACTURING_DATA,
        FIRMWARE_PARAMETER_DATA,
        PC_SOFTWARE_PARAMETER,
        SENSOR_DATA,
        EGV_DATA,
        CAL_SET,
        DEVIATION,
        INSERTION_TIME,
        RECEIVER_LOG_DATA,
        RECEIVER_ERROR_DATA,
        METER_DATA,
        USER_EVENT_DATA,
        USER_SETTING_DATA,
        MAX_VALUE
    }

    public enum TREND_ARROW_VALUES {
        NONE(0),
        DOUBLE_UP(1,"\u21C8", "DoubleUp", 40d),
        SINGLE_UP(2,"\u2191", "SingleUp", 3.5d),
        UP_45(3,"\u2197", "FortyFiveUp", 2d),
        FLAT(4,"\u2192", "Flat", 1d),
        DOWN_45(5,"\u2198", "FortyFiveDown", -1d),
        SINGLE_DOWN(6,"\u2193", "SingleDown", -2d),
        DOUBLE_DOWN(7,"\u21CA", "DoubleDown", -3.5d),
        NOT_COMPUTABLE(8, "", "NotComputable"),
        OUT_OF_RANGE(9, "", "RateOutOfRange");

        private String arrowSymbol;
        private String trendName;
        private int myID;
        private Double threshold;

        TREND_ARROW_VALUES(int id, String symbol, String name) {
            this.myID = id;
            this.arrowSymbol = symbol;
            this.trendName = name;
        }

        TREND_ARROW_VALUES(int id, String symbol, String name, Double threshold) {
            this.myID = id;
            this.arrowSymbol = symbol;
            this.trendName = name;
            this.threshold = threshold;
        }

        TREND_ARROW_VALUES(int id) {
            this(id,null, null, null);
        }

        public String Symbol() {
            if (arrowSymbol == null) {
                return "\u2194\uFE0E";
            } else {
                return arrowSymbol + "\uFE0E";
            }
        }

        public String trendName() {
            return this.trendName;
        }

        public String friendlyTrendName() {
            if (trendName == null) {
                return name().replace("_", " ");
            } else {
                return trendName;
            }
        }

        public int getID() {
            return myID;
        }

        public static TREND_ARROW_VALUES getTrend(double value) {
            TREND_ARROW_VALUES finalTrend = NONE;
            for (TREND_ARROW_VALUES trend : values()) {
                if (trend.threshold == null)
                    continue;

                if (value > trend.threshold)
                    return finalTrend;
                else
                    finalTrend = trend;
            }
            return finalTrend;
        }

        public static double getSlope(String value) {
            for (TREND_ARROW_VALUES trend : values())
                if (trend.trendName.equalsIgnoreCase(value))
                    return trend.threshold;
            throw new IllegalArgumentException();
        }

        public static TREND_ARROW_VALUES getEnum(int id) {
            for (TREND_ARROW_VALUES t : values()) {
                if (t.myID == id)
                    return t;
            }
            return OUT_OF_RANGE;
        }

        public static TREND_ARROW_VALUES getEnum(String value) {
            try {
                int val = Integer.parseInt(value);
                return getEnum(val);
            } catch (NumberFormatException e) {
                for (TREND_ARROW_VALUES trend : values())
                    if (trend.friendlyTrendName().equalsIgnoreCase(value) || trend.name().equalsIgnoreCase(value))
                        return trend;
            }
            return OUT_OF_RANGE;
        }

    }

    public enum SPECIALBGVALUES_MGDL {
        NONE("??0", 0),
        SENSORNOTACTIVE("?SN", 1),
        MINIMALLYEGVAB("??2", 2),
        NOANTENNA("?NA", 3),
        SENSOROUTOFCAL("?NC", 5),
        COUNTSAB("?CD", 6),
        ABSOLUTEAB("?AD", 9),
        POWERAB("???", 10),
        RFBADSTATUS("?RF", 12);


        private String name;
        private int val;
        private SPECIALBGVALUES_MGDL(String s, int i){
            name=s;
            val=i;
        }

        public int getValue(){
            return val;
        }

        public String toString(){
            return name;
        }

        public static SPECIALBGVALUES_MGDL getEGVSpecialValue(int val){
            for (SPECIALBGVALUES_MGDL e: values()){
                if (e.getValue()==val)
                    return e;
            }
            return null;
        }

        public static boolean isSpecialValue(int val){
            for (SPECIALBGVALUES_MGDL e: values()){
                if (e.getValue()==val)
                    return true;
            }
            return false;
        }

    }

    public enum InsertionState {
        NONE,
        REMOVED,
        EXPIRED,
        RESIDUAL_DEVIATION,
        COUNTS_DEVIATION,
        SECOND_SESSION,
        OFF_TIME_LOSS,
        STARTED,
        BAD_TRANSMITTER,
        MANUFACTURING_MODE,
        MAX_VALUE
    }

    public enum NOISE {
        NOISE_NONE(0),
        CLEAN(1),
        LIGHT(2),
        MEDIUM(3),
        HEAVY(4),
        NOT_COMPUTED(5),
        MAX(6);

        private final int value;

        private NOISE(int value) {
            this.value = value;
        }

    }

}
