package com.eveningoutpost.dexdrip.utils.framework;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

/**
 * Gzips outgoing request bodies. By default every eligible request is gzipped; supply a
 * {@link GzipDecider} to gate gzipping per request (e.g. only when the server is known to
 * support it).
 *
 * @author Asbjørn Aarrestad
 */
public class GzipRequestInterceptor implements Interceptor {

    private static final GzipDecider ALWAYS = request -> true;

    private final GzipDecider decider;

    public GzipRequestInterceptor() {
        this(ALWAYS);
    }

    public GzipRequestInterceptor(GzipDecider decider) {
        this.decider = decider;
    }

    @Override
    public okhttp3.Response intercept(Chain chain) throws IOException {
        final Request originalRequest = chain.request();
        if (originalRequest.body() == null
                || originalRequest.header("Content-Encoding") != null
                || !decider.shouldGzip(originalRequest)) {
            return chain.proceed(originalRequest);
        }

        final Request compressedRequest = originalRequest.newBuilder()
                .header("Content-Encoding", "gzip")
                .method(originalRequest.method(), gzip(originalRequest.body()))
                .build();
        return chain.proceed(compressedRequest);
    }

    private RequestBody gzip(final RequestBody body) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return body.contentType();
            }

            @Override
            public long contentLength() {
                return -1; // We don't know the compressed length in advance!
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                body.writeTo(gzipSink);
                gzipSink.close();
            }
        };
    }
}
