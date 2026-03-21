package com.eveningoutpost.dexdrip.cgm.nsfollow.messages;

import com.google.gson.annotations.Expose;

/**
 * Response from Nightscout /api/v2/authorization/request/{accessToken}.
 *
 * @author Asbjørn Aarrestad
 */
public class AuthResponse {

    @Expose
    public String token;

    @Expose
    public String sub;

    /** Expiry as Unix timestamp in seconds. */
    @Expose
    public long exp;
}
