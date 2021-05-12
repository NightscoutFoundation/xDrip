package com.eveningoutpost.dexdrip.tidepool;

// jamorham

// Manages the session data

import java.util.List;

import okhttp3.Headers;

public class Session {

    private final String SESSION_TOKEN_HEADER;
    final TidepoolUploader.Tidepool service = TidepoolUploader.getRetrofitInstance().create(TidepoolUploader.Tidepool.class);
    final String authHeader;

    String token;
    MAuthReply authReply;
    MDatasetReply datasetReply;
    long start;
    long end;
    volatile int iterations;


    Session(String authHeader, String session_token_header) {
        this.authHeader = authHeader;
        this.SESSION_TOKEN_HEADER = session_token_header;
    }

    void populateHeaders(final Headers headers) {
        if (this.token == null) {
            this.token = headers.get(SESSION_TOKEN_HEADER);
        }
    }

    void populateBody(final Object obj) {
        if (obj == null) return;
        if (obj instanceof MAuthReply) {
            authReply = (MAuthReply) obj;
        } else if (obj instanceof List) {
            List list = (List)obj;
            if (list.size() > 0 && list.get(0) instanceof MDatasetReply) {
                datasetReply = (MDatasetReply) list.get(0);
            }
        } else if (obj instanceof MDatasetReply) {
            datasetReply = (MDatasetReply) obj;
        }
    }

    boolean exceededIterations() {
        return iterations > 50;
    }

}
