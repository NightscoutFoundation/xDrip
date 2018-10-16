package com.eveningoutpost.dexdrip.UtilityModels;

// jamorham

public class Unitized {

    // TODO move static BgGraphBuilder methods here

    public static boolean usingMgDl() {
        return Pref.getString("units", "mgdl").equals("mgdl");
    }

}
