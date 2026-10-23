package com.eveningoutpost.dexdrip.utils.framework;

import okhttp3.Request;

/**
 * Decides, per request, whether the {@link GzipRequestInterceptor} should gzip the request body.
 *
 * @author Asbjørn Aarrestad
 */
public interface GzipDecider {
    boolean shouldGzip(Request request);
}
