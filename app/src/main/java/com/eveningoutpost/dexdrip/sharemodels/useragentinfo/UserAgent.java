package com.eveningoutpost.dexdrip.sharemodels.useragentinfo;

import com.google.gson.annotations.Expose;

/**
 * Created by Emma Black on 6/29/15.
 */
public class UserAgent {
    @Expose
    public String sessionId;

    @Expose
    public RuntimeInfo runtimeInfo;

    public UserAgent(String aSessionId) {
        this.runtimeInfo = new RuntimeInfo();
        this.sessionId = aSessionId;
    }
}
