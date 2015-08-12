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

import com.eveningoutpost.dexdrip.ShareModels.FollowerManager;
import com.eveningoutpost.dexdrip.ShareModels.Models.ExistingFollower;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.util.List;

import rx.functions.Action1;

/**
 * Created by stephenblack on 8/11/15.
 */
public class FollowerManagementActivity extends ActivityWithMenu {
    static String FollowerManagementActivity = Home.class.getName();
    public static String menu_name = "Follower Management";

    ListView existingFollowersView;
    Button addFollowerButton;
    FollowerManager followerManager;
    Action1<List<ExistingFollower>> existingFollowerListener;
    List<ExistingFollower> existingFollowerList;
    FollowerListAdapter followerListAdapter;

    String login;
    String password;
    String receiverSn;


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
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        login = prefs.getString("dexcom_account_name", "");
        password = prefs.getString("dexcom_account_password", "");
        receiverSn = prefs.getString("share_key", "SM00000000").toUpperCase();

        populateFollowerList();
        setInviteListener();
    }

    private void populateFollowerList() {
        followerManager = new FollowerManager(login, password, receiverSn, getApplicationContext());
         existingFollowerListener = new Action1<List<ExistingFollower>>() {
            @Override
            public void call(List<ExistingFollower> existingFollowers) {
                if(followerListAdapter != null) {
                    existingFollowerList.clear();
                    existingFollowerList.addAll(existingFollowers);
                    followerListAdapter.notifyDataSetChanged();
                } else {
                    existingFollowerList = existingFollowers;
                    followerListAdapter = new FollowerListAdapter(getApplicationContext(), followerManager, existingFollowerList);
                    existingFollowersView.setAdapter(followerListAdapter);
                }
            }
        };
        followerManager.getFollowers(existingFollowerListener);
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
                            Action1<Boolean> invitedFollowerListener = new Action1<Boolean>() {
                                @Override
                                public void call(Boolean deleted) {
                                    if (deleted) {
                                        Toast.makeText(getApplicationContext(), "Follower invite sent succesfully", Toast.LENGTH_LONG).show();
                                        populateFollowerList();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Failed to invite follower", Toast.LENGTH_LONG).show();
                                    }
                                }
                            };
                            followerManager.inviteFollower(followerEmail.getText().toString().trim(), followerName.getText().toString().trim(), followerNicName.getText().toString().trim(), invitedFollowerListener);
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
                lp.copyFrom(dialog.getWindow().getAttributes());
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                dialog.show();
                dialog.getWindow().setAttributes(lp);
            }
        });
    }
}
