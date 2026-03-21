package com.eveningoutpost.dexdrip.cgm.nsfollow;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utils.framework.RetrofitService.BaseCallback;

import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Response;

// jamorham

/**
 * Retrofit callback for Nightscout API calls.
 * When a {@code populator} is provided, it stores the response body instead of
 * {@link Session#populate(Object)}, enabling typed population for v3 responses.
 */
public class NightscoutCallback<T> extends BaseCallback<T> {

    private final Session session;
    private final Consumer<T> populator;

    public NightscoutCallback(final String name, final Session session, final Runnable onSuccess) {
        this(name, session, null, onSuccess);
    }

    public NightscoutCallback(final String name, final Session session, final Consumer<T> populator, final Runnable onSuccess) {
        super(name);
        this.session = session;
        this.populator = populator;
        this.setOnSuccess(onSuccess);
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        if (response.isSuccessful() && response.body() != null) {
            if (populator != null) {
                try {
                    populator.accept(response.body());
                } catch (Exception e) {
                    UserError.Log.e("NightscoutCallback", name + " populator error: " + e);
                }
            } else {
                session.populate(response.body());
            }
        }
        super.onResponse(call, response);
    }
}
