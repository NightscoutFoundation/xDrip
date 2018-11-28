package com.eveningoutpost.dexdrip.tidepool;

import com.google.gson.annotations.Expose;

public class MCloseDatasetRequest extends BaseMessage {
    @Expose
    String dataState = "closed";

}
