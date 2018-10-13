package com.eveningoutpost.dexdrip.tidepool;

import com.eveningoutpost.dexdrip.Models.UserError;

import lombok.AllArgsConstructor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// jamorham

// Callback template to reduce boiler plate

@AllArgsConstructor
class TidePoolCallback<T> implements Callback<T> {

    final Session session;
    final String name;
    final Runnable onSuccess;

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        if (response.isSuccessful() && response.body() != null) {
            UserError.Log.d(TidePoolUploader.TAG, name + " success");
            session.populateBody(response.body());
            session.populateHeaders(response.headers());
            if (onSuccess != null) {
                onSuccess.run();
            }
        } else {
            UserError.Log.e(TidePoolUploader.TAG, name + " was not successful: " + response.code() + " " + response.message());
        }
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        UserError.Log.e(TidePoolUploader.TAG, name + " Failed: " + t);
    }
}
