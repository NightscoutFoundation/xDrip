package com.eveningoutpost.dexdrip.watch.miband.message;

public class DeviceEvent {
    public static final byte FELL_ASLEEP = 0x01;
    public static final byte WOKE_UP = 0x02;
    public static final byte STEPSGOAL_REACHED = 0x03;
    public static final byte BUTTON_PRESSED = 0x04;
    public static final byte START_NONWEAR = 0x06;
    public static final byte CALL_REJECT = 0x07;
    public static final byte FIND_PHONE_START = 0x08;
    public static final byte CALL_IGNORE = 0x09;
    public static final byte ALARM_TOGGLED = 0x0a;
    public static final byte BUTTON_PRESSED_LONG = 0x0b;
    public static final byte TICK_30MIN = 0x0e; // unsure
    public static final byte FIND_PHONE_STOP = 0x0f;
    public static final byte MTU_REQUEST = 0x16;
    public static final byte MUSIC_CONTROL = (byte) 0xfe;
}