package com.newrelic.agent.android;

import static com.eveningoutpost.dexdrip.BuildConfig.buildUUID;

final class NewRelicConfig {
    static final String BUILD_ID = buildUUID;
    static final String MAP_PROVIDER = "r8";
    static final Boolean OBFUSCATED = true;
    static final String VERSION = "5.27.1";

    public static String getBuildId() {
        return BUILD_ID;
    }

}