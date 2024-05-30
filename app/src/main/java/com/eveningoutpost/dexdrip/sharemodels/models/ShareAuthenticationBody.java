package com.eveningoutpost.dexdrip.sharemodels.models;

import com.google.gson.annotations.Expose;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Emma Black on 3/16/15.
 */
public class ShareAuthenticationBody {
    @Expose
    public String password;

    @Expose
    public String applicationId;

    @Expose
    public String accountName;

    public ShareAuthenticationBody(String aPassword, String aAccountName) {
        this.password = aPassword;
        this.accountName = aAccountName;
        this.applicationId = "d89443d2-327c-4a6f-89e5-496bbb0317db";
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new TreeMap<>();
        map.put("password", password);
        map.put("applicationId", applicationId);
        map.put("accountName", accountName);
        return map;
    }
}
