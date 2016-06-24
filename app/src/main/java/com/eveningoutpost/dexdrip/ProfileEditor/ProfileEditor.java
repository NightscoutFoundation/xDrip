package com.eveningoutpost.dexdrip.ProfileEditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jamorham on 21/06/2016.
 */

public class ProfileEditor extends AppCompatActivity {

    private static final String TAG = "jamorhamprofile";
    private List<ProfileItem> profileItemList = new ArrayList<>();
    private RecyclerView recyclerView;
    private ProfileAdapter mAdapter;

    public static final int MINS_PER_DAY = 1440; // 00:00
    private static final int END_OF_DAY = MINS_PER_DAY - 1; // 23:59

    private static boolean doMgdl;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        doMgdl = (Home.getPreferencesStringWithDefault("units", "mgdl").equals("mgdl"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_editor);
        loadData();



        //Toolbar toolbar = (Toolbar) findViewById(R.id.profile_toolbar);
        // setSupportActionBar(toolbar);

        JoH.fixActionBar(this);
        // TODO add menu in xml

        recyclerView = (RecyclerView) findViewById(R.id.profile_recycler_view);

        mAdapter = new ProfileAdapter(this, profileItemList, doMgdl);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());


        if (profileItemList.size()==0)
        {
            profileItemList.add(new ProfileItem(0, END_OF_DAY,
                    JoH.tolerantParseDouble(Home.getPreferencesStringWithDefault("profile_carb_ratio_default", "10")),
                    JoH.tolerantParseDouble(Home.getPreferencesStringWithDefault("profile_insulin_sensitivity_default", "0.1"))));
        }


        shuffleFit();

        mAdapter.registerAdapterDataObserver(
                new RecyclerView.AdapterDataObserver() {

                    @Override
                    public void onChanged() {
                        super.onChanged();
                        Log.d(TAG, "onChanged");

                    }

                    @Override
                    public void onItemRangeChanged(final int positionStart, int itemCount, Object payload) {
                        super.onItemRangeChanged(positionStart, itemCount, payload);

                        Log.d(TAG, "onItemRangeChanged: pos:" + positionStart + " cnt:" + itemCount + " p: " + payload.toString());

                        if (payload.toString().equals("time start updated")) {
                            if (positionStart > 0) {
                                // did we encroach on previous item end or open up a gap??
                                if ((profileItemList.get(positionStart - 1).end_min != profileItemList.get(positionStart).start_min - 1)) {
                                    profileItemList.get(positionStart - 1).end_min = profileItemList.get(positionStart).start_min - 1;
                                }
                            } else {
                                // if setting the first start time also mirror this to the last end time
                                profileItemList.get(profileItemList.size() - 1).end_min = profileItemList.get(positionStart).start_min - 1; //
                            }
                        } else if (payload.toString().equals("time end updated")) {
                            // don't apply gap calcualtion to last item when end changes
                            if (positionStart < profileItemList.size() - 1) {
                                // did we encroach on previous item end or open up a gap??

                                // for all blocks other than the final one
                                if ((profileItemList.get(positionStart + 1).start_min != profileItemList.get(positionStart).end_min + 1)) {
                                    profileItemList.get(positionStart + 1).start_min = profileItemList.get(positionStart).end_min + 1;
                                }
                            } else {
                                // if setting the last end time also mirror this to the first start time
                                profileItemList.get(0).start_min = profileItemList.get(positionStart).end_min + 1; //
                            }
                            // split or delete
                        } else if (payload.toString().equals("long-split")) {

                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                            alertDialogBuilder.setMessage("Split delete or do nothing");

                            alertDialogBuilder.setPositiveButton("Split this block in two", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {

                                    ProfileItem old_item = profileItemList.get(positionStart);
                                    ProfileItem new_item;
                                    if (old_item.start_min < old_item.end_min) {
                                        // no wrapping time
                                        new_item = new ProfileItem(old_item.end_min - (old_item.end_min - old_item.start_min) / 2, old_item.end_min, old_item.carb_ratio, old_item.sensitivity);
                                    } else {
                                        // wraps around
                                        new_item = new ProfileItem(old_item.end_min - ((old_item.end_min - MINS_PER_DAY) - old_item.start_min) / 2, old_item.end_min, old_item.carb_ratio, old_item.sensitivity);

                                    }
                                    old_item.end_min = new_item.start_min - 1;
                                    profileItemList.add(positionStart + 1, new_item);
                                    forceRefresh();

                                }
                            });
                            if (profileItemList.size() > 1) {
                                alertDialogBuilder.setNeutralButton("Delete this time block", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        profileItemList.remove(positionStart);
                                        forceRefresh();
                                    }
                                });
                            }

                            alertDialogBuilder.setNegativeButton("Cancel - do nothing", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });

                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();
                        }

                       forceRefresh();

                    }
                });


        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };

        // mAdapter.max_position=profileItemList.size();

        recyclerView.setItemAnimator(animator);
        recyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();



    }

    private void saveData()
    {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                //.registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();
        String data =  gson.toJson(profileItemList);
        Home.setPreferencesString("saved_profile_list_json",data);
    }

    private void loadData()
    {
        String data = Home.getPreferencesStringWithDefault("saved_profile_list_json", "");

        ProfileItem[] restored = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(data, ProfileItem[].class);
        if (restored!=null) {
            profileItemList.clear();
            for (ProfileItem datab : restored) {
                profileItemList.add(datab);
            }
        }
    }

    private void forceRefresh()
    {
        shuffleFit();
        saveData(); // TODO give button to save changes

        mAdapter.first_run = profileItemList.size();
        mAdapter.calcTopScale();
        recyclerView.invalidate();
        recyclerView.refreshDrawableState();
        mAdapter.notifyDataSetChanged();
    }

    private void shuffleFit() {
        int i;

        // always wrap around
        if (profileItemList.size() > 1) {
            profileItemList.get(0).start_min = profileItemList.get(profileItemList.size() - 1).end_min + 1;
        } else {
            // only one time period possible with 1 item
            profileItemList.get(0).start_min = 0;
            profileItemList.get(0).end_min = END_OF_DAY;
        }

        // sanity check
        for (ProfileItem item : profileItemList) {

            item.end_min = item.end_min % MINS_PER_DAY;
            item.start_min = item.start_min % MINS_PER_DAY;
        }

        // shuffle up
        if (profileItemList.size()>0) {
            for (i = 1; i < profileItemList.size(); i++) {
                ProfileItem current = profileItemList.get(i);
                ProfileItem previous = profileItemList.get(i - 1);

                if (current.start_min < previous.end_min) current.start_min = previous.end_min + 1;

            }
        }

    }


}

