package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.eveningoutpost.dexdrip.xdrip.gs;

public class AlertList extends ActivityWithMenu {
    ListView listViewLow;
    ListView listViewHigh;
    Button createLowAlert;
    Button createHighAlert;
    boolean doMgdl;
    Context mContext;
    final int ADD_ALERT = 1;
    final int EDIT_ALERT = 2;
    SharedPreferences prefs;
    Animation anim;
    private final static String TAG = AlertPlayer.class.getSimpleName();

    String stringTimeFromAlert(AlertType alert) {
        if (alert.all_day) {
            return getString(R.string.all_day);
        }
        String start = timeFormatString(AlertType.time2Hours(alert.start_time_minutes), AlertType.time2Minutes(alert.start_time_minutes));
        String end = timeFormatString(AlertType.time2Hours(alert.end_time_minutes), AlertType.time2Minutes(alert.end_time_minutes));
        return start + " - " + end;
    }

    HashMap<String, String> createAlertMap(AlertType alert) {
        HashMap<String, String> map = new HashMap<String, String>();
        String overrideSilentMode = getString(R.string.override_silent_mode);
        if (!alert.override_silent_mode) {
            overrideSilentMode = getString(R.string.no_alert_in_silent_mode);
        }
        // We use a - sign to tell that this text should be stiked through
        String extra = "-";
        if (alert.active) {
            extra = "+";
        }


        map.put("alertName", extra + alert.name);
        map.put("alertThreshold", extra + EditAlertActivity.unitsConvert2Disp(doMgdl, alert.threshold));
        map.put("alertTime", extra + stringTimeFromAlert(alert));
        map.put("alertMp3File", extra + shortPath(alert.mp3_file));
        map.put("alertOverrideSilenceMode", extra + overrideSilentMode);
        map.put("uuid", alert.uuid);

        return map;
    }

    ArrayList<HashMap<String, String>> createAlertsMap(boolean above) {
        ArrayList<HashMap<String, String>> feedList = new ArrayList<HashMap<String, String>>();

        List<AlertType> alerts = AlertType.getAll(above);
        for (AlertType alert : alerts) {
            Log.d(TAG, alert.toString());
            feedList.add(createAlertMap(alert));
        }
        return feedList;
    }


