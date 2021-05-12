package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Paint;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.text.format.DateFormat;
import android.text.method.DigitsKeyListener;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;
import static com.eveningoutpost.dexdrip.xdrip.gs;

public class EditAlertActivity extends ActivityWithMenu {
    //public static String menu_name = "Edit Alert";

    private TextView viewHeader;

    private EditText alertText;
    private EditText alertThreshold;
    private EditText alertMp3File;
    private EditText editSnooze;
    private EditText reraise;

    private Button buttonalertMp3;

    private Button buttonSave;
    private Button buttonRemove;
    private Button buttonTest;
    private Button buttonPreSnooze;
    private CheckBox checkboxAllDay;
    private CheckBox checkboxVibrate;
    private CheckBox checkboxDisabled;

    private LinearLayout layoutTimeBetween;
    private LinearLayout timeInstructions;
    private TextView viewTimeStart;
    private TextView viewTimeEnd;
    private TextView timeInstructionsStart;
    private TextView timeInstructionsEnd;

    private int startHour = 0;
    private int startMinute = 0;
    private int endHour = 23;
    private int endMinute = 59;

    private String audioPath;

    private LinearLayout layoutSilentModeWarning;
    private TextView viewAlertOverrideText;
    private CheckBox checkboxOverrideSilent;
    private CheckBox checkboxForceSpeaker;
    private boolean doMgdl;

    private String uuid;
    private Context mContext;
    private boolean above;
    private final int REQUEST_CODE_CHOOSE_FILE = 1;
    private final static int MY_PERMISSIONS_REQUEST_STORAGE = 138;
    
    private final int MIN_ALERT = 40;
    private final int MAX_ALERT = 400;

    private final static String TAG = AlertPlayer.class.getSimpleName();

