package com.eveningoutpost.dexdrip.tidepool;

import com.eveningoutpost.dexdrip.models.JoH;

import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * jamorham
 *
 * message base
 */

public abstract class BaseMessage {

    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }

    public RequestBody getBody() {
        return RequestBody.create(MediaType.parse("application/json"), this.toS());
    }

}
