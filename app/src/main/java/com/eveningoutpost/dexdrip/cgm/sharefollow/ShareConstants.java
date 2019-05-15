package com.eveningoutpost.dexdrip.cgm.sharefollow;

public class ShareConstants {

    public static final String US_SHARE_BASE_URL = "https://share2.dexcom.com/ShareWebServices/Services/";
    public static final String NON_US_SHARE_BASE_URL = "https://shareous1.dexcom.com/ShareWebServices/Services/";

    public static final String USER_AGENT = "User-Agent: Dexcom Share/3.0.2.11 CFNetwork/711.2.23 Darwin/14.0.0";

    static final long SESSION_ID_VALIDITY_TIME = 60 * 60 * 8 * 1000;

    static final int MAX_RECORDS_TO_ASK_FOR = 36;
}
