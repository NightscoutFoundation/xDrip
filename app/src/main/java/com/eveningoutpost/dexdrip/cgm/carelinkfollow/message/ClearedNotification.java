package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.Date;

public class ClearedNotification {

    public String faultId;  // Changed from int to String
    public String version;  // new
    public String referenceGUID;
    public String dateTime;  // Changed from Date and "2024-12-21T15:41:06.000-00:00" to String and "2025-01-16T13:08:12"
    public Date datetime;
    public String type;
    public String triggeredDateTime;
    public Date triggeredDatetime;  // Changed from Date and "2024-12-21T15:41:06.000-00:00" to String and "2025-01-16T13:08:12"
    public AdditionalInfo additionalInfo;  // new
    public int instanceId;
    public String messageId;
    public String pumpDeliverySuspendState;
    public String pnpId;
    public int relativeOffset;


}
