package com.eveningoutpost.dexdrip.cgm.nsfollow.messages;

import com.google.gson.annotations.Expose;

/**
 * Wrapper for Nightscout API v3 responses: {@code {"status":200,"result":[...]}}.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutV3Response<T> {

    @Expose
    public int status;

    @Expose
    public T result;

}
