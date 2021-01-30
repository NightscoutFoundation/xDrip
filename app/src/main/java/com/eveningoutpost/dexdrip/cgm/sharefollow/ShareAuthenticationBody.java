package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.google.gson.annotations.Expose;

/**
 * jamorham
 *
 * Class to model the parameters required in the authentication json
 */

public class ShareAuthenticationBody {

    @Expose
    public String applicationId;

    @Expose
    public String accountName;

    @Expose
    public String password;

    ShareAuthenticationBody(final String password, final String username) {
        this.applicationId = ShareConstants.APPLICATION_ID;
        this.accountName = username;
        this.password = password;
    }
}