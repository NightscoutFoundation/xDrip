package com.eveningoutpost.dexdrip.ShareModels;

import com.google.gson.annotations.Expose;

/**
 * Created by stephenblack on 3/16/15.
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
}
