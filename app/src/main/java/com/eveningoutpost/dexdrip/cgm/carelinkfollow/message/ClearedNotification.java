package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util.CareLinkJsonAdapter;
import com.google.gson.annotations.JsonAdapter;

import java.util.Date;

public class ClearedNotification extends Notification {

    public String referenceGUID;

    @JsonAdapter(CareLinkJsonAdapter.class)
    public Date triggeredDateTime;

}
