package com.eveningoutpost.dexdrip.profileeditor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.Dex_Constants;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Profile;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.JamorhamShowcaseDrawer;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.ShotStateStore;
import com.eveningoutpost.dexdrip.utils.Preferences;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by jamorham on 21/06/2016.
 */

public class ProfileEditor extends BaseAppCompatActivity {

    private static final String TAG = "jamorhamprofile";
    private final List<ProfileItem> profileItemList = new ArrayList<>();
    private RecyclerView recyclerView;
    private ProfileAdapter mAdapter;
    //private Button cancelBtn;
    private static Button saveBtn;
    private static Button undoBtn;
    private static SeekBar adjustallSeekBar;
    private static TextView adjustPercentage;

    private static double last_conversion = 0;
    private static final boolean oneshot = true;
    private static ShowcaseView myShowcase;

    private static final int SHOWCASE_PROFILE_SPLIT = 501;

    public static final int MINS_PER_DAY = 1440; // 00:00
    private static final int END_OF_DAY = MINS_PER_DAY - 1; // 23:59

    private static boolean doMgdl;
    private boolean dataChanged = false;

    private static double adjustmentFactor = 1;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        doMgdl = (Pref.getString("units", "mgdl").equals("mgdl"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_editor);

        undoBtn = (Button) findViewById(R.id.profileUndoBtn);
        saveBtn = (Button) findViewById(R.id.profileSaveBtn);

        adjustallSeekBar = (SeekBar) findViewById(R.id.profileAdjustAllseekBar);
        adjustPercentage = (TextView) findViewById(R.id.profileAdjustAllPercentage);

        profileItemList.clear();
        profileItemList.addAll(loadData(true));

        //Toolbar toolbar = (Toolbar) findViewById(R.id.profile_toolbar);
        // setSupportActionBar(toolbar);

        JoH.fixActionBar(this);
        // TODO add menu in xml


        adjustallSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                adjustmentFactor = 1 + ((progress - 25) * 0.02);
                adjustPercentage.setText(JoH.qs(adjustmentFactor * 100, 0) + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {


                profileItemList.clear();
                // we must preserve the existing object reference used by the adapter
                profileItemList.addAll(loadData(false));
                mAdapter.resetTopMax(); // allow autosliding scales downwards
                forceRefresh(true); // does temporary save
            }
        });


        recyclerView = (RecyclerView) findViewById(R.id.profile_recycler_view);

