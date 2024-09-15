package com.eveningoutpost.dexdrip.cloud.jamcm;

/**
 * JamOrHam
 */
public class Upstream {

    private static final String serverInstance = "jamcm3749021";
    private static final String legacyServerInstance = "legacypush188263";
    private static final String serverDomain = "bluejay.website";
    private static final String serverAddress = serverInstance + "." + serverDomain;
    private static final String legacyServerAddress = legacyServerInstance + "." + serverDomain;

    public static String getBestServerAddress() {
        return serverAddress;
    }

    public static String getLegacyServerAddress() {
        return legacyServerAddress;
    }
}
