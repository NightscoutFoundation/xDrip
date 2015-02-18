package com.eveningoutpost.dexdrip.UtilityModels;

import java.math.BigInteger;
import java.util.UUID;

/**
 * Created by stephenblack on 2/4/15.
 */
public class DexShareAttributes {

    // These are stupid looking because without updated java it fails to properly parse newer UUIDS
//    public static UUID CradleService() { return uuid_for(CradleServiceString); }
//    public static UUID AuthenticationCode() { return uuid_for(AuthenticationCodeString); }
//    public static UUID ShareMessageReceiver() { return uuid_for(ShareMessageReceiverString); }
//    public static UUID ShareMessageResponse() { return uuid_for(ShareMessageResponseString); }
//    public static UUID Command() { return uuid_for(CommandString); }
//    public static UUID Response() { return uuid_for(ResponseString); }
//    public static UUID HeartBeat() { return uuid_for(HeartBeatString); }

    //Share Service String
    public static final UUID CradleService= UUID.fromString("F0ABA0B1-EBFA-F96F-28DA-076C35A521DB");

    //Share Characteristic Strings
    public static final UUID AuthenticationCode = UUID.fromString("F0ABACAC-EBFA-F96F-28DA-076C35A521DB");
    public static final UUID ShareMessageReceiver= UUID.fromString("F0ABB20A-EBFA-F96F-28DA-076C35A521DB"); // Max 20 Bytes - Writable
    public static final UUID ShareMessageResponse= UUID.fromString("F0ABB20B-EBFA-F96F-28DA-076C35A521DB"); // Max 20 Bytes
    public static final UUID Command= UUID.fromString("F0ABB0CC-EBFA-F96F-28DA-076C35A521DB");
    public static final UUID Response= UUID.fromString("F0ABB0CD-EBFA-F96F-28DA-076C35A521DB"); // Writable?
    public static final UUID HeartBeat= UUID.fromString("F0AB2B18-EBFA-F96F-28DA-076C35A521DB");


    //Device Info
    public static final UUID DeviceService= UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID PowerLevel= UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");


//

    // Message Structure - 20 Bytes Total
//    Uchar messageNumber
//    Uchar totalMessages
//    Uchar messagegBytes[18]

//    Write a message to the ShareMessageReceiver using the message structure defined above

//    public static UUID uuid_for(String string) {
//        String s2 = string.replace("-", "");
//        UUID uuid = new UUID(new BigInteger(s2.substring(0, 16), 16).longValue(), new BigInteger(s2.substring(16), 16).longValue());
//        return uuid;
//    }
}
