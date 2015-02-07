package com.eveningoutpost.dexdrip.UtilityModels;

import java.util.UUID;

/**
 * Created by stephenblack on 2/4/15.
 */
public class DexShareAttributes {

    //Share Service
    public static UUID CradleService = UUID.fromString("F0ABA0B1–EBFA-F96F-28DA-076C35A521DB");

    //Share Characteristics
    public static UUID AuthenticationCode = UUID.fromString("F0ABACAC–EBFA-F96F-28DA-076C35A521DB");
    public static UUID ShareMessageReceiver = UUID.fromString("F0ABB20A–EBFA-F96F-28DA-076C35A521DB"); // Max 20 Bytes - Writable
    public static UUID ShareMessageResponse = UUID.fromString("F0ABB20B–EBFA-F96F-28DA-076C35A521DB"); // Max 20 Bytes
    public static UUID Command = UUID.fromString("F0ABB0CC–EBFA-F96F-28DA-076C35A521DB");
    public static UUID Response = UUID.fromString("F0ABB0CD–EBFA-F96F-28DA-076C35A521DB"); // Writable?
    public static UUID HeartBeat = UUID.fromString("F0AB2B18–EBFA-F96F-28DA-076C35A521DB");


    // Message Structure - 20 Bytes Total
//    Uchar messageNumber
//    Uchar totalMessages
//    Uchar messagegBytes[18]

//    Write a message to the ShareMessageReceiver using the message structure defined above



}
