package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.SendFeedBack;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.util.ArrayList;
import java.util.List;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;

/**
 * Created by Emma Black on 8/3/15.
 * This is the old logs activity.
 */
public class ErrorsActivity extends ActivityWithMenu {
    public static final String menu_name = "Errors";
    private static final String TAG = "ErrorView";
    public String getMenuName() { return  menu_name; }
    private CheckBox highCheckboxView;
    private CheckBox mediumCheckboxView;
    private CheckBox lowCheckboxView;
    private CheckBox userEventLowCheckboxView;
    private CheckBox userEventHighCheckboxView;
    private Switch autoRefreshSwitch;
    private ListView errorList;
    private List<UserError> errors;
    private List<UserError> errors_tmp = new ArrayList<>();
    private ErrorListAdapter adapter;
    private boolean autoRefresh = false;
    private Handler handler = new Handler();
    private static final boolean d = false;
    private boolean is_visible = false;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (mPrefs.getBoolean("wear_sync", false) && mPrefs.getBoolean("sync_wear_logs", false)) {
            startWatchUpdaterService(this, WatchUpdaterService.ACTION_SYNC_LOGS, TAG);
        }
        setContentView(R.layout.activity_errors);

        highCheckboxView = (CheckBox) findViewById(R.id.highSeverityCheckbox);
        mediumCheckboxView = (CheckBox) findViewById(R.id.midSeverityCheckbox);
        lowCheckboxView = (CheckBox) findViewById(R.id.lowSeverityCheckBox);
        userEventLowCheckboxView = (CheckBox) findViewById(R.id.userEventLowCheckbox);
        userEventHighCheckboxView = (CheckBox) findViewById(R.id.userEventHighCheckbox);
        autoRefreshSwitch = (Switch) findViewById(R.id.autorefresh);

        highCheckboxView.setOnClickListener(checkboxListener);
        mediumCheckboxView.setOnClickListener(checkboxListener);
        lowCheckboxView.setOnClickListener(checkboxListener);
        userEventLowCheckboxView.setOnClickListener(checkboxListener);
        userEventHighCheckboxView.setOnClickListener(checkboxListener);

        autoRefreshSwitch.setOnCheckedChangeListener(switchChangeListener);


        Intent intent = getIntent();
        if (intent != null) {
            final Bundle bundle = intent.getExtras();
            if (bundle != null) {
                final String str = bundle.getString("events");
                if (str != null) {
                    userEventHighCheckboxView.setChecked(true);
                    userEventLowCheckboxView.setChecked(PersistentStore.getBoolean("events-userlowcheckbox"));
                    mediumCheckboxView.setChecked(PersistentStore.getBoolean("events-mediumcheckbox"));
                    highCheckboxView.setChecked(PersistentStore.getBoolean("events-highcheckbox"));
                    lowCheckboxView.setChecked(PersistentStore.getBoolean("events-lowcheckbox"));
                }
            }
        }


        updateErrors();
        errorList = (ListView) findViewById(R.id.errorList);
        adapter = new ErrorListAdapter(getApplicationContext(), errors);
        errorList.setAdapter(adapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        is_visible=false;
        autoRefresh=false;
    }

    @Override
    public void onResume() {
        super.onResume();
        is_visible=true;
        autoRefreshSwitch.setChecked(autoRefresh); // turn off after gone in to background

    }

    private View.OnClickListener checkboxListener = new View.OnClickListener() {
        public void onClick(View v) {
            updateErrors();

        }
    };

    public void uploadLogs(View v) {
        StringBuilder tmp = new StringBuilder(20000);
        tmp.append("The following logs will be sent to the developers: \n\nPlease also include your email address or we will not know who they are from!\n\n");
        for (UserError item : errors) {
            tmp.append(item.toString());
            tmp.append("\n");
            if (tmp.length() > 200000) {
                JoH.static_toast(this, "Could not package up all logs, using most recent", Toast.LENGTH_LONG);
                break;
            }
        }
        startActivity(new Intent(getApplicationContext(), SendFeedBack.class).putExtra("generic_text", tmp.toString()));
    }


    private CheckBox.OnCheckedChangeListener switchChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {
            if (isChecked && !autoRefresh) handler.postDelayed(runnable, 1000); // start timer
            autoRefresh = isChecked;

            if (autoRefresh) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (mPrefs.getBoolean("wear_sync", false) && mPrefs.getBoolean("sync_wear_logs", false)) {
                    startWatchUpdaterService(getApplicationContext(), WatchUpdaterService.ACTION_SYNC_LOGS, TAG);
                }
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    };


    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (autoRefresh && is_visible)
            {
                updateErrors(true);
                handler.postDelayed(this, 1000);
            }
        }
    };


    public void updateErrors() {
        updateErrors(false);
    }

    public void updateErrors(boolean from_timer) {
        List<Integer> severitiesList = new ArrayList<>();

        PersistentStore.setBoolean("events-highcheckbox", highCheckboxView.isChecked());
        PersistentStore.setBoolean("events-mediumcheckbox", mediumCheckboxView.isChecked());
        PersistentStore.setBoolean("events-lowcheckbox", lowCheckboxView.isChecked());
        PersistentStore.setBoolean("events-userlowcheckbox", userEventLowCheckboxView.isChecked());
        PersistentStore.setBoolean("events-userhighcheckbox", userEventHighCheckboxView.isChecked());

        if (highCheckboxView.isChecked()) severitiesList.add(3);
        if (mediumCheckboxView.isChecked()) severitiesList.add(2);
        if (lowCheckboxView.isChecked()) severitiesList.add(1);
        if (userEventLowCheckboxView.isChecked()) severitiesList.add(5);
        if (userEventHighCheckboxView.isChecked()) severitiesList.add(6);
        if(errors == null) {
            errors = UserError.bySeverity(severitiesList.toArray(new Integer[severitiesList.size()]));
            if (adapter != null) adapter.notifyDataSetChanged();
        } else {
            if (from_timer) {
                errors_tmp.clear();
                errors_tmp.addAll(UserError.bySeverity(severitiesList.toArray(new Integer[severitiesList.size()])));
                if (errors_tmp.size()!=errors.size())
                {
                    errors.clear();
                    errors.addAll(errors_tmp);
                    if (adapter != null) adapter.notifyDataSetChanged();
                    if (d) UserError.Log.d(TAG,"Updating list with new data");
                } else {
                    if (d) UserError.Log.d(TAG,"List sizes the same: "+errors.size());
                }
            } else {
                errors.clear();
                errors.addAll(UserError.bySeverity(severitiesList.toArray(new Integer[severitiesList.size()])));
                if (adapter != null) adapter.notifyDataSetChanged();
            }
        }
    }
}
