package com.eveningoutpost.dexdrip.ShareModels;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.ShareModels.Models.ShareUploadPayload;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.Observable;
import rx.functions.Action1;

/**
 * Created by stephenblack on 7/31/15.
 */
public class BgUploader {
    public static String TAG = BgUploader.class.getSimpleName();
    private String sessionId;

    private String username;
    private String password;
    private String serialNumber;
    private Context context;
    private SharedPreferences prefs;
    private ShareUploadableBg bg;

    private boolean uploadRetried = false;
    private Action1<Boolean> successListener;

    public BgUploader(String username, String password, String serialNumber, Context context, Action1<Boolean> successListener) {
        this.username = username;
        this.password = password;
        this.serialNumber = serialNumber;
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.successListener = successListener;
    }

    public BgUploader(String username, String password, String serialNumber, Context context) {
        this.username = username;
        this.password = password;
        this.serialNumber = serialNumber;
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void upload(ShareUploadableBg bg) {
        this.uploadRetried = false;
        this.bg = bg;
        tryUpload();
    }

    private void tryUpload() {
        Action1<Boolean> authListener = new Action1<Boolean>() {
            @Override
            public void call(Boolean authed) {
                if (authed) {
                    sessionId = prefs.getString("dexcom_share_session_id", "");
                    upload();
                } else {
                    if(successListener != null) {
                        Observable.just(false).subscribe(successListener);
                    }
                }
            }
        };
        new ShareAuthentication(username, password, serialNumber, context, authListener).authenticate();
    }

    private void upload() {
       ShareRest.jsonBodyInterface().uploadBGRecords(ShareRest.querySessionMap(sessionId), new ShareUploadPayload(serialNumber, bg), new Callback<Response>() {
            @Override
            public void success(Response o, Response response) {
                Log.d(TAG, "Success!! Uploaded!!");
                if(successListener != null) {
                    Observable.just(true).subscribe(successListener);
                }
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e(TAG, "Upload failure", retrofitError);
                if (ShareAuthentication.shouldReAuth(context, retrofitError, uploadRetried)) {
                    uploadRetried = true;
                    tryUpload();
                } else {
                    if(successListener != null) {
                        Observable.just(false).subscribe(successListener);
                    }
                }
            }
        });
    }
}
