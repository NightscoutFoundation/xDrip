package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.google.gson.annotations.Expose;

/**
 * jamorham
 * <p>
 * Class to model the parameters required in the authentication json
 */

public class ShareLoginBody {

    @Expose
    public String applicationId;

    @Expose
    public String accountId;

    @Expose
    public String password;

    ShareLoginBody(final String password, final String accountId) {
        this.applicationId = ShareConstants.APPLICATION_ID;
        this.accountId = accountId;
        this.password = password;
    }
}
