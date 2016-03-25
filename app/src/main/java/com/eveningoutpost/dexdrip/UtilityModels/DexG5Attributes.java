package com.eveningoutpost.dexdrip.UtilityModels;

/**
 * Created by jcostik1 on 3/15/16.
 */

import java.util.UUID;

public class DexG5Attributes {

    //G5 Service Strings
    public static final UUID Advertisement = UUID.fromString("0000FEBC-0000-1000-8000-00805F9B34FB");
    public static final UUID CGMService = UUID.fromString("F8083532-849E-531C-C594-30F1F86A4EA5");
    public static final UUID ServiceB = UUID.fromString("F8084532-849E-531C-C594-30F1F86A4EA5");

    //G5 Characteristics Strings
    public static final UUID Communication = UUID.fromString("F8083533-849E-531C-C594-30F1F86A4EA5");
    public static final UUID Control = UUID.fromString("F8083534-849E-531C-C594-30F1F86A4EA5");
    public static final UUID Authentication = UUID.fromString("F8083535-849E-531C-C594-30F1F86A4EA5");
    public static final UUID ProbablyBackfill = UUID.fromString("F8083536-849E-531C-C594-30F1F86A4EA5");

    //ServiceB Characteristics
    public static final UUID CharacteristicE = UUID.fromString("F8084533-849E-531C-C594-30F1F86A4EA5");
    public static final UUID CharacteristicF = UUID.fromString("F8084534-849E-531C-C594-30F1F86A4EA5");
}

//G5 Details thanks to Nathan Racklyeft