        mAdapter = new ProfileAdapter(this, profileItemList, doMgdl);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());


        if (profileItemList.size() == 0) {
            profileItemList.add(new ProfileItem(0, END_OF_DAY,
                    JoH.tolerantParseDouble(Pref.getString("profile_carb_ratio_default", "10"), 10d),
                    JoH.tolerantParseDouble(Pref.getString("profile_insulin_sensitivity_default", "0.1"), 0.1d)));
        }

        updateAdjustmentFactor(1.0);

        shuffleFit();

        mAdapter.registerAdapterDataObserver(
                // handle incoming messages from the adapater
                new RecyclerView.AdapterDataObserver() {

                    @Override
                    public void onChanged() {
                        super.onChanged();
                        //  Log.d(TAG, "onChanged");

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
                            forceRefresh();
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
                            forceRefresh();
                            // split or delete
                        } else if (payload.toString().equals("long-split")) {

                            final DisplayMetrics dm = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getMetrics(dm);
                            final int screen_width = dm.widthPixels;
                            final int screen_height = dm.heightPixels;
                            boolean small_screen = false;
                            // smaller screens or lower version don't seem to play well with long dialog button names
                            if ((screen_width < 720) || (screen_height < 720) || ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M)))
                                small_screen = true;

                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                            alertDialogBuilder.setMessage(R.string.split_delete_or_do_nothing);

                            alertDialogBuilder.setPositiveButton(small_screen ? getString(R.string.split) : getString(R.string.split_this_block_in_two), new DialogInterface.OnClickListener() {
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
                                alertDialogBuilder.setNeutralButton(small_screen ? getString(R.string.delete) : getString(R.string.delete_this_time_block), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        profileItemList.remove(positionStart);
                                        forceRefresh();
                                    }
                                });
                            }

                            alertDialogBuilder.setNegativeButton(small_screen ? getString(R.string.cancel) : getString(R.string.cancel_do_nothing), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });

                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();
                        } else {

                            forceRefresh(); // for other non special changes
                        }


                    }
                });


        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };

        recyclerView.setItemAnimator(animator);
        recyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        showcasemenu(SHOWCASE_PROFILE_SPLIT);


    }

    @Override
    public void onResume() {
        super.onResume();
        profileItemList.clear();
        profileItemList.addAll(loadData(true));
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (dataChanged) {
            Profile.reloadPreferences();
            Intent intent = new Intent(ProfileEditor.this, Preferences.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("refresh", "");
            startActivity(intent);
        }
    }

    private static void updateAdjustmentFactor(double factor) {
        adjustmentFactor = factor;
        adjustPercentage.setText(JoH.qs(adjustmentFactor * 100, 0) + "%");

        int position = (int) ((adjustmentFactor - 1) / 0.02) + 25;
        adjustallSeekBar.setProgress(position > 0 ? position : 0);

    }

    private void saveData(boolean for_real) {
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                //.registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();

        final List<ProfileItem> profileItemListTmp = new ArrayList<>();


        if (!for_real) {
            Log.d(TAG, "Saving for real with adjustment factor: " + adjustmentFactor);
            // save working set clean of adjustment factor to avoid stacking
            for (ProfileItem item : profileItemList) {
                profileItemListTmp.add(item.clone());
            }

            for (ProfileItem item : profileItemListTmp) {
                item.carb_ratio = item.carb_ratio / adjustmentFactor;
                item.sensitivity = item.sensitivity * adjustmentFactor;
            }

        } else {
            // for real save
            profileItemListTmp.addAll(profileItemList); // no need to clone

            for (ProfileItem item : profileItemListTmp) {
                item.carb_ratio = (double) (Math.round(item.carb_ratio * 10)) / 10; // round to nearest .1g
                item.sensitivity = (double) (Math.round(item.sensitivity * 10)) / 10;
            }
        }

        String data = gson.toJson(profileItemListTmp);
        if (for_real) {
            saveBtn.setVisibility(View.INVISIBLE);
            saveProfileJson(data);
            UserError.Log.uel(TAG, "Saved Treatment Profile data, timeblocks:" + profileItemListTmp.size());
            updateAdjustmentFactor(1.0); // reset it
            dataChanged = true;
            Profile.invalidateProfile();

        } else {
            Pref.setString("saved_profile_list_json_working", data);
            saveBtn.setVisibility(View.VISIBLE);
            undoBtn.setVisibility(View.VISIBLE);
            Log.d(TAG, "Saved working data");
        }
    }

    public static void saveProfileJson(final String data) {
        Pref.setString("saved_profile_list_json", data);
        Pref.setString("saved_profile_list_json_working", "");
        Log.d(TAG, "Saved final data");
    }

    private void clearWorkingData() {
        Pref.setString("saved_profile_list_json_working", "");
    }

    public void profileCancelButton(View myview) {
        clearWorkingData();
        finish();
    }

    public void profileSaveButton(View myview) {
        saveData(true);
        finish();
    }

    public void profileUndoButton(View myview) {
        clearWorkingData();
        adjustmentFactor = 1;
        adjustallSeekBar.setProgress(25);
        profileItemList.clear();
        // we must preserve the existing object reference used by the adapter
        profileItemList.addAll(loadData(true));
        mAdapter.resetTopMax(); // allow autosliding scales downwards
        forceRefresh(false);

    }

    // convert between mg/dl or mmol when we change preferences
    public static void convertData(double multiplier) {
        if (last_conversion == multiplier) return; // prevent repeated adjustments
        last_conversion = multiplier;
        final List<ProfileItem> mydata = ProfileEditor.loadData(false);
        for (ProfileItem item : mydata) {
            item.carb_ratio = (double) (Math.round(item.carb_ratio * 10)) / 10; // round to nearest .1g
            item.sensitivity = (double) (Math.round(item.sensitivity * 10 * multiplier)) / 10;
        }
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeSpecialFloatingPointValues()
                .create();
        final String data = gson.toJson(mydata);

        Pref.setString("saved_profile_list_json", data);
        Pref.setString("saved_profile_list_json_working", "");
        UserError.Log.uel(TAG, "Converted Profile data with multiplier: " + ((multiplier == Dex_Constants.MG_DL_TO_MMOL_L) ? " to mmol/l" : "to mg/dl"));
    }

    public static List<ProfileItem> loadData(boolean buttons) {
        final List<ProfileItem> myprofileItemList = new ArrayList<>();
        String data = Pref.getString("saved_profile_list_json_working", "");
        if (data.length() == 0) {
            data = Pref.getString("saved_profile_list_json", "");

            if (buttons) {
                saveBtn.setVisibility(View.INVISIBLE);
                undoBtn.setVisibility(View.INVISIBLE);
            }
            Log.d(TAG, "Loaded real data");
        } else {
            Log.d(TAG, "Loaded working data");
            if (buttons) {
                saveBtn.setVisibility(View.VISIBLE);
                undoBtn.setVisibility(View.VISIBLE);
            }
        }


        ProfileItem[] restored = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(data, ProfileItem[].class);
        if (restored != null) {
            // process adjustment factor
            for (ProfileItem item : restored) {
                item.carb_ratio = item.carb_ratio * adjustmentFactor;
                item.sensitivity = item.sensitivity / adjustmentFactor;
            }
            Collections.addAll(myprofileItemList, restored);
        }
        if (myprofileItemList.size() == 0) {
            try {
                Log.d(TAG,"Creating default profile entries: sens default: "+ Pref.getString("profile_insulin_sensitivity_default", "0.1"));
                ProfileItem item = new ProfileItem(0, END_OF_DAY, Double.parseDouble(Pref.getString("profile_carb_ratio_default", "10")),
                        Double.parseDouble(Pref.getString("profile_insulin_sensitivity_default", "0.1")));
                myprofileItemList.add(item);
            } catch (Exception e) {
                Home.toaststatic("Problem with default insulin parameters");
            }
        }
        return myprofileItemList;
    }

    public static String minMaxCarbs(List<ProfileItem> myprofileItemList) {
        double carbsmin = 9999999;
        double carbsmax = -1;
        for (ProfileItem item : myprofileItemList) {
            if (item.carb_ratio > carbsmax) carbsmax = item.carb_ratio;
            if (item.carb_ratio < carbsmin) carbsmin = item.carb_ratio;
        }
        if (carbsmin == carbsmax) return JoH.qs(carbsmin, -1);
        return JoH.qs(carbsmin, -1) + " - " + JoH.qs(carbsmax, -1);
    }

    public static String minMaxSens(List<ProfileItem> myprofileItemList) {
        double sensmin = 9999999;
        double sensmax = -1;
        for (ProfileItem item : myprofileItemList) {
            if (item.sensitivity > sensmax) sensmax = item.sensitivity;
            if (item.sensitivity < sensmin) sensmin = item.sensitivity;
        }
        if (sensmin == sensmax) return JoH.qs(sensmin, -1);
        return JoH.qs(sensmin, -1) + " - " + JoH.qs(sensmax, -1);
    }


    private void forceRefresh() {
        forceRefresh(true);
    }

    private void forceRefresh(boolean save) {
        shuffleFit();
        if (save) saveData(false);

        mAdapter.calcTopScale();
        mAdapter.first_run = profileItemList.size();
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
        if (profileItemList.size() > 0) {
            for (i = 1; i < profileItemList.size(); i++) {
                ProfileItem current = profileItemList.get(i);
                ProfileItem previous = profileItemList.get(i - 1);

                if (current.start_min < previous.end_min) current.start_min = previous.end_min + 1;

            }
        }

    }

    private synchronized void showcasemenu(int option) {
        if ((myShowcase != null) && (myShowcase.isShowing())) return;
        if (ShotStateStore.hasShot(option)) return;
        try {
            ProfileViewTarget target = null;
            String title = "";
            String message = "";

            switch (option) {

                case SHOWCASE_PROFILE_SPLIT:
                    target = new ProfileViewTarget(R.id.profile_recycler_view, this, 40, 40);

                    title = getString(R.string.long_press_to_split_or_delete);
                    message = getString(R.string.press_and_hold_on_the_background_to_split_or_delete);
                    break;

            }


            if (target != null) {
                myShowcase = new ShowcaseView.Builder(this)

                        .setTarget(target)
                        .setStyle(R.style.CustomShowcaseTheme2)
                        .setContentTitle(title)
                        .setContentText("\n" + message)
                        .setShowcaseDrawer(new JamorhamShowcaseDrawer(getResources(), getTheme(), 90, 14))
                        .singleShot(oneshot ? option : -1)
                        .build();

                myShowcase.setBackgroundColor(Color.TRANSPARENT);
                myShowcase.show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception in showcase: " + e.toString());
        }
    }

    public class ProfileViewTarget implements Target {

        private final View mView;
        private int offsetX = 0;
        private int offsetY = 0;

        public ProfileViewTarget(View view) {
            mView = view;
        }

        public ProfileViewTarget(int viewId, Activity activity) {
            mView = activity.findViewById(viewId);
        }

        public ProfileViewTarget(int viewId, Activity activity, int offsetX, int offsetY) {
            mView = activity.findViewById(viewId);
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }


        @Override
        public Point getPoint() {
            int[] location = new int[2];
            mView.getLocationInWindow(location);
            int x, y;
            if ((offsetX != 0) || (offsetY != 0)) {
                // TODO to implement DP we probably need to check scaling!!!
                x = offsetX + location[0]; // from top left if offset specified
                y = offsetY + location[1];
            } else {
                x = location[0] + mView.getWidth() / 2;
                y = location[1] + mView.getHeight() / 2;
            }
            return new Point(x, y);
        }
    }


}

