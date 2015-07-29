package com.eveningoutpost.dexdrip.ShareModels;

import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.android.AndroidLog;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;

/**
 * Created by stephenblack on 7/17/15.
 */

@Table(name = "FollowerInvites", id = BaseColumns._ID)
public class FollowerInvite extends Model {
    private String sessionId;
    private String contactId;

    @Column(name = "email")
    public String email;

    @Column(name = "follower_name")
    public String followerName;

    @Column(name = "display_name")
    public String displayName;

    @Column(name = "invited")
    public boolean invited;

    public static FollowerInvite nextFollowerInvite() {
        return new Select().from(FollowerInvite.class)
                .where("invited = ?", false)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public void startSendingInvite(String sessionId) {
        this.sessionId = sessionId;
        new ShareRest().emptyBodyInterface().doesContactExist(queryCheckContactExists(), new Callback() {
            @Override
            public void success(Object o, Response response) {
                if (new String(((TypedByteArray) response.getBody()).getBytes()).toLowerCase().contains("true")) {
                    Log.d("ShareFollower", "contact already exists");
                    delete();
                } else {
                    createContact();
                }
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e("RETROFIT ERROR: ", "Trouble Checking if contact exists", retrofitError);
            }
        });
    }

    private void createContact() {
        new ShareRest().emptyBodyInterface().createContact(queryCreateContact(), new Callback() {
            @Override
            public void success(Object o, Response response) {
                Log.d("ShareFollower", "contact created!");
                contactId = new String(((TypedByteArray) response.getBody()).getBytes()).replace("\"", "");
                sendInviteToContact();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e("RETROFIT ERROR", "Creating new Contact", retrofitError);
            }
        });
    }

    private void sendInviteToContact() {
        jsonBodyInterface().createInvitationForContact(invitationPayload(), querySendInvite(), new Callback() {
            @Override
            public void success(Object o, Response response) {
                Log.d("ShareFollower", "contact Invite sent!");
                delete();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e("RETROFIT ERROR", "Sending invite", retrofitError);
            }
        });
    }

    private InvitationPayload invitationPayload() {
        return new InvitationPayload(displayName);
    }

    public Map<String, String> queryCheckContactExists() {
        Map<String, String> map = new HashMap<>();
        map.put("sessionID", sessionId);
        map.put("contactName", followerName);
        return map;
    }

    public Map<String, String> queryCreateContact() {
        Map<String, String> map = new HashMap<>();
        map.put("sessionID", sessionId);
        map.put("contactName", followerName);
        map.put("emailAddress", email);
        return map;
    }

    public Map<String, String> querySendInvite() {
        Map<String, String> map = new HashMap<>();
        map.put("sessionID", sessionId);
        map.put("contactId", contactId);
        return map;
    }


    private RestAdapter.Builder authoirizeAdapterBuilder() {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder();
        adapterBuilder
                .setClient(new ShareRest().getOkClient())
                .setLogLevel(RestAdapter.LogLevel.FULL).setLog(new AndroidLog("FollowerInvite"))
                .setEndpoint("https://share1.dexcom.com/ShareWebServices/Services")
                .setRequestInterceptor(ShareRest.authorizationRequestInterceptor)
                .setConverter(new GsonConverter(new GsonBuilder().create()));
        return adapterBuilder;
    }

    public DexcomShareInterface jsonBodyInterface() {
        RestAdapter adapter = authoirizeAdapterBuilder().build();
        DexcomShareInterface dexcomShareInterface =
                adapter.create(DexcomShareInterface.class);
        return dexcomShareInterface;
    }
}
