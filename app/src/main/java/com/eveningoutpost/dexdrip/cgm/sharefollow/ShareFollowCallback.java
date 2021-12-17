package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.eveningoutpost.dexdrip.cgm.nsfollow.BaseCallback;

import retrofit2.Call;
import retrofit2.Response;

/**
 * jamorham
 *
 * Handle specifics of callback for ShareFollow
 *
 * Push different response types in to session
 *
 */

public class ShareFollowCallback<T> extends BaseCallback<T> {

    private final Session session;

    ShareFollowCallback(final String name, final Session session, final Runnable onSuccess) {
        super(name);
        this.session = session;
        this.setOnSuccess(onSuccess);
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        session.setLastResponseCode(response.code());
        if (response.isSuccessful() && response.body() != null) {
            if (this.name.equalsIgnoreCase("auth")) {
                session.extractAccountId(response.body());
            } else {
                session.populate(response.body());
            }
        } else {
            session.errorHandler(response.errorBody());
        }
        super.onResponse(call, response);
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        session.setLastResponseCode(0);
        super.onFailure(call, t);
    }
}
