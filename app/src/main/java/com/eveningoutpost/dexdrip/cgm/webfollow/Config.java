package com.eveningoutpost.dexdrip.cgm.webfollow;

import lombok.Builder;
import okhttp3.RequestBody;

/**
 * JamOrHam
 */

@Builder
public class Config implements Exposed {

    public String url;
    public String agent;
    public String version;
    public String product;
    public String authorization;
    public String query;
    public String contentType;
    public RequestBody body;

}
