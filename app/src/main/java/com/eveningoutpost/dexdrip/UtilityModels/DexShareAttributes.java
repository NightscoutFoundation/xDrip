package com.eveningoutpost.dexdrip.UtilityModels;

import java.math.BigInteger;
import java.util.UUID;

/**
 * Created by stephenblack on 2/4/15.
 */
public class DexShareAttributes {
    //Share Service String
    public static final UUID CradleService= UUID.fromString("F0ABA0B1-EBFA-F96F-28DA-076C35A521DB");

    //Share Characteristic Strings
    public static final UUID AuthenticationCode = UUID.fromString("F0ABACAC-EBFA-F96F-28DA-076C35A521DB");
    public static final UUID ShareMessageReceiver= UUID.fromString("F0ABB20A-EBFA-F96F-28DA-076C35A521DB"); // Max 20 Bytes - Writable
    public static final UUID ShareMessageResponse= UUID.fromString("F0ABB20B-EBFA-F96F-28DA-076C35A521DB"); // Max 20 Bytes
    public static final UUID Command= UUID.fromString("F0ABB0CC-EBFA-F96F-28DA-076C35A521DB");
    public static final UUID Response= UUID.fromString("F0ABB0CD-EBFA-F96F-28DA-076C35A521DB"); // Writable?
    public static final UUID HeartBeat= UUID.fromString("F0AB2B18-EBFA-F96F-28DA-076C35A521DB");

    //Possible new uuids????  60bfxxxx-60b0-4d4f-0000-000160c48d70
    public static final UUID CradleService2= UUID.fromString("60bfA0B1-60b0-4d4f-0000-000160c48d70");
    public static final UUID AuthenticationCode2 = UUID.fromString("60bfACAC-60b0-4d4f-0000-000160c48d70");
    public static final UUID ShareMessageReceiver2= UUID.fromString("60bfB20A-60b0-4d4f-0000-000160c48d70"); // Max 20 Bytes - Writable
    public static final UUID ShareMessageResponse2= UUID.fromString("60bfB20B-60b0-4d4f-0000-000160c48d70"); // Max 20 Bytes
    public static final UUID Command2= UUID.fromString("60bfB0CC-60b0-4d4f-0000-000160c48d70");
    public static final UUID Response2= UUID.fromString("60bfB0CD-60b0-4d4f-0000-000160c48d70"); // Writable?
    public static final UUID HeartBeat2= UUID.fromString("60bf2B18-60b0-4d4f-0000-000160c48d70");

    //Device Info
    public static final UUID DeviceService= UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID PowerLevel= UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");

}