    class AlertsOnItemLongClickListener implements AdapterView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id) {
            anim.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                    ListView lv = (ListView) parent;
                    @SuppressWarnings("unchecked")
                    HashMap<String, String> item = (HashMap<String, String>) lv.getItemAtPosition(position);
                    Log.d(TAG, "Item clicked " + lv.getItemAtPosition(position) + item.get("uuid"));

                    //The XML for each item in the list (should you use a custom XML) must have android:longClickable="true"
                    // as well (or you can use the convenience method lv.setLongClickable(true);). This way you can have a list
                    // with only some items responding to longclick. (might be used for non removable alerts)

                    xdrip.checkForcedEnglish(xdrip.getAppContext());
                    Intent myIntent = new Intent(AlertList.this, EditAlertActivity.class);
                    myIntent.putExtra("uuid", item.get("uuid")); //Optional parameters
                    AlertList.this.startActivityForResult(myIntent, EDIT_ALERT);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }
            });
            view.startAnimation(anim);
            return true;
        }
    }

    @Override
    protected void onResume() {
        xdrip.checkForcedEnglish(xdrip.getAppContext());
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_list);
        mContext = getApplicationContext();
        listViewLow = (ListView) findViewById(R.id.listView_low);
        listViewHigh = (ListView) findViewById(R.id.listView_high);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        doMgdl = (prefs.getString("units", "mgdl").compareTo("mgdl") == 0);

        addListenerOnButton();
        FillLists();
        anim = AnimationUtils.loadAnimation(this, R.anim.fade_anim);
        listViewLow.setOnItemLongClickListener(new AlertsOnItemLongClickListener());
        listViewHigh.setOnItemLongClickListener(new AlertsOnItemLongClickListener());
    }

    @Override
    public String getMenuName() {
        return getString(R.string.level_alerts);
    }


    public void addListenerOnButton() {
        createLowAlert = (Button) findViewById(R.id.button_create_low);
        createHighAlert = (Button) findViewById(R.id.button_create_high);

        createLowAlert.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                xdrip.checkForcedEnglish(xdrip.getAppContext());
                Intent myIntent = new Intent(AlertList.this, EditAlertActivity.class);
                myIntent.putExtra("above", "false");
                AlertList.this.startActivityForResult(myIntent, ADD_ALERT);
            }

        });

        createHighAlert.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                xdrip.checkForcedEnglish(xdrip.getAppContext());
                Intent myIntent = new Intent(AlertList.this, EditAlertActivity.class);
                myIntent.putExtra("above", "true");
                AlertList.this.startActivityForResult(myIntent, ADD_ALERT);
            }
        });
    }

    void displayWarning() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (!isFinishing()) {
                    new AlertDialog.Builder(AlertList.this)
                            .setTitle("Warning !")
                            .setMessage("No active Low Alert exists, without this there will be no alert on low glucose! Please add or enable a low alert.")
                            .setCancelable(false)
                            .setPositiveButton(
                                    "Ok",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    })
                            .create().show();
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult called request code  = " + requestCode + " result code " + resultCode);
        if (!AlertType.activeLowAlertExists()) {
            displayWarning();
        }
        if (requestCode == ADD_ALERT || requestCode == EDIT_ALERT) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "onActivityResult called invalidating...");
                FillLists();
            }
            if (resultCode == RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }

    void FillLists() {
        // We use a - sign to tell that this text should be stiked through
        SimpleAdapter.ViewBinder vb = new SimpleAdapter.ViewBinder() {
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                TextView tv = (TextView) view;
                tv.setText(textRepresentation.substring(1));
                if (textRepresentation.substring(0, 1).equals("-")) {
                    tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }
                return true;
            }
        };

        ArrayList<HashMap<String, String>> feedList;
        feedList = createAlertsMap(false);
        SimpleAdapter simpleAdapterLow = new SimpleAdapter(this, feedList, R.layout.row_alerts, new String[]{"alertName", "alertThreshold", "alertTime", "alertMp3File", "alertOverrideSilenceMode"}, new int[]{R.id.alertName, R.id.alertThreshold, R.id.alertTime, R.id.alertMp3File, R.id.alertOverrideSilent});
        simpleAdapterLow.setViewBinder(vb);

        listViewLow.setAdapter(simpleAdapterLow);

        feedList = createAlertsMap(true);
        SimpleAdapter simpleAdapterHigh = new SimpleAdapter(this, feedList, R.layout.row_alerts, new String[]{"alertName", "alertThreshold", "alertTime", "alertMp3File", "alertOverrideSilenceMode"}, new int[]{R.id.alertName, R.id.alertThreshold, R.id.alertTime, R.id.alertMp3File, R.id.alertOverrideSilent});
        simpleAdapterHigh.setViewBinder(vb);
        listViewHigh.setAdapter(simpleAdapterHigh);
    }

    private String shortPath(final String path) {
        try {
            return EditAlertActivity.shortPath(path);
        } catch (SecurityException e) {
            // need external storage permission?
            checkStoragePermissions(gs(R.string.need_permission_to_access_audio_files));
            return "";
        }
    }

    // TODO this can be centralized
    private final static int MY_PERMISSIONS_REQUEST_STORAGE = 104;

    private boolean checkStoragePermissions(String msg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                final Activity activity = this;
                JoH.show_ok_dialog(activity, gs(R.string.please_allow_permission), msg, () -> ActivityCompat.requestPermissions(activity,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE));
                return false;
            }
        }
        return true;
    }

    public String timeFormatString(int hour, int minute) {
        SimpleDateFormat timeFormat24 = new SimpleDateFormat("HH:mm");
        String selected = hour + ":" + ((minute < 10) ? "0" : "") + minute;
        if (!android.text.format.DateFormat.is24HourFormat(mContext)) {
            try {
                Date date = timeFormat24.parse(selected);
                SimpleDateFormat timeFormat12 = new SimpleDateFormat("hh:mm aa");
                return timeFormat12.format(date);
            } catch (final ParseException e) {
                e.printStackTrace();
            }
        }
        return selected;
    }
}
