package com.eveningoutpost.dexdrip.sharemodels;

import android.content.Context;

import com.eveningoutpost.dexdrip.sharemodels.models.ShareUploadPayload;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Emma Black on 7/31/15.
 */
public class BgUploader {
    public static String TAG = BgUploader.class.getSimpleName();

    private ShareRest shareRest;

    public BgUploader(Context context) {
        this.shareRest = new ShareRest(context, null);
    }

    public void upload(ShareUploadPayload bg) {
        shareRest.uploadBGRecords(bg, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // This should probably be pulled up into BgSendQueue or NightscoutUploader
                // where errors can be handled properly.
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // TODO add error handling in a refactoring pass
            }
        });
    }
}
