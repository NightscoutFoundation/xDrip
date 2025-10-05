package com.eveningoutpost.dexdrip.cgm.nsfollow;

import com.eveningoutpost.dexdrip.utils.framework.RetrofitService.BaseCallback;

import retrofit2.Call;
import retrofit2.Response;

// jamorham

public class NightscoutCallback<T> extends BaseCallback<T> {

    private final Session session;


    public NightscoutCallback(final String name, final Session session, final Runnable onSuccess) {
        super(name);
        this.session = session;
        this.setOnSuccess(onSuccess);
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        if (response.isSuccessful() && response.body() != null) {
            session.populate(response.body());
        }
        super.onResponse(call, response);
    }
}