    String getExtra(Bundle savedInstanceState, String paramName, String defaultVal) {
        String newString;
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                newString= null;
            } else {
                newString= extras.getString(paramName);
            }
        } else {
            newString = (String) savedInstanceState.getSerializable(paramName);
        }
        if (newString != null) {
        	return newString;
        } else {
        	return defaultVal;
        }
    }

    @Override
    protected void onResume() {
        xdrip.checkForcedEnglish(xdrip.getAppContext());
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        xdrip.checkForcedEnglish(xdrip.getAppContext());
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_edit_alert);

        viewHeader = (TextView) findViewById(R.id.view_alert_header);

        buttonSave = (Button)findViewById(R.id.edit_alert_save);
        buttonRemove = (Button)findViewById(R.id.edit_alert_remove);
        buttonTest = (Button)findViewById(R.id.edit_alert_test);
        buttonalertMp3 = (Button)findViewById(R.id.Button_alert_mp3_file);
        buttonPreSnooze = (Button)findViewById(R.id.edit_alert_pre_snooze);


        alertText = (EditText) findViewById(R.id.edit_alert_text);
        alertThreshold = (EditText) findViewById(R.id.edit_alert_threshold);
        alertMp3File = (EditText) findViewById(R.id.edit_alert_mp3_file);

        checkboxAllDay = (CheckBox) findViewById(R.id.check_alert_time);
        checkboxVibrate = (CheckBox) findViewById(R.id.check_vibrate);
        checkboxDisabled = (CheckBox) findViewById(R.id.view_alert_check_disable);

        layoutTimeBetween = (LinearLayout) findViewById(R.id.time_between);
        timeInstructions = (LinearLayout) findViewById(R.id.time_instructions);
        timeInstructionsStart = (TextView) findViewById(R.id.time_instructions_start);
        timeInstructionsEnd = (TextView) findViewById(R.id.time_instructions_end);


        viewTimeStart = (TextView) findViewById(R.id.view_alert_time_start);
        viewTimeEnd = (TextView) findViewById(R.id.view_alert_time_end);
        editSnooze = (EditText) findViewById(R.id.edit_snooze);
        reraise = (EditText) findViewById(R.id.reraise);

        layoutSilentModeWarning = (LinearLayout) findViewById(R.id.layout_silent_mode_warning);
        viewAlertOverrideText = (TextView) findViewById(R.id.view_alert_override_silent);
        checkboxOverrideSilent = (CheckBox) findViewById(R.id.check_override_silent);
        checkboxForceSpeaker = (CheckBox) findViewById(R.id.check_force_speaker);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        addListenerOnButtons();

        if(BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
            viewHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            buttonSave.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            buttonRemove.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            buttonTest.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            buttonalertMp3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            
            buttonPreSnooze.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            alertText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            alertThreshold.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            alertMp3File.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);

            checkboxAllDay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            checkboxVibrate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            checkboxDisabled.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);

            viewTimeStart.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            viewTimeEnd.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            editSnooze.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            reraise.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            viewAlertOverrideText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);

            ((TextView) findViewById(R.id.view_alert_text)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            ((TextView) findViewById(R.id.view_alert_threshold)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            ((TextView) findViewById(R.id.view_alert_default_snooze)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            ((TextView) findViewById(R.id.view_alert_mp3_file)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            ((TextView) findViewById(R.id.view_alert_time)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            ((TextView) findViewById(R.id.view_alert_time_between)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            ((TextView) findViewById(R.id.view_alert_disable)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);

        }
        SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        doMgdl = (prefs.getString("units", "mgdl").compareTo("mgdl") == 0);

        if(!doMgdl) {
            alertThreshold.setInputType(InputType.TYPE_CLASS_NUMBER);
            alertThreshold.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
            alertThreshold.setKeyListener(DigitsKeyListener.getInstance(false,true));
        }

        uuid = getExtra(savedInstanceState, "uuid", null);
        String status;
        int alertReraise;
        int defaultSnooze;
        if (uuid == null) {
            // This is a new alert
            above = Boolean.parseBoolean(getExtra(savedInstanceState, "above", null));
            checkboxAllDay.setChecked(true);
            checkboxVibrate.setChecked(true);
            checkboxDisabled.setChecked(false);
            checkboxOverrideSilent.setChecked(true);
            checkboxForceSpeaker.setChecked(true);

            audioPath = "";
            alertMp3File.setText(shortPath(audioPath));
            alertMp3File.setKeyListener(null);
            defaultSnooze = SnoozeActivity.getDefaultSnooze(above);
            buttonRemove.setVisibility(View.GONE);
            // One can not snooze an alert that is still not in the database...
            buttonPreSnooze.setVisibility(View.GONE);
            status = getString(R.string.adding)+" " + (above ? getString(R.string.high) : getString(R.string.low)) + " "+getString(R.string.alert);
            startHour = 0;
            startMinute = 0;
            endHour = 23;
            endMinute = 59;
            alertReraise = 1;
        } else {
            // We are editing an alert
            AlertType alertType = AlertType.get_alert(uuid);
            if(alertType==null) {
                Log.wtf(TAG, "Error editing alert, when that alert does not exist...");
                Intent returnIntent = new Intent();
                setResult(RESULT_CANCELED, returnIntent);
                finish();
                return;
            }

            above = alertType.above;
            alertText.setText(alertType.name);
            alertThreshold.setText(unitsConvert2Disp(doMgdl, alertType.threshold));
            checkboxAllDay.setChecked(alertType.all_day);
            checkboxVibrate.setChecked(alertType.vibrate);
            checkboxDisabled.setChecked(!alertType.active);
            checkboxOverrideSilent.setChecked(alertType.override_silent_mode);
            checkboxForceSpeaker.setChecked(alertType.force_speaker);
            defaultSnooze = alertType.default_snooze;
            if(defaultSnooze == 0) {
                SnoozeActivity.getDefaultSnooze(above);
            }

            audioPath = getExtra(savedInstanceState, "audioPath" ,alertType.mp3_file);
            alertMp3File.setText(shortPath(audioPath));

            status = "editing " + (above ? "high" : "low") + " alert";
            startHour = AlertType.time2Hours(alertType.start_time_minutes);
            startMinute = AlertType.time2Minutes(alertType.start_time_minutes);
            endHour = AlertType.time2Hours(alertType.end_time_minutes);
            endMinute = AlertType.time2Minutes(alertType.end_time_minutes);
            alertReraise = alertType.minutes_between;

            if(uuid.equals(AlertType.LOW_ALERT_55)) {
                // This is the 55 alert, can not be edited
                alertText.setKeyListener(null);
                alertThreshold.setKeyListener(null);
                buttonalertMp3.setEnabled(false);
                checkboxAllDay.setEnabled(false);
                checkboxVibrate.setEnabled(false);
                checkboxOverrideSilent.setEnabled(false);
                checkboxForceSpeaker.setEnabled(false);
                reraise.setEnabled(false);
            }
        }
        reraise.setText(String.valueOf(alertReraise));
        alertMp3File.setKeyListener(null);
        viewHeader.setText(status);
        setDefaultSnoozeSpinner(defaultSnooze);
        setPreSnoozeSpinner();
        enableAllDayControls();
        setDisabledView();
        showHideSilentModeWarning();


    }
    
    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString("uuid", uuid);
        outState.putString("above", String.valueOf(above));
        outState.putString("audioPath", audioPath);
    }

    @Override
    public String getMenuName() {
        return getString(R.string.title_activity_edit_alert);
    }


    public static DecimalFormat getNumberFormatter(boolean doMgdl) {
        DecimalFormat df = new DecimalFormat("#");
        if (doMgdl) {
            df.setMaximumFractionDigits(0);
            df.setMinimumFractionDigits(0);
        } else {
            df.setMaximumFractionDigits(1);
            df.setMinimumFractionDigits(1);
        }

        return df;
    }

    public static String unitsConvert2Disp(boolean doMgdl, double threshold) {
        DecimalFormat df = getNumberFormatter(doMgdl);
        if (doMgdl)
            return df.format(threshold);

        return df.format(threshold / Constants.MMOLL_TO_MGDL);
    }

    double unitsConvertFromDisp(double threshold) {
        if(doMgdl ) {
            return threshold;
        } else {
            return threshold * Constants.MMOLL_TO_MGDL;
        }
    }

    void enableAllDayControls() {
        boolean allDay = checkboxAllDay.isChecked();
        if(allDay) {
            layoutTimeBetween.setVisibility(View.GONE);
            timeInstructions.setVisibility(View.GONE);
        } else {
            setTimeRanges();
        }
    }
    
    void setDisabledView() {
    	boolean disabled = checkboxDisabled.isChecked();
    	
    	ArrayList<TextView> textViews = new ArrayList<TextView>();
    	textViews.add((TextView) findViewById(R.id.view_alert_text));
    	textViews.add((TextView) findViewById(R.id.view_alert_threshold));
    	textViews.add((TextView) findViewById(R.id.view_alert_default_snooze));
    	textViews.add((TextView) findViewById(R.id.view_alert_mp3_file));
    	textViews.add((TextView) findViewById(R.id.view_alert_time_between));
    	textViews.add((TextView) findViewById(R.id.view_alert_disable));
    	textViews.add((TextView) findViewById(R.id.view_alert_time));
    	textViews.add((TextView) findViewById(R.id.view_alert_override_silent));
    	textViews.add((TextView) findViewById(R.id.view_alert_vibrate));
    	
    	for (TextView tv : textViews) {
    		if(disabled) {
                tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
            	tv.setPaintFlags(tv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }
    	}
    }


    void showHideSilentModeWarning() {
        layoutSilentModeWarning.setVisibility(checkboxOverrideSilent.isChecked() ? View.GONE : View.VISIBLE);
    }

    private boolean verifyThreshold(double threshold, boolean allDay, int startTime, int endTime) {
        List<AlertType> lowAlerts = AlertType.getAll(false);
        List<AlertType> highAlerts = AlertType.getAll(true);

        if(threshold < MIN_ALERT || threshold > MAX_ALERT) {
            Toast.makeText(getApplicationContext(), "threshold has to be between " +unitsConvert2Disp(doMgdl, MIN_ALERT) + " and " + unitsConvert2Disp(doMgdl, MAX_ALERT),Toast.LENGTH_LONG).show();
            return false;
        }
        // We want to make sure that for each threashold there is only one alert. Otherwise, which file should we play.
        for (AlertType lowAlert : lowAlerts) {
            if(lowAlert.threshold == threshold  && overlapping(lowAlert, allDay, startTime, endTime) && lowAlert.active) {
                if(uuid == null || ! uuid.equals(lowAlert.uuid)){ //new alert or not myself
                    Toast.makeText(getApplicationContext(),
                            "Each alert should have it's own threshold. Please choose another threshold.",Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }
        for (AlertType highAlert : highAlerts) {
            if(highAlert.threshold == threshold  && overlapping(highAlert, allDay, startTime, endTime) && highAlert.active) {
                if(uuid == null || ! uuid.equals(highAlert.uuid)){ //new alert or not myself
                    Toast.makeText(getApplicationContext(),
                            "Each alert should have it's own threshold. Please choose another threshold.",Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }

        // high alerts have to be higher than all low alerts...
        if(above) {
            for (AlertType lowAlert : lowAlerts) {
                if(threshold < lowAlert.threshold  && overlapping(lowAlert, allDay, startTime, endTime) && lowAlert.active) {
                    Toast.makeText(getApplicationContext(),
                            "High alert threshold has to be higher than all low alerts. Please choose another threshold.",Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        } else {
            // low alert has to be lower than all high alerts
            for (AlertType highAlert : highAlerts) {
                if(threshold > highAlert.threshold  && overlapping(highAlert, allDay, startTime, endTime) && highAlert.active) {
                    Toast.makeText(getApplicationContext(),
                            "Low alert threshold has to be lower than all high alerts. Please choose another threshold.",Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }

        return true;
    }

    // determines if an alertType is 'at' has a temporal overlap with a new alert with parameters
    // allday, startTime, entTime
    private boolean overlapping(AlertType at, boolean allday, int startTime, int endTime){
        //shortcut: if one is all day, they must overlap
        if(at.all_day || allday) {
            return true;
        }
        int st1 = at.start_time_minutes;
        int st2 = startTime;
        int et1 = at.end_time_minutes;
        int et2 = endTime;

        return  st1 <= st2 && et1 > st2 ||
                st1 <= st2 && (et2 < st2) && et2 > st1 || //2nd timeframe passes midnight
                st2 <= st1 && et2 > st1 ||
                st2 <= st1 && (et1 < st1) && et1 > st2 ||
                (et1 < st1 && et2 < st2); //both timeframes pass midnight -> overlap at least at midnight
    }

    private double parseDouble(String str) {
        try {
            final DecimalFormat numberFormatter = getNumberFormatter(doMgdl);
            return numberFormatter.parse(str).doubleValue();
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Invalid number", nfe);
            Toast.makeText(getApplicationContext(), "Invalid number: " + str, Toast.LENGTH_LONG).show();
            return Double.NaN;
        } catch (ParseException e) {
            Log.e(TAG, "Invalid number", e);
            Toast.makeText(getApplicationContext(), "Invalid number: " + str, Toast.LENGTH_LONG).show();
            return Double.NaN;
        }
    }

    // rarely the parseInt can fail, this adds a failsafe in that case
    private int safeGetDefaultSnooze()
    {
        int defaultSnooze;
        try {
            defaultSnooze = parseInt(editSnooze.getText().toString());
        } catch (NullPointerException e) {
            Log.wtf(TAG,"Got null pointer exception unboxing parseInt: ",e);
            defaultSnooze = SnoozeActivity.getDefaultSnooze(above);
        }
        return defaultSnooze;
    }

    private Integer parseInt(String str) {
        try {
            return Integer.parseInt(str);
        }
        catch (NumberFormatException nfe) {
            Log.e(TAG, "Invalid number", nfe);
            Toast.makeText(getApplicationContext(), "Invalid number: " + str, Toast.LENGTH_LONG).show();
            return null;
        }
    }

    public void addListenerOnButtons() {
      
        buttonSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Check that values are ok.
                double threshold = parseDouble(alertThreshold.getText().toString());
                if(Double.isNaN(threshold))
                    return;

                threshold = unitsConvertFromDisp(threshold);

                int alertReraise = 1;
                Integer alterReraiseInt = parseInt(reraise.getText().toString());
                if(alterReraiseInt ==null)
                    return;
                alertReraise = alterReraiseInt;
                int defaultSnooze = safeGetDefaultSnooze();

                if(alertReraise < 1) {
                    Toast.makeText(getApplicationContext(), "Reraise Value must be 1 minute or greater", Toast.LENGTH_LONG).show();
                    return;
                } else if (alertReraise >= defaultSnooze) {
                    Toast.makeText(getApplicationContext(), "Reraise Value must be less than snooze length", Toast.LENGTH_LONG).show();
                    return;
                }

                int timeStart = AlertType.toTime(startHour, startMinute);
                int timeEnd = AlertType.toTime(endHour, endMinute);

                boolean allDay = checkboxAllDay.isChecked();
                // if 23:59 was set, we increase it to 24:00
                if(timeStart == AlertType.toTime(23, 59)) {
                    timeStart++;
                }
                if(timeEnd == AlertType.toTime(23, 59)) {
                    timeEnd++;
                }
                if(timeStart == AlertType.toTime(0, 0) &&
                   timeEnd == AlertType.toTime(24, 0)) {
                    allDay = true;
                }
                if (timeStart == timeEnd && (allDay==false)) {
                    Toast.makeText(getApplicationContext(), "start time and end time of alert can not be equal",Toast.LENGTH_LONG).show();
                    return;
                }
                boolean disabled = checkboxDisabled.isChecked();
                if(!disabled && !verifyThreshold(threshold, allDay, timeStart, timeEnd)) {
                    return;
                }
                boolean vibrate = checkboxVibrate.isChecked();
                
                boolean overrideSilentMode = checkboxOverrideSilent.isChecked();
                boolean forceSpeaker = checkboxForceSpeaker.isChecked();

                String mp3_file = audioPath;
                if (uuid != null) {
                    AlertType.update_alert(uuid, alertText.getText().toString(), above, threshold, allDay, alertReraise, mp3_file, timeStart, timeEnd, overrideSilentMode, forceSpeaker, defaultSnooze, vibrate, !disabled);
                }  else {
                    AlertType.add_alert(null, alertText.getText().toString(), above, threshold, allDay, alertReraise, mp3_file, timeStart, timeEnd, overrideSilentMode, forceSpeaker, defaultSnooze, vibrate, !disabled);
                }

                startWatchUpdaterService(mContext, WatchUpdaterService.ACTION_SYNC_ALERTTYPE, TAG);
                Intent returnIntent = new Intent();
                setResult(RESULT_OK,returnIntent);
                BlueJayEntry.startWithRefreshIfEnabled();
                finish();
            }

        });

        buttonRemove.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


                if (uuid == null) {
                    Log.wtf(TAG, "Error remove pressed, while we were adding an alert");
                }  else {
                    AlertType.remove_alert(uuid);
                    startWatchUpdaterService(mContext, WatchUpdaterService.ACTION_SYNC_ALERTTYPE, TAG);
                }
                Intent returnIntent = new Intent();
                setResult(RESULT_OK,returnIntent);
                finish();
            }

        });

        buttonTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                testAlert();
            }

        });

        buttonalertMp3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("What type of Alert?")
                        .setItems(R.array.alertType, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select tone for Alerts:");
                                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
                                    startActivityForResult(intent, 999);
                                } else if (which == 1) {
                                    if (checkPermissions()) {
                                      chooseFile();
                                    }
                                } else {
                                    // Xdrip default was chossen, we live the file name as empty.
                                    audioPath = "";
                                    alertMp3File.setText(shortPath(audioPath));
                                }
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
       }); //- See more at: http://blog.kerul.net/2011/12/pick-file-using-intentactiongetcontent.html#sthash.c8xtIr1Y.dpuf

        checkboxAllDay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            //          @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableAllDayControls();
            }
        });

        checkboxDisabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            //          @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setDisabledView();
            }
        });

        
        checkboxOverrideSilent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            //          @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showHideSilentModeWarning();
            }
        });

        //Register Liseners to modify start and end time

        View.OnClickListener startTimeListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                TimePickerDialog mTimePicker = new TimePickerDialog(mContext, AlertDialog.THEME_HOLO_DARK, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        startHour = selectedHour;
                        startMinute = selectedMinute;
                        setTimeRanges();
                    }
                }, startHour, startMinute, DateFormat.is24HourFormat(mContext));
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();

            }
        } ;

        View.OnClickListener endTimeListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                TimePickerDialog mTimePicker = new TimePickerDialog(mContext, AlertDialog.THEME_HOLO_DARK, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        endHour = selectedHour;
                        endMinute = selectedMinute;
                        setTimeRanges();
                    }
                }, endHour, endMinute, DateFormat.is24HourFormat(mContext));
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();

            }
        };

        viewTimeStart.setOnClickListener(startTimeListener);
        timeInstructionsStart.setOnClickListener(startTimeListener);
        viewTimeEnd.setOnClickListener(endTimeListener);
        timeInstructionsEnd.setOnClickListener(endTimeListener);

    }

    private void chooseFile()
    {
        final Intent fileIntent = new Intent();
        fileIntent.setType("audio/mpeg3");
        fileIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(fileIntent, "Select File for Alert"), REQUEST_CODE_CHOOSE_FILE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                chooseFile(); // must be the only functionality which calls for permission
            } else {
                JoH.static_toast_long(this, "Cannot choose file without storage permission");
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                audioPath = uri.toString();
                alertMp3File.setText(shortPath(audioPath));
            } else {
                if (requestCode == REQUEST_CODE_CHOOSE_FILE) {
                    Uri selectedImageUri = data.getData();

                    // Todo this code is very flacky. Probably need a much better understanding of how the different programs
                    // select the file names. We might also have to
                    // - See more at: http://blog.kerul.net/2011/12/pick-file-using-intentactiongetcontent.html#sthash.c8xtIr1Y.cx7s9nxH.dpuf

                    //MEDIA GALLERY
                    String selectedAudioPath = getPath(selectedImageUri);
                    if (selectedAudioPath == null) {
                        //OI FILE Manager
                        selectedAudioPath = selectedImageUri.getPath();
                    }
                    audioPath = selectedAudioPath;
                    alertMp3File.setText(shortPath(audioPath));
                }
            }
        }
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if(cursor!=null)
        {
            //HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            //THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            int column_index;
            try {
                column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            } catch ( IllegalArgumentException e) {
                Log.e(TAG, "cursor.getColumnIndexOrThrow failed", e);
                return null;
            }
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }   else {
            return null;
        }
    }

    static public String timeFormatString(Context context, int hour, int minute) {
        SimpleDateFormat timeFormat24 = new SimpleDateFormat("HH:mm");
        String selected = hour+":" + ((minute<10)?"0":"") + minute;
        if (!DateFormat.is24HourFormat(context)) {
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

    public void setTimeRanges() {
        timeInstructions.setVisibility(View.VISIBLE);
        layoutTimeBetween.setVisibility(View.VISIBLE);
        viewTimeStart.setText(timeFormatString(mContext, startHour, startMinute));
        viewTimeEnd.setText(timeFormatString(mContext, endHour, endMinute));
    }

    public static boolean isPathRingtone(Context context, String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        Ringtone ringtone = RingtoneManager.getRingtone(context, Uri.parse(path));
        return ringtone != null;
    }

    public String shortPath(String path) {
        if(isPathRingtone(mContext, path)) {
            Ringtone ringtone = RingtoneManager.getRingtone(mContext, Uri.parse(path));
            // Just verified that the ringtone exists... not checking for null
            return ringtone.getTitle(mContext);
        }
        if(path == null) {
            return "";
        }
        if(path.length() == 0) {
            return "xDrip Default";
        }
        String[] segments = path.split("/");
        if (segments.length > 1) {
            return segments[segments.length - 1];
        }
        return path;
    }
    public void setDefaultSnoozeSpinner(int defaultSnooze) {
        editSnooze.setText(String.valueOf(defaultSnooze));
        editSnooze.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View mView, MotionEvent mMotionEvent) {
                if (mMotionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    final Dialog d = new Dialog(mContext);
                    d.setTitle("Default Snooze");
                    d.setContentView(R.layout.snooze_picker);
                    Button b1 = (Button) d.findViewById(R.id.button1);
                    Button b2 = (Button) d.findViewById(R.id.button2);

                    final NumberPicker snoozeValue = (NumberPicker) d.findViewById(R.id.numberPicker1);

                    int defaultSnooze = safeGetDefaultSnooze();

                    SnoozeActivity.SetSnoozePickerValues(snoozeValue, above, defaultSnooze);
                    b1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int defaultSnooze = SnoozeActivity.getTimeFromSnoozeValue(snoozeValue.getValue());
                            editSnooze.setText(String.valueOf(defaultSnooze));

                            d.dismiss();
                        }
                    });
                    b2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            d.dismiss();
                        }
                    });
                    d.show();
                }
                return false;

            }});

    }

    public void setPreSnoozeSpinner() {


        buttonPreSnooze.setOnClickListener(new View.OnClickListener() {
            @Override
            //public boolean onTouch(View mView, MotionEvent mMotionEvent) {
            public void onClick(View v) {
                final Dialog d = new Dialog(mContext);
                d.setTitle("Snooze this alert...");
                d.setContentView(R.layout.snooze_picker);
                Button b1 = (Button) d.findViewById(R.id.button1);
                Button b2 = (Button) d.findViewById(R.id.button2);
                b1.setText("pre-Snooze");

                final NumberPicker snoozeValue = (NumberPicker) d.findViewById(R.id.numberPicker1);

                int defaultSnooze = safeGetDefaultSnooze();
                SnoozeActivity.SetSnoozePickerValues(snoozeValue, above, defaultSnooze);
                b1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int intValue = SnoozeActivity.getTimeFromSnoozeValue(snoozeValue.getValue());
                        AlertPlayer.getPlayer().PreSnooze(getApplicationContext(), uuid, intValue);
                        d.dismiss();
                    }
                });
                b2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d.dismiss();
                    }
                });
                d.show();
            }});

    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                final Activity activity = this;
                JoH.show_ok_dialog(activity, gs(R.string.please_allow_permission), gs(R.string.need_storage_permission_to_access_all_ringtones), new Runnable() {
                    @Override
                    public void run() {
                        ActivityCompat.requestPermissions(activity,
                                new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE);
                    }
                });
                return false;
            }
        }
        return true;
    }


    public void testAlert() {
        // Check that values are ok.
        double threshold = parseDouble(alertThreshold.getText().toString());
        if(Double.isNaN(threshold)) {
          JoH.static_toast_long("Threshold number is not valid");
            return;
        }

        threshold = unitsConvertFromDisp(threshold);

        int timeStart = AlertType.toTime(startHour, startMinute);
        int timeEnd = AlertType.toTime(endHour, endMinute);

        boolean allDay = checkboxAllDay.isChecked();
        // if 23:59 was set, we increase it to 24:00
        if(timeStart == AlertType.toTime(23, 59)) {
            timeStart++;
        }
        if(timeEnd == AlertType.toTime(23, 59)) {
            timeEnd++;
        }
        if(timeStart == AlertType.toTime(0, 0) &&
                timeEnd == AlertType.toTime(24, 0)) {
            allDay = true;
        }
        if (timeStart == timeEnd && (!allDay)) {
            Toast.makeText(getApplicationContext(), "start time and end time of alert can not be equal",Toast.LENGTH_LONG).show();
            return;
        }
        if(!verifyThreshold(threshold, allDay, timeStart, timeEnd)) {
            return;
        }
        boolean vibrate = checkboxVibrate.isChecked();
        boolean overrideSilentMode = checkboxOverrideSilent.isChecked();
        boolean forceSpeaker = checkboxForceSpeaker.isChecked();
        String mp3_file = audioPath;
        try {
            int defaultSnooze = safeGetDefaultSnooze();

            if (Pref.getBooleanDefaultFalse("start_snoozed"))  {
                JoH.static_toast_long("Start Snoozed setting means alert would normally start silent");
            } else if (Pref.getStringDefaultBlank("bg_alert_profile").equals("ascending")) {
                JoH.static_toast_long("Ascending Volume Profile means it will start silent");
            } else if (Pref.getStringDefaultBlank("bg_alert_profile").equals("Silent")) {
                JoH.static_toast_long("Volume Profile is set to silent!");
            }

            AlertType.testAlert(alertText.getText().toString(), above, threshold, allDay, 1, mp3_file, timeStart, timeEnd, overrideSilentMode, forceSpeaker, defaultSnooze, vibrate, mContext);
        } catch (NullPointerException e) {
            JoH.static_toast_long("Snooze value is not a number - cannot test");
        }
    }
}
