package com.eveningoutpost.dexdrip.ShareModels;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import com.eveningoutpost.dexdrip.ShareModels.Models.ExistingFollower;
import com.eveningoutpost.dexdrip.ShareModels.Models.InvitationPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import rx.Observable;
import rx.functions.Action1;

/**
 * Created by stephenblack on 8/10/15.
 */
public class FollowerManager {
    public static String TAG = FollowerManager.class.getSimpleName();
    private String sessionId;

    private String username;
    private String password;
    private String serialNumber;
    private Context context;
    private SharedPreferences prefs;

    private Action1<List <ExistingFollower>> followerListener;

    private Action1<Boolean> deletedListener;

    private boolean getFollowersRetried = false;
    private boolean deleteFollowerRetried = false;

    private String contactId;
    private String email;
    private String followerName;
    private String displayName;
    private Action1<Boolean> invitedListener;
    private boolean inviteRetried = false;


    public FollowerManager(String username, String password, String serialNumber, Context context) {
        this.username = username;
        this.password = password;
        this.serialNumber = serialNumber;
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void getFollowers(Action1<List <ExistingFollower>> followerListener) {
        this.getFollowersRetried = false;
        this.followerListener = followerListener;
        tryGetFollowers();
    }

    public void deleteFollower(Action1<Boolean> deletedListener, String contactId) {
        this.deleteFollowerRetried = false;
        this.deletedListener = deletedListener;
        this.contactId = contactId;
        tryDelete();
    }

    public void inviteFollower(String email, String followerName, String displayName, Action1<Boolean> invitedListener) {
        this.email = email;
        this.followerName = followerName;
        this.displayName = displayName;
        this.invitedListener = invitedListener;
        inviteRetried = false;
        tryInviteFollower();
    }



    //GET FOLLOWERS RELATED
    private void tryGetFollowers() {
        Action1<Boolean> followerAuthListener = new Action1<Boolean>() {
            @Override
            public void call(Boolean authed) {
                if (authed) {
                    sessionId = prefs.getString("dexcom_share_session_id", "");
                    getListOfFollowers();
                } else {
                    Observable.just(new ArrayList<ExistingFollower>()).subscribe(followerListener);
                }
            }
        };
        new ShareAuthentication(username, password, serialNumber, context, followerAuthListener).authenticate();
    }

    private void getListOfFollowers() {
        ShareRest.emptyBodyInterface().getContacts(ShareRest.querySessionMap(sessionId), new Callback<List<ExistingFollower>>() {
            @Override
            public void success(List<ExistingFollower> followers, Response response) {
                Observable.just(followers).subscribe(followerListener);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e(TAG, "List Followers failure", retrofitError);
                if (ShareAuthentication.shouldReAuth(context, retrofitError, getFollowersRetried)) {
                    getFollowersRetried = true;
                    tryGetFollowers();
                } else {
                    Observable.just(new ArrayList<ExistingFollower>()).subscribe(followerListener);
                }
            }
        });
    }




    //Delete Follower Related
    private void tryDelete() {
        Action1<Boolean> authListener = new Action1<Boolean>() {
            @Override
            public void call(Boolean authed) {
                if (authed) {
                    sessionId = prefs.getString("dexcom_share_session_id", "");
                    deleteFollower();
                } else {
                    Observable.just(false).subscribe(deletedListener);
                }
            }
        };
        new ShareAuthentication(username, password, serialNumber, context, authListener).authenticate();
    }

    private void deleteFollower() {
        ShareRest.emptyBodyInterface().deleteContact(deleteFollowerPayload(sessionId, contactId), new Callback<Response>() {
            @Override
            public void success(Response o, Response response) {
                Log.d("ShareFollower", "contact Invite sent!");
                Observable.just(true).subscribe(deletedListener);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e(TAG, "Deleting Follower Failure", retrofitError);
                if (ShareAuthentication.shouldReAuth(context, retrofitError, deleteFollowerRetried)) {
                    deleteFollowerRetried = true;
                    tryDelete();
                } else {
                    Observable.just(false).subscribe(deletedListener);
                }
            }
        });
    }

    private Map<String, String> deleteFollowerPayload(String sessionId, String contactId) {
        Map map = new HashMap<String, String>();
        map.put("sessionID", sessionId);
        map.put("contactId", contactId);
        return map;
    }



   //Invite follower Related
    private void tryInviteFollower() {
        Action1<Boolean> authListener = new Action1<Boolean>() {
            @Override
            public void call(Boolean authed) {
                if (authed) {
                    sessionId = prefs.getString("dexcom_share_session_id", "");
                    invite();
                } else {
                    Observable.just(false).subscribe(invitedListener);
                }
            }
        };
        new ShareAuthentication(username, password, serialNumber, context, authListener).authenticate();
    }

    private void invite() {
        ShareRest.emptyBodyInterface().doesContactExist(queryCheckContactExists(), new Callback<Response>() {
            @Override
            public void success(Response o, Response response) {
                if (new String(((TypedByteArray) response.getBody()).getBytes()).toLowerCase().contains("true")) {
                    Log.e("ShareFollower", "contact already exists");
                    Observable.just(true).subscribe(invitedListener);
                } else {
                    createContact();
                }
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e(TAG, "Trouble Checking if contact exists", retrofitError);
                if (ShareAuthentication.shouldReAuth(context, retrofitError, inviteRetried)) {
                    inviteRetried = true;
                    tryInviteFollower();
                } else {
                    Observable.just(false).subscribe(invitedListener);
                }
            }
        });
    }

    private void createContact() {
        ShareRest.emptyBodyInterface().createContact(queryCreateContact(), new Callback<Response>() {
            @Override
            public void success(Response o, Response response) {
                Log.d("ShareFollower", "contact created!");
                contactId = new String(((TypedByteArray) response.getBody()).getBytes()).replace("\"", "");
                sendInviteToContact();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e(TAG, "Creating new Contact", retrofitError);
                if (ShareAuthentication.shouldReAuth(context, retrofitError, inviteRetried)) {
                    inviteRetried = true;
                    tryInviteFollower();
                } else {
                    Observable.just(false).subscribe(invitedListener);
                }
            }
        });
    }

    private void sendInviteToContact() {
        ShareRest.jsonBodyInterface().createInvitationForContact(new InvitationPayload(displayName), querySendInvite(), new Callback<Response>() {
            @Override
            public void success(Response o, Response response) {
                Log.d("ShareFollower", "contact Invite sent!");
                Observable.just(true).subscribe(invitedListener);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e(TAG, "Sending invite", retrofitError);
                if (ShareAuthentication.shouldReAuth(context, retrofitError, inviteRetried)) {
                    inviteRetried = true;
                    tryInviteFollower();
                } else {
                    Observable.just(false).subscribe(invitedListener);
                }
            }
        });
    }

    private Map<String, String> queryCheckContactExists() {
        Map<String, String> map = new HashMap<>();
        map.put("sessionID", sessionId);
        map.put("contactName", followerName);
        return map;
    }

    private Map<String, String> queryCreateContact() {
        Map<String, String> map = new HashMap<>();
        map.put("sessionID", sessionId);
        map.put("contactName", followerName);
        map.put("emailAddress", email);
        return map;
    }

    private Map<String, String> querySendInvite() {
        Map<String, String> map = new HashMap<>();
        map.put("sessionID", sessionId);
        map.put("contactId", contactId);
        return map;
    }
}
