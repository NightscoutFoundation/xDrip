package com.eveningoutpost.dexdrip.watch.miband;
//00000006-0000-3512-2118-0009af100700
//
//        f			= ?
//        30		= 48%
//        00		= 00 = STATUS_NORMAL, 01 = STATUS_CHARGING
//        e0 07		= 2016
//        0b		= 11
//        1a		= 26
//        12		= 18
//        23		= 35
//        2c		= 44
//        04		= 4 // num charges??
//
//        e0 07		= 2016 // last charge time
//        0b		= 11
//        1a		= 26
//        17		= 23
//        2b		= 43
//        3b		= 59
//        04		= 4   // num charges??
//        64		= 100 // how much was charged
import java.util.GregorianCalendar;

public class BatteryInfo {
    enum BatteryState {
        UNKNOWN,
        BATTERY_NORMAL,
        BATTERY_LOW,
        BATTERY_CHARGING,
        BATTERY_CHARGING_FULL,
        BATTERY_NOT_CHARGING_FULL,
        NO_BATTERY
    }

    private byte[] mData = new byte[0];
    public static final byte DEVICE_BATTERY_NORMAL = 0;
    public static final byte DEVICE_BATTERY_CHARGING = 1;
    //    public static final byte DEVICE_BATTERY_LOW = 1;
    //    public static final byte DEVICE_BATTERY_CHARGING_FULL = 3;
    //    public static final byte DEVICE_BATTERY_CHARGE_OFF = 4;

    public BatteryInfo( ) {
    }

    public BatteryInfo(byte[] data) {
        mData = new byte[data.length];
        System.arraycopy(data, 0, mData, 0, data.length);
    }

    public int getLevelInPercent() {
        if (mData.length >= 2) {
            return mData[1];
        }
        return 1000; // actually unknown
    }

    public BatteryState getState() {
        if (mData.length >= 3) {
            int value = mData[2];
            switch (value) {
                case DEVICE_BATTERY_NORMAL:
                    return BatteryState.BATTERY_NORMAL;
                case DEVICE_BATTERY_CHARGING:
                    return BatteryState.BATTERY_CHARGING;
//                case DEVICE_BATTERY_CHARGING:
//                    return BatteryState.BATTERY_CHARGING;
//                case DEVICE_BATTERY_CHARGING_FULL:
//                    return BatteryState.BATTERY_CHARGING_FULL;
//                case DEVICE_BATTERY_CHARGE_OFF:
//                    return BatteryState.BATTERY_NOT_CHARGING_FULL;
            }
        }
        return BatteryState.UNKNOWN;
    }

    public int getLastChargeLevelInParcent() {
        if (mData.length >= 20) {
            return mData[19];
        }
        return 1000; // actually unknown
    }

    public GregorianCalendar getLastChargeTime() {
        GregorianCalendar lastCharge = new GregorianCalendar();

        if (mData.length >= 18) {
            lastCharge = new GregorianCalendar(
                    mData[10] + 2000,
                    mData[11],
                    mData[12],
                    mData[13],
                    mData[14],
                    mData[15]);
        }

        return lastCharge;
    }

    public int getNumCharges() {
//        if (mData.length >= 10) {
//            return ((0xff & mData[7]) | ((0xff & mData[8]) << 8));
//
//        }
        return -1;
    }
}
