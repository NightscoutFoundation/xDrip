package com.eveningoutpost.dexdrip;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.sharemodels.models.ExistingFollower;
import com.eveningoutpost.dexdrip.sharemodels.models.InvitationPayload;
import com.eveningoutpost.dexdrip.sharemodels.ShareRest;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.util.List;

import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by Emma Black on 8/11/15.
 */
public class FollowerManagementActivity extends ActivityWithMenu {
    private static final String menu_name = "Follower Management";
    private static final String TAG = Home.class.getName();
    private ListView existingFollowersView;
    private Button addFollowerButton;
    private ShareRest shareRest;
    private Callback<List<ExistingFollower>> existingFollowerListener;
    private List<ExistingFollower> existingFollowerList;
    private FollowerListAdapter followerListAdapter;

    @Override
    public String getMenuName() {
        return menu_name;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follower_management);
        existingFollowersView = (ListView) findViewById(R.id.followerList);
        addFollowerButton = (Button) findViewById(R.id.inviteFollower);
        shareRest = new ShareRest(this, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        populateFollowerList();
        setInviteListener();
    }

    private void populateFollowerList() {
        existingFollowerListener = new Callback<List<ExistingFollower>>() {
            @Override
            public void onResponse(Response<List<ExistingFollower>> response, Retrofit retrofit) {
                List<ExistingFollower> existingFollowers = response.body();
                if (followerListAdapter != null) {
                    existingFollowerList.clear();
                    if (existingFollowers != null && existingFollowers.size() > 0)
                        existingFollowerList.addAll(existingFollowers);
                    followerListAdapter.notifyDataSetChanged();
                } else {
                    if (existingFollowers != null && existingFollowers.size() > 0) {
                        existingFollowerList = existingFollowers;
                        followerListAdapter = new FollowerListAdapter(getApplicationContext(), shareRest, existingFollowerList);
                        existingFollowersView.setAdapter(followerListAdapter);
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                // If it fails, don't show followers.
            }
        };

        shareRest.getContacts(existingFollowerListener);

    }

    private void setInviteListener() {
        addFollowerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(FollowerManagementActivity.this);
                dialog.setContentView(R.layout.dialog_invite_follower);
                dialog.setTitle("Invite a Follower");
                Button saveButton = (Button) dialog.findViewById(R.id.saveButton);
                Button cancelButton = (Button) dialog.findViewById(R.id.cancelButton);
                final EditText followerName = (EditText) dialog.findViewById(R.id.followerNameField);
                final EditText followerNicName = (EditText) dialog.findViewById(R.id.followerDisplayNameField);
                final EditText followerEmail = (EditText) dialog.findViewById(R.id.followerEmailField);
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!TextUtils.isEmpty(followerName.getText()) && !TextUtils.isEmpty(followerNicName.getText()) && !TextUtils.isEmpty(followerEmail.getText())) {


                            shareRest.createContact(followerName.getText().toString().trim(), followerEmail.getText().toString().trim(), new Callback<String>() {

                                        @Override
                                        public void onResponse(Response<String> response, Retrofit retrofit) {
                                            if (response.isSuccess()) {
                                                shareRest.createInvitationForContact(response.body(), new InvitationPayload(followerNicName.getText().toString().trim()), new Callback<String>() {
                                                    @Override
                                                    public void onResponse(Response<String> response, Retrofit retrofit) {
                                                        if (response.isSuccess()) {
                                                            populateFollowerList();
                                                            Toast.makeText(getApplicationContext(), "Follower invite sent succesfully", Toast.LENGTH_LONG).show();
                                                        } else {
                                                            Toast.makeText(getApplicationContext(), "Failed to invite follower", Toast.LENGTH_LONG).show();
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(Throwable t) {
                                                        Toast.makeText(getApplicationContext(), "Failed to invite follower", Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                            } else {
                                                Toast.makeText(getApplicationContext(), "Failed to invite follower", Toast.LENGTH_LONG).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Throwable t) {
                                            Toast.makeText(getApplicationContext(), "Failed to invite follower", Toast.LENGTH_LONG).show();
                                        }
                                    }
                            );
                        }
                        dialog.dismiss();
                    }
                });
                cancelButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        dialog.dismiss();
                                                    }
                                                });
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(dialog.getWindow().
                                getAttributes());
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                dialog.show();
                dialog.getWindow().setAttributes(lp);
            }
        });
    }
}
