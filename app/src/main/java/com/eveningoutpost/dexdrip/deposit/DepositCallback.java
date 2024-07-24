package com.eveningoutpost.dexdrip.deposit;


import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.eveningoutpost.dexdrip.models.UserError;

import lombok.RequiredArgsConstructor;
import lombok.var;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// jamorham

// Callback template to reduce boiler plate

@RequiredArgsConstructor
@RequiresApi(api = Build.VERSION_CODES.N)
class DepositCallback<T> implements Callback<T> {

    private static final String TAG = DepositCallback.class.getSimpleName();

    private final String name;
    private final F onSuccess;

    private F onFailure;
    private F onStatus;

    DepositCallback<T> setOnFailure(final F runnable) {
        this.onFailure = runnable;
        return this;
    }

    DepositCallback<T> setOnStatus(final F runnable) {
        this.onStatus = runnable;
        return this;
    }


    @Override
    public void onResponse(@NonNull Call<T> call, Response<T> response) {
        if (response.isSuccessful() && response.body() != null) {
            String msg = "Error";
            if (response.body() instanceof DepositReply1) {
                msg = ((DepositReply1) response.body()).result;
            }
            status("Result: " + msg);
            if (onSuccess != null) {
                onSuccess.apply(msg);

            }
        } else {
            final String msg = name + " was not successful: " + response.code() + " " + response.message();
            status(msg);
            if (onFailure != null) {
                onFailure.apply(msg);
            }
        }
    }

    @Override
    public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
        final String msg = "Failed: " + t;
        UserError.Log.e(TAG, msg);
        status(msg);
        if (onFailure != null) {
            onFailure.apply(msg);
        }
    }

    private void status(final String status) {
        UserError.Log.d(TAG, "Status: " + status);
        if (onStatus != null) {
            onStatus.apply(status);
        }
    }

}
