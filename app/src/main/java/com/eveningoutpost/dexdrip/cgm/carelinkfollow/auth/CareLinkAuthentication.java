package com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth;

import okhttp3.Headers;

public class CareLinkAuthentication {

    public CareLinkAuthType authType;
    private Headers.Builder builder;

    public CareLinkAuthentication(Headers headers, CareLinkAuthType authType) {
        this.builder = new Headers.Builder();
        this.builder.addAll(headers);
        this.authType = authType;
    }

    public Headers getHeaders() {
        return builder.build();
    }

}
