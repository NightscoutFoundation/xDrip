package com.eveningoutpost.dexdrip;

// TODO Pagenate for upcoming
// TODO stop alert notification for swiped alerts


import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.eveningoutpost.dexdrip.Home.SHOWCASE_REMINDER3;
import static com.eveningoutpost.dexdrip.models.JoH.dateTimeText;
import static com.eveningoutpost.dexdrip.models.JoH.hourMinuteString;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalarNatural;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static lecho.lib.hellocharts.animation.ChartDataAnimator.DEFAULT_ANIMATION_DURATION;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Reminder;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.JamorhamShowcaseDrawer;
import com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.ShotStateStore;
import com.eveningoutpost.dexdrip.utilitymodels.SpeechUtil;
import com.eveningoutpost.dexdrip.profileeditor.DatePickerFragment;
import com.eveningoutpost.dexdrip.profileeditor.ProfileAdapter;
import com.eveningoutpost.dexdrip.profileeditor.TimePickerFragment;
import com.eveningoutpost.dexdrip.receiver.ReminderReceiver;
import com.eveningoutpost.dexdrip.utils.HomeWifi;
import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.rits.cloning.Cloner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.val;

// TODO swipe right reschedule options
// TODO wake up option

public class Reminders extends ActivityWithRecycler implements SensorEventListener {

    private static final String TAG = "Reminders";
    private static final int NOTIFICATION_ID = 765;
    private static final String REMINDER_WAKEUP = "REMINDER_WAKEUP";
    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 139;
    private static final int REQUEST_CODE_CHOOSE_FILE = 2;
    private static final int REQUEST_CODE_CHOOSE_RINGTONE = 55;

    private static final long MEGA_PRIORITY = 1000000;


    private static final boolean d = true;

    public final List<Reminder> reminders = new ArrayList<>();
    public static final String REMINDER_ACTION = "reminder";

    private AlertDialog dialog;
    private EditText reminderDaysEdt;
    private View dialogView;
    private CardView floatingsnooze;
    private TextView floaterText;
    private TextView swipePromptText;
    private boolean floaterHidden = true;
    private String selectedSound;

    private Reminder last_undo, last_swiped;
    private int last_undo_pos;

    private long default_snooze = Constants.MINUTE_IN_MS * 30;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private static volatile boolean proximity = true; // default to near
    private int proximity_events = 0;
    private int highlighted = 0;

    MenuItem remindersDisabledAtNightMenuItem;
    MenuItem remindersDisabledMenuItem;
    MenuItem remindersAdvancedMenuItem;
    MenuItem remindersRestartTomorrowMenuItem;
    MenuItem remindersCancelByDefaultMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);
        setTheme(R.style.AppThemeToolBarLite); // for toolbar mode

        //setSupportActionBar((Toolbar)findViewById(R.id.reminder_toolbar));
        JoH.fixActionBar(this);

        setTitle(getString(R.string.xdrip_reminders));
        // TODO subtitle with summary
        recyclerView = (RecyclerView) findViewById(R.id.reminder_recycler);
        floatingsnooze = (CardView) findViewById(R.id.floatingsnooze);
        floaterText = (TextView) findViewById(R.id.floaterText);
        swipePromptText = (TextView) findViewById(R.id.reminders_info);
        floatingsnooze.setVisibility(View.GONE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        mAdapter = new RecyclerAdapter(this, reminders);

        if (selectedSound == null) {
            if (PersistentStore.getString("reminders-last-sound").length() > 5)
                selectedSound = PersistentStore.getString("reminders-last-sound");
        }

        reloadList();

        postOnCreate();

        final Bundle bundle = getIntent().getExtras();
        processIncomingBundle(bundle);


        if (reminders.size() == 0) {
            //JoH.static_toast_long("No reminders yet, add one!"); // replace with showcase?
            showcase(this, Home.SHOWCASE_REMINDER1);
        }
    }


    @Override
    public void onNewIntent(Intent intent) {
        final Bundle bundle = intent.getExtras();

        processIncomingBundle(bundle);
    }

    public void onRemindNightClick(MenuItem v) {
        invertPreferenceBoolean(Reminder.REMINDERS_NIGHT_DISABLED);
        updateMenuCheckboxes();
    }

    public void onAllDisabledClick(MenuItem v) {
        invertPreferenceBoolean(Reminder.REMINDERS_ALL_DISABLED);
        updateMenuCheckboxes();
    }

    public void onCancelByDefaultClick(MenuItem v) {
        invertPreferenceBoolean(Reminder.REMINDERS_CANCEL_DEFAULT);
        updateMenuCheckboxes();
    }

    public void onRestartTomorrowClick(MenuItem v) {
        invertPreferenceBoolean(Reminder.REMINDERS_RESTART_TOMORROW);
        updateMenuCheckboxes();
    }

    public void onRemindersAdvancedClick(MenuItem v) {
        invertPreferenceBoolean(Reminder.REMINDERS_ADVANCED_MODE);
        updateMenuCheckboxes();
    }

    public void onRemindersHomeNetworkClick(MenuItem item) {
        HomeWifi.ask(this);
    }


    private void invertPreferenceBoolean(String pref) {
        Pref.setBoolean(pref, !Pref.getBooleanDefaultFalse(pref));
    }

    private void updateMenuCheckboxes() {
        remindersAdvancedMenuItem.setChecked(Pref.getBooleanDefaultFalse(Reminder.REMINDERS_ADVANCED_MODE));
        remindersDisabledMenuItem.setChecked(Pref.getBooleanDefaultFalse(Reminder.REMINDERS_ALL_DISABLED));
        remindersRestartTomorrowMenuItem.setChecked(Pref.getBooleanDefaultFalse(Reminder.REMINDERS_RESTART_TOMORROW));
        remindersDisabledAtNightMenuItem.setChecked(Pref.getBooleanDefaultFalse(Reminder.REMINDERS_NIGHT_DISABLED));
        remindersRestartTomorrowMenuItem.setEnabled(remindersDisabledMenuItem.isChecked());
        remindersCancelByDefaultMenuItem.setChecked(Pref.getBooleanDefaultFalse(Reminder.REMINDERS_CANCEL_DEFAULT));
    }

    private void handleSwipePromptVisibility() {
        if (reminders.size() > 0) {
            swipePromptText.setVisibility(View.VISIBLE);
        } else {
            swipePromptText.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_reminders, menu);
        remindersDisabledAtNightMenuItem = menu.findItem(R.id.reminders_atnight);
        remindersDisabledMenuItem = menu.findItem(R.id.reminders_disabled);
        remindersRestartTomorrowMenuItem = menu.findItem(R.id.reminders_restart);
        remindersAdvancedMenuItem = menu.findItem(R.id.reminders_advanced);
        remindersCancelByDefaultMenuItem = menu.findItem(R.id.reminders_cancel_default);

        updateMenuCheckboxes();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        PowerManager.WakeLock wl = JoH.getWakeLock("reminders-onresume", 15000);
        super.onResume();
        if (JoH.ratelimit("proximity-reset", 5)) {
            Log.d(TAG, "Initializing proximity as true");
            proximity = true; // default to near
        }
        proximity_events = 0;
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        highlighted = 0;
        reloadList();
        handleSwipePromptVisibility();
        // intentionally do not release wakelock
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        final SensorEventListener activity = this;

        JoH.runOnUiThreadDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Unregistering proximity sensor listener");
                try {
                    mSensorManager.unregisterListener(activity);
                } catch (Exception e) {
                    Log.d(TAG, "Error unregistering proximity listener: " + e);
                }
            }
        }, 10000);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            proximity_events++;
            if (proximity_events < 20)
                Log.d(TAG, "Sensor: " + event.values[0] + " " + mProximity.getMaximumRange());
            if (event.values[0] <= (Math.min(mProximity.getMaximumRange() / 2, 10))) {
                proximity = true; // near
            } else {
                proximity = false; // far
            }
            if (proximity_events < 20) Log.d(TAG, "Proxmity set to: " + proximity);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private void reloadList() {
        reminders.clear();
        reminders.addAll(Reminder.getAllReminders());
        mAdapter.notifyDataSetChanged();
    }


    // View holder
    public class MyViewHolder extends ActivityWithRecycler.MyViewHolder {

        RelativeLayout wholeBlock;
        EditText title_text;
        TextView next_due_text, small_text;

        public MyViewHolder(View view) {
            super(view);
            small_text = (TextView) view.findViewById(R.id.reminder_small_top_text);
            title_text = (EditText) view.findViewById(R.id.reminder_title_text);
            next_due_text = (TextView) view.findViewById(R.id.reminders_next_due_text);
            wholeBlock = (RelativeLayout) view.findViewById(R.id.reminder_whole_row_block);
        }
    }

    ///////////// Adapter
    public class RecyclerAdapter extends ActivityWithRecycler.RecyclerAdapater {

        public RecyclerAdapter(Context ctx, List<Reminder> reminderList) {
            Log.d(TAG, "New adapter, size: " + reminderList.size());
        }

        @Override
        public int getItemCount() {
            return reminders.size();
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.reminder_list_row, parent, false);

            final MyViewHolder holder = new MyViewHolder(itemView);

            holder.title_text.setOnEditorActionListener(new EditText.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    boolean handled = false;
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        final int pos = holder.getAdapterPosition();
                        reminders.get(pos).updateTitle(holder.title_text.getText().toString());
                        notifyItemChanged(pos);
                    }
                    return handled;
                }
            });

            holder.title_text.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showReminderDialog(null, reminders.get(holder.getAdapterPosition()), 0);
                    return false;
                }
            });

            holder.next_due_text.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showReminderDialog(null, reminders.get(holder.getAdapterPosition()), 0);
                    return false;
                }
            });

            holder.small_text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    askTime(holder.getAdapterPosition());
                }
            });

            return holder;
        }

        @Override
        public void onBindViewHolder(final ActivityWithRecycler.MyViewHolder base_holder, @SuppressLint("RecyclerView") int position) {
            final MyViewHolder holder = (MyViewHolder) base_holder; // cast it
            final Reminder reminder_item = reminders.get(position);
            holder.position = position; // should we use getadapterposition instead?
            holder.title_text.setText(reminder_item.getTitle());


            String lastDetails = "";
            if (reminder_item.fired_times > 0) {
                if (msSince(reminder_item.last_fired) < Constants.DAY_IN_MS) {
                    lastDetails += xdrip.getAppContext().getString(R.string.last) + " " + JoH.hourMinuteString(reminder_item.last_fired);
                } else {
                    lastDetails += xdrip.getAppContext().getString(R.string.last) + " " + dateTimeText(reminder_item.last_fired);
                }
                lastDetails += " (x" + reminder_item.fired_times + ") " + xdrip.getAppContext().getString(R.string.next);
            }
            String nextDetails = "";
            if (reminder_item.enabled) {
                if (Math.abs(msSince(reminder_item.next_due)) < Constants.DAY_IN_MS) {
                    nextDetails += JoH.hourMinuteString(reminder_item.next_due);
                } else {
                    final String dstring = dateTimeText(reminder_item.next_due);
                    nextDetails += dstring.substring(0, dstring.length() - 3); // remove seconds
                }
            }
            holder.small_text.setText(lastDetails + " " + nextDetails);

            final long duein = Math.max(JoH.msTill(reminder_item.next_due), JoH.msTill(reminder_item.snoozed_till));
            holder.wholeBlock.setBackgroundColor(Color.TRANSPARENT);
            String firstpart = "";
            if (reminder_item.enabled) {
                holder.wholeBlock.setAlpha(1.0f);
                if ((reminder_item.snoozed_till > tsl()) && (reminder_item.next_due < tsl())) {
                    firstpart = xdrip.getAppContext().getString(R.string.snoozed_for) +" " + JoH.niceTimeTill(reminder_item.snoozed_till);
                } else {
                    if (duein >= 0) {
                        String natural_due = JoH.niceTimeScalarNatural(duein);
                        if (natural_due.matches("^[0-9]+.*")) {
                            natural_due = xdrip.getAppContext().getString(R.string.in) + " " + natural_due;
                        }
                        firstpart = xdrip.getAppContext().getString(R.string.due) +" " + natural_due;
                    } else {
                        firstpart = xdrip.getAppContext().getString(R.string.due_uppercase) + " " + JoH.hourMinuteString(reminder_item.next_due);
                        holder.wholeBlock.setBackgroundColor(Color.parseColor("#660066"));
                        highlighted++;
                    }
                }
            } else {
                firstpart = xdrip.getAppContext().getString(R.string.completed) + " ";
                holder.wholeBlock.setAlpha(0.3f);
            }
            holder.next_due_text.setText(firstpart + (reminder_item.repeating ? ", " + xdrip.getAppContext().getString(R.string.repeats_every) + " " + JoH.niceTimeScalarRedux(reminder_item.period) : ""));

        }


        @Override
        public boolean onItemMove(int source, int destination) {
            if (JoH.ratelimit("on item move", 5)) {
                showReminderDialog(null, reminders.get(source), 0);
            }
            return true;
        }

        @Override
        public void onItemDismiss(int position) {
            // snooze + save in undo + save details for pushbuttons
            final Reminder remind = reminders.get(position);
            final Cloner cloner = new Cloner();
            //cloner.setDumpClonedClasses(true);
            //cloner.dontClone(
            //       android.graphics.DashPathEffect.class);
            last_undo = cloner.deepClone(remind);
            if (SimpleItemTouchHelperCallback.last_swipe_direction == ItemTouchHelper.LEFT) {

                // swipe left == snooze
                snoozeReminder(remind, remind.last_snoozed_for > 0 ? remind.last_snoozed_for : default_snooze);

                last_swiped = remind;
                //last_undo_pos = position;
                reminders.remove(position);
                notifyItemRemoved(position);
                cancelAlert();
                showSnoozeFloater();
                reinjectDelayed(remind);
            } else {
                // swipe right == reschedule
                cancelAlert();
                last_swiped = remind;
                if (!rescheduleOrCancelReminder(remind)) {
                    reinjectDelayed(remind);
                }
                // as we swiped off in UI we always must reinject
                reminders.remove(position);
                notifyItemRemoved(position);
            }
        }
    }

    private void reinjectDelayed(Reminder remind) {
        JoH.runOnUiThreadDelayed(() -> reinject(remind), 500);
    }

    private void snoozeReminder(Reminder remind, long snooze_time) {
        if (remind == null) return;
        remind.last_snoozed_for = snooze_time;
        if (remind.isDue()) {
            remind.snoozed_till = tsl() + snooze_time;
            setFloaterText(remind.getTitle() + " " + xdrip.getAppContext().getString(R.string.snoozed_for) + " " + JoH.niceTimeScalar(snooze_time));
        } else {
            remind.snoozed_till = remind.next_due + snooze_time;
            setFloaterText(remind.getTitle() + " " + xdrip.getAppContext().getString(R.string.postponed_for) + " " + JoH.niceTimeScalar(snooze_time));
        }
        remind.save();
    }

    private void updateFloaterForReschedule(final Reminder remind) {
        setFloaterText(remind.getTitle() + " " + xdrip.getAppContext().getString(R.string.next_in) + " " + JoH.niceTimeTill(remind.next_due));
        reinjectDelayed(remind);
        showSnoozeFloater();

    }

    private void rescheduleNextOld(final Reminder remind) {
        remind.schedule_next();
        updateFloaterForReschedule(remind);
    }

    private void rescheduleNextNew(final Reminder remind, long when) {
        remind.schedule_next(when);
        updateFloaterForReschedule(remind);
    }


    private boolean rescheduleOrCancelReminder(final Reminder remind) {
        if (remind.repeating || !remind.isDue()) {
            if (remind.isOverdue()) {
               askWhenToReschedule(remind);
                return false;
            } else {
               rescheduleNextOld(remind);
            }
        } else if (!remind.repeating) {
            setFloaterText(remind.getTitle() + " " + xdrip.getAppContext().getString(R.string.completed));
            remind.enabled = false;
            remind.save();
        }
        return true;
    }

    private void askWhenToReschedule(final Reminder remind) {
        val now = tsl();
        val oldt = remind.getPotentialNextSchedule();
        val newt = now + remind.period;
        val choice = String.format("%s:    %s  @  %s\n\nor\n\n%s:   %s  @  %s",
                getString(R.string.old), niceTimeScalarNatural(-msSince(oldt)),hourMinuteString(oldt),
                getString(R.string.neww), niceTimeScalarNatural(-msSince(newt)),hourMinuteString(newt));

        val builder = new AlertDialog.Builder(this)
                .setTitle(R.string.reschedule_with_new_timing)
                .setMessage(choice);

        builder.setPositiveButton(getString(R.string.old), (dialog, which) -> rescheduleNextOld(remind));
        builder.setNegativeButton(getString(R.string.neww), (dialog, which) -> rescheduleNextNew(remind, newt));

        val dialog = builder.create();
        try {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            //
        }
        dialog.show();
    }


    //////////// Button pushes
    public void newReminder(View v) {
        showReminderDialog(v, null, 0);
    }

    private synchronized void playSelectedSound() {
        JoH.playSoundUri((selectedSound != null) ? selectedSound : JoH.getResourceURI(R.raw.reminder_default_notification));
    }

    public void chooseReminderSound(View v) {
        playSelectedSound();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_new_sound_source)
                .setOnDismissListener(dialog -> {
                        JoH.stopSoundUri();
                })
                .setItems(R.array.reminderAlertType, (dialog, which) -> {
                    JoH.stopSoundUri();
                    if (which == 0) {
                        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, xdrip.getAppContext().getString(R.string.select_tone_alert));
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
                        startActivityForResult(intent, REQUEST_CODE_CHOOSE_RINGTONE);
                    } else if (which == 1) {
                        if (checkPermissions()) {
                            chooseFile();
                        }
                    } else {
                        JoH.static_toast_long(xdrip.getAppContext().getString(R.string.using_default_sound));
                        selectedSound = null;
                        playSelectedSound();
                        PersistentStore.setString("reminders-last-sound", "");
                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void chooseFile() {
        final Intent fileIntent = new Intent();
        fileIntent.setType("audio/*");
        fileIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        startActivityForResult(Intent.createChooser(fileIntent, xdrip.getAppContext().getString(R.string.select_file_reminder_sound)), REQUEST_CODE_CHOOSE_FILE);
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                final Activity activity = this;
                JoH.show_ok_dialog(activity, xdrip.getAppContext().getString(R.string.Please_Allow_Permission), xdrip.getAppContext().getString(R.string.need_storage_permission), new Runnable() {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                chooseFile(); // must be the only functionality which calls for permission
            } else {
                JoH.static_toast_long(this, xdrip.getAppContext().getString(R.string.cannot_chose_file));
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE_RINGTONE) {
                final Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri != null) {
                    //JoH.static_toast_long(uri.toString());
                    selectedSound = uri.toString();
                    PersistentStore.setString("reminders-last-sound", selectedSound);
                }
            } else {
                if (requestCode == REQUEST_CODE_CHOOSE_FILE) {
                    final Uri selectedFileUri = data.getData();
                    //JoH.static_toast_long(selectedFileUri.toString());
                    try {
                        getContentResolver().takePersistableUriPermission(selectedFileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        selectedSound = selectedFileUri.toString();
                        PersistentStore.setString("reminders-last-sound", selectedSound);
                        // play it?
                    } catch (Exception e) {
                        JoH.static_toast_long(xdrip.getAppContext().getString(R.string.problem_with_sound) + " " + e.getMessage());
                    }
                }
            }
        }
    }


    public void snoozeAdjust(View v) {
        final String button_text = ((Button) v).getTag().toString(); //changed String to Tag, to make the texts translateable
        Log.d(TAG, "Snooze adjust button: " + button_text);
        long multiplier = Constants.MINUTE_IN_MS;
        final String[] button_textA = button_text.split(" ");
        switch (button_textA[1].toLowerCase()) {
            case "hour":
            case "hours":
                multiplier = Constants.HOUR_IN_MS;
                break;

            case "day":
            case "days":
                multiplier = Constants.DAY_IN_MS;
                break;

            case "week":
            case "weeks":
                multiplier = Constants.WEEK_IN_MS;
                break;
        }
        final long snooze_adjust = Integer.parseInt(button_textA[0]) * multiplier;
        Log.d(TAG, "Snoozed adjust button result: " + snooze_adjust);
        dismissItem(last_swiped);
        snoozeReminder(last_swiped, snooze_adjust);
        reinject(last_swiped);
    }

    public void reminderUpButton(View v) {
        try {
            reminderDaysEdt.setText(Integer.toString(Integer.parseInt(reminderDaysEdt.getText().toString()) + 1));
        } catch (NumberFormatException e) {
            JoH.static_toast_short(xdrip.getAppContext().getString(R.string.invalid_number));
        }
    }

    public void reminderDownButton(View v) {
        try {
            reminderDaysEdt.setText(Integer.toString(Integer.parseInt(reminderDaysEdt.getText().toString()) - 1));
        } catch (NumberFormatException e) {
            JoH.static_toast_short(xdrip.getAppContext().getString(R.string.invalid_number));
        }
        if (reminderDaysEdt.getText().toString().equals("0")) {
            reminderDaysEdt.setText("1");
        }
    }

    public synchronized void reminderTrashButton(View v) {
        if (last_undo != null) {
            dismissDoppelgangerItem(last_undo);
            JoH.static_toast_long(xdrip.getAppContext().getString(R.string.toast_deleted) + " " + last_undo.title);
            last_undo.delete();
            last_undo = null;
            hideSnoozeFloater();
        }
    }

    public void hideFloater(View v) {
        hideSnoozeFloater();
        hideKeyboard(v);
        reloadList();
    }

    public synchronized void undoFromFloater(View v) {
        if (last_undo != null) {
            dismissDoppelgangerItem(last_undo);
            reinject(last_undo);
            last_undo = null;
            hideSnoozeFloater();
        } else {
            JoH.static_toast_short(xdrip.getAppContext().getString(R.string.nothing_to_undo));
        }
    }

    private synchronized void dismissItem(Reminder reminder) {
        synchronized (reminders) {
            try {
                mAdapter.notifyItemRemoved(reminders.indexOf(reminder));
                reminders.remove(reminder);
            } catch (Exception e) {
                Log.d(TAG, "Dismiss item: " + e);
            }
        }
    }

    private synchronized void dismissDoppelgangerItem(Reminder reminder) {
        synchronized (reminders) {
            try {
                for (int i = 0; i < reminders.size(); i++) {
                    if (reminders.get(i).getId().equals(reminder.getId())) {
                        mAdapter.notifyItemRemoved(i);
                        reminders.remove(i);
                        break;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "DismissDoppelganger item: " + e);
            }
        }
    }

    private synchronized void freshen(Reminder reminder) {
        dismissItem(reminder);
        reinject(reminder);
    }

    private synchronized void reinject(Reminder reminder) {
        if (reminder == null) return;
        dismissDoppelgangerItem(reminder);
        final int i = reinjectionPosition(reminder);
        // TODO lock?
        Log.d(TAG, "child Reinjection position: " + i);
        reminders.add(i, reminder);
        mAdapter.notifyItemInserted(i);
        final AtomicBoolean corrected = new AtomicBoolean();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrollStateChanged(RecyclerView rv, int state) {
                if (state == RecyclerView.SCROLL_STATE_IDLE) {
                    rv.removeOnScrollListener(this);
                    correctiveScrolling(i, corrected);
                }
            }
        });
        if (highlighted < 2) {
            Log.d(TAG, "Reinjection: scrolling to: " + i);
            recyclerView.smoothScrollToPosition(i);
            JoH.runOnUiThreadDelayed(new Runnable() {
                @Override
                public void run() {
                    // failsafe
                    correctiveScrolling(i, corrected);
                }
            }, 1000);
        } else {
            Log.d(TAG, "Not scrolling due to highlighted: " + highlighted);
        }
    }

    private void correctiveScrolling(int i, AtomicBoolean marker) {
        final float floaterY = floatingsnooze.getTop();
        if (marker.getAndSet(true)) return; // already processed
        int val = 0;
        int ii = 0;
        while (val > -1) {
            View v = recyclerView.getChildAt(ii);
            if (v != null) {
                val = recyclerView.getChildAdapterPosition(v);
                if (val == i) {
                    final float lowest_point = v.getY() + (v.getHeight() * 2);
                    //Log.d(TAG, "Requested Child at position : " + i + " / " + ii + " " + val + " v:" + lowest_point + " vs " + floaterY);
                    if (lowest_point > floaterY) {
                        // is obscured
                        final float difference = lowest_point - floaterY;
                        //  int scrollto = i+((int)difference)+1;
                        Log.d(TAG, "Corrective Scrolling by: " + (int) difference);
                        // TODO wrap with speed adjustment
                        recyclerView.smoothScrollBy(0, (int) difference);
                    }
                    val = -1;
                }
            } else {
                val = -1;
            }
            ii++;
        }
    }

    private synchronized int reinjectionPosition(Reminder reminder) {
        if (reminder == null) return 0;

        synchronized (reminders) {
            int more_than_pos = -1;
            final int max_count = reminders.size() - 1;
            for (int i = max_count; i > 0; i--) {
                //Log.d(TAG, "Checking position: " + i + " " + (reminders.get(i).next_due - reminder.next_due));
                // TODO consider snooze?
                if ((reminder.next_due < reminders.get(i).next_due) && (reminder.enabled == reminders.get(i).enabled)) {
                    more_than_pos = i;
                } else if (((more_than_pos != -1) || (i == max_count)) && ((reminder.next_due >= reminders.get(i).next_due) && (reminder.enabled == reminders.get(i).enabled))) {
                    return (i == max_count) ? max_count + 1 : more_than_pos;
                }
            }
            if (!reminder.enabled) {
                Log.d(TAG, "Returning reinjection max_count+1 due to non enabled");
                return max_count + 1;
            }
        }
        return 0; // is this right?
    }

    // TODO edit mode
    private void showReminderDialog(View myitem, final Reminder reminder, int position) {

        if ((dialog != null) && (dialog.isShowing())) return;

        final Activity activity = this;
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        dialogView = inflater.inflate(R.layout.reminder_new_dialog, null);
        dialogBuilder.setView(dialogView);

        // New Reminder title
        final EditText titleEditText = (EditText) dialogView.findViewById(R.id.reminder_edit_new);
        final EditText alternateEditText = (EditText) dialogView.findViewById(R.id.reminder_edit_alt_title);

        final RadioButton rbday = (RadioButton) dialogView.findViewById(R.id.reminderDayButton);
        final RadioButton rbhour = (RadioButton) dialogView.findViewById(R.id.reminderHourButton);
        final RadioButton rbweek = (RadioButton) dialogView.findViewById(R.id.reminderWeekButton);


        if (reminder == null) {
            rbday.setChecked(PersistentStore.getBoolean("reminders-rbday"));
            rbhour.setChecked(PersistentStore.getBoolean("reminders-rbhour"));
            rbweek.setChecked(PersistentStore.getBoolean("reminders-rbweek"));

        } else {
            rbday.setChecked(reminder.isDaysPeriod());
            rbhour.setChecked(reminder.isHoursPeriod());
            rbweek.setChecked(reminder.isWeeksPeriod());
        }

        // first run if nothing set default to day
        if (!rbday.isChecked() && !rbhour.isChecked() && !rbweek.isChecked()) {
            rbday.setChecked(true);
        }

        titleEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                // hide remove done button if no title present
                try {
                    if (s.length() < 3) {
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
                    } else {
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                    }
                } catch (NullPointerException e) {
                    // dialog not showing yet
                }
            }
        });


        reminderDaysEdt = (EditText) dialogView.findViewById(R.id.reminderRepeatDays);
        final CheckBox repeatingCheckbox = (CheckBox) dialogView.findViewById(R.id.reminderRepeatcheckBox);
        final CheckBox chimeOnlyCheckbox = (CheckBox) dialogView.findViewById(R.id.chimeonlycheckbox);
        final CheckBox alternatingCheckbox = (CheckBox) dialogView.findViewById(R.id.alternatingcheckbox);
        final CheckBox weekendsCheckbox = (CheckBox) dialogView.findViewById(R.id.weekEndsCheckbox);
        final CheckBox weekdaysCheckbox = (CheckBox) dialogView.findViewById(R.id.weekDaysCheckbox);
        final CheckBox megapriorityCheckbox = (CheckBox) dialogView.findViewById(R.id.highPriorityCheckbox);
        final CheckBox homeOnlyCheckbox = (CheckBox) dialogView.findViewById(R.id.homeOnlyCheckbox);
        final CheckBox speechCheckbox = (CheckBox) dialogView.findViewById(R.id.speakCheckbox);
        final CheckBox graphIconCheckbox = (CheckBox) dialogView.findViewById(R.id.GraphIconCheckbox);

        final ImageButton swapButton = (ImageButton) dialogView.findViewById(R.id.reminderSwapButton);


        if (reminder == null) {
            repeatingCheckbox.setChecked(PersistentStore.getLong("reminders-last-repeating") != 2);
            chimeOnlyCheckbox.setChecked(PersistentStore.getLong("reminders-last-chimeonly") == 1);
            alternatingCheckbox.setChecked(PersistentStore.getLong("reminders-last-alternating") == 1);
            homeOnlyCheckbox.setChecked(false); // defaults to false

            try {
                reminderDaysEdt.setText(!PersistentStore.getString("reminders-last-number").equals("") ? Integer.toString(Integer.parseInt(PersistentStore.getString("reminders-last-number"))) : "1");
            } catch (Exception e) {
                //
            }
        } else {
            selectedSound = reminder.sound_uri;
            titleEditText.setText(reminder.title);
            alternateEditText.setText(reminder.getAlternateTitle());
            repeatingCheckbox.setChecked(reminder.repeating);
            chimeOnlyCheckbox.setChecked(reminder.chime_only);
            alternatingCheckbox.setChecked(reminder.alternating);
            megapriorityCheckbox.setChecked(reminder.priority > MEGA_PRIORITY);
            if (!reminder.weekdays && !reminder.weekends) {
                // this shouldn't be possible so we think it pre-dates the current schema so fix it
                reminder.weekdays = true;
                reminder.weekends = true;
            }
            weekdaysCheckbox.setChecked(reminder.weekdays);
            weekendsCheckbox.setChecked(reminder.weekends);
            homeOnlyCheckbox.setChecked(reminder.homeonly);
            speechCheckbox.setChecked(reminder.speak);
            graphIconCheckbox.setChecked(reminder.graphicon);
            reminderDaysEdt.setText(Long.toString(reminder.periodInUnits()));
        }

        maskAlternatingCheckbox(repeatingCheckbox.isChecked(), alternatingCheckbox);


        swapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Editable swappage = alternateEditText.getText();
                alternateEditText.setText(titleEditText.getText());
                titleEditText.setText(swappage);
            }
        });

        repeatingCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final String insert = chimeOnlyCheckbox.isChecked() ? xdrip.getAppContext().getString(R.string.sound_once) : xdrip.getAppContext().getString(R.string.alert_until_dismissed);
                final String second_insert = isChecked ? xdrip.getAppContext().getString(R.string.and_then_repeat) + " " + JoH.niceTimeScalarNatural(getPeriod(rbday, rbhour, rbweek)) : xdrip.getAppContext().getString(R.string.will_not_repeat);
                JoH.static_toast_long(xdrip.getAppContext().getString(R.string.reminder_will) + " " + insert + " " + second_insert);
                maskAlternatingCheckbox(isChecked, alternatingCheckbox);
            }
        });
        chimeOnlyCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final String insert = isChecked ?  xdrip.getAppContext().getString(R.string.sound_once) : xdrip.getAppContext().getString(R.string.alert_until_dismissed);
                final String second_insert = repeatingCheckbox.isChecked() ? xdrip.getAppContext().getString(R.string.and_then_repeat) + " " + JoH.niceTimeScalar(getPeriod(rbday, rbhour, rbweek)) : xdrip.getAppContext().getString(R.string.will_not_repeat);
                JoH.static_toast_long(xdrip.getAppContext().getString(R.string.reminder_will) + " " + insert + " " + second_insert);
            }
        });
        alternatingCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                alternateEditText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                swapButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                final String insert = isChecked ? xdrip.getAppContext().getString(R.string.alternate_between_titles) + " " + JoH.niceTimeScalar(getPeriod(rbday, rbhour, rbweek)) : xdrip.getAppContext().getString(R.string.always_use_same_title);
                JoH.static_toast_long(xdrip.getAppContext().getString(R.string.reminder_will) + " " + insert);
            }
        });

        alternateEditText.setVisibility(alternatingCheckbox.isChecked() ? View.VISIBLE : View.GONE);
        swapButton.setVisibility(alternatingCheckbox.isChecked() ? View.VISIBLE : View.GONE);

        dialogBuilder.setTitle(((reminder == null) ? xdrip.getAppContext().getString(R.string.title_add_reminder) : xdrip.getAppContext().getString(R.string.title_edit_reminder)));
        //dialogBuilder.setMessage("Enter text below");
        dialogBuilder.setPositiveButton(xdrip.getAppContext().getString(R.string.done), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String reminder_title = titleEditText.getText().toString().trim();
                Log.d(TAG, "Got reminder title: " + reminder_title);
                // get and scale period
                long period = getPeriod(rbday, rbhour, rbweek);

                PersistentStore.setBoolean("reminders-rbday", rbday.isChecked());
                PersistentStore.setBoolean("reminders-rbhour", rbhour.isChecked());
                PersistentStore.setBoolean("reminders-rbweek", rbweek.isChecked());

                Reminder new_reminder = reminder;
                if (new_reminder == null) {
                    new_reminder = Reminder.create(reminder_title, period);
                }

                if (new_reminder != null) {
                    if (reminder != null) {
                        new_reminder.title = reminder_title;
                        new_reminder.period = period;
                    }

                    final String alternate_text = alternateEditText.getText().toString();
                    if (alternate_text.length() > 2) {
                        new_reminder.alternate_title = alternate_text;
                    }

                    new_reminder.sound_uri = selectedSound;
                    new_reminder.repeating = repeatingCheckbox.isChecked();
                    new_reminder.chime_only = chimeOnlyCheckbox.isChecked();
                    new_reminder.alternating = alternatingCheckbox.isChecked();


                    new_reminder.weekdays = weekdaysCheckbox.isChecked();
                    new_reminder.weekends = weekendsCheckbox.isChecked();
                    new_reminder.homeonly = homeOnlyCheckbox.isChecked();
                    new_reminder.speak = speechCheckbox.isChecked();
                    new_reminder.graphicon = graphIconCheckbox.isChecked();


                    if ((new_reminder.priority > MEGA_PRIORITY) && (!megapriorityCheckbox.isChecked())) {
                        new_reminder.priority -= MEGA_PRIORITY;
                    } else if ((new_reminder.priority < MEGA_PRIORITY) && (megapriorityCheckbox.isChecked())) {
                        new_reminder.priority += MEGA_PRIORITY;
                    }

                    new_reminder.save();

                    freshen(new_reminder);

                    PersistentStore.setString("reminders-last-number", reminderDaysEdt.getText().toString());
                    PersistentStore.setLong("reminders-last-repeating", repeatingCheckbox.isChecked() ? 1 : 2);
                    PersistentStore.setLong("reminders-last-chimeonly", chimeOnlyCheckbox.isChecked() ? 1 : 2);
                    PersistentStore.setLong("reminders-last-alternating", alternatingCheckbox.isChecked() ? 1 : 2);

                    showcase(activity, Home.SHOWCASE_REMINDER2);
                } else {
                    JoH.static_toast_long(xdrip.getAppContext().getString(R.string.something_went_wrong_reminder));
                }

                dialog = null;
                dialogView = null;
            }
        });
        dialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog = null;
                dialogView = null;

            }
        });
        dialog = dialogBuilder.create();
        titleEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        titleEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (dialog != null)
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        dialog.show();
        if ((reminder != null) && (reminder.title.length() > 2)) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
        } else {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
        }

    }

    private long getPeriod(RadioButton rbday, RadioButton rbhour, RadioButton rbweek) {
        long period;
        try {
            period = Integer.parseInt(reminderDaysEdt.getText().toString());
        } catch (NumberFormatException e) {
            period = 1; // TODO avoid this happening from the UI
            try {
                reminderDaysEdt.setText("" + period);
            } catch (Exception ee) {
                //
            }
        }
        if (rbday.isChecked()) {
            period = period * Constants.DAY_IN_MS;
        } else if (rbhour.isChecked()) {
            period = period * Constants.HOUR_IN_MS;
        } else if (rbweek.isChecked()) {
            period = period * Constants.WEEK_IN_MS;
        }
        return period;
    }

    // set the state of the alternating checkbox
    private void maskAlternatingCheckbox(boolean state, CheckBox alternatingCheck) {
        if (state) {
            alternatingCheck.setEnabled(true);
        } else {
            alternatingCheck.setEnabled(false);
            alternatingCheck.setChecked(false);
        }
    }

    private int getPositionFromReminderId(final int id) {
        for (int i = 0; i < reminders.size(); i++) {
            if (reminders.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }


    ////
    public static void doAlert(final Reminder reminder) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("reminder-alert-wakeup", 20000);
        if (JoH.isOngoingCall()) {
            UserError.Log.uel(TAG, "Not alerting due to ongoing call");
            return;
        }
        Log.d(TAG, "Scheduling alert reminder in 10 seconds time");
        // avoid conflicts with other notification alerts in first 10 seconds

        JoH.runOnUiThreadDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "delayed wakeup firing");
                startReminderWithExtra(REMINDER_WAKEUP, reminder.getId().toString());
            }
        }, 9000);

        JoH.runOnUiThreadDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "delayed alert firing");
                final Intent notificationIntent = new Intent(xdrip.getAppContext(), Reminders.class).putExtra("reminder_id", reminder.getId().toString());
                //final Intent notificationDeleteIntent = new Intent(xdrip.getAppContext(), Reminders.class).putExtra("snooze_id", reminder.getId()).putExtra(Pref.getBooleanDefaultFalse(Reminder.REMINDERS_CANCEL_DEFAULT) ? "cancel" : "snooze", "true");
                final Intent notificationDeleteIntent = new Intent(xdrip.getAppContext(), ReminderReceiver.class)
                        .setAction(REMINDER_ACTION)
                        .putExtra("snooze_id", reminder.getId())
                        .putExtra(Pref.getBooleanDefaultFalse(Reminder.REMINDERS_CANCEL_DEFAULT) ? "cancel" : "snooze", "true");
                final PendingIntent deleteIntent = PendingIntent.getBroadcast(xdrip.getAppContext(), NOTIFICATION_ID + 1, notificationDeleteIntent, FLAG_UPDATE_CURRENT);
                final PendingIntent pendingIntent = PendingIntent.getActivity(xdrip.getAppContext(), NOTIFICATION_ID, notificationIntent, FLAG_UPDATE_CURRENT);

                if (reminder.graphicon) {
                    Treatments.create_note("Reminder"+": " + reminder.getTitle(), tsl());
                }

                JoH.showNotification(reminder.getTitle(), xdrip.getAppContext().getString(R.string.reminder_due) + " " + JoH.hourMinuteString(reminder.next_due), pendingIntent, NOTIFICATION_ID, NotificationChannels.REMINDER_CHANNEL, true, true, deleteIntent, JoH.isOngoingCall() ? null : (reminder.sound_uri != null) ? Uri.parse(reminder.sound_uri) : Uri.parse(JoH.getResourceURI(R.raw.reminder_default_notification)), null);

                //    JoH.showNotification(reminder.getTitle(), "Reminder due " + JoH.hourMinuteString(reminder.next_due), pendingIntent, NOTIFICATION_ID, true, true, deleteIntent, JoH.isOngoingCall() ? null : (reminder.sound_uri != null) ? Uri.parse(reminder.sound_uri) : Uri.parse(JoH.getResourceURI(R.raw.reminder_default_notification)));
                UserError.Log.ueh("Reminder Alert", reminder.getTitle() + " due: " + dateTimeText(reminder.next_due) + ((reminder.snoozed_till > reminder.next_due) ? " snoozed till: " + dateTimeText(reminder.snoozed_till) : ""));
                if (reminder.speak) {
                    SpeechUtil.say(reminder.getTitle(), 1000, 3);
                }
                reminder.notified();
            }
        }, 10000);
        // intentionally don't release wakelock to ensure sound plays
    }

    public static void cancelAlert() {
        JoH.runOnUiThreadDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Cancelling notification");
                JoH.cancelNotification(NOTIFICATION_ID);
            }
        }, 500);
    }

    ////


    private void askTime(final int position) {
        final Calendar calendar = Calendar.getInstance();
        final Reminder reminder = reminders.get(position);
        calendar.setTimeInMillis(reminder.next_due);

        final TimePickerFragment timePickerFragment = new TimePickerFragment();
        timePickerFragment.setTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        timePickerFragment.setTitle(xdrip.getAppContext().getString(R.string.title_what_time_day));
        timePickerFragment.setTimeCallback(new ProfileAdapter.TimePickerCallbacks() {
            @Override
            public void onTimeUpdated(int newmins) {
                int min = newmins % 60;
                int hour = (newmins - min) / 60;
                calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), hour, min);
                reminder.next_due = calendar.getTimeInMillis();
                reminder.snoozed_till = 0; // reset this field
                reminder.save();
                freshen(reminder);
            }
        });
        timePickerFragment.show(this.getFragmentManager(), "TimePicker");

        // appears on top
        if (JoH.msTill(reminder.next_due) > Constants.DAY_IN_MS) {
            final DatePickerFragment datePickerFragment = new DatePickerFragment();
            datePickerFragment.setAllowFuture(true);
            datePickerFragment.setEarliestDate(tsl());
            datePickerFragment.setInitiallySelectedDate(reminder.next_due);
            datePickerFragment.setTitle(xdrip.getAppContext().getString(R.string.title_which_day));
            datePickerFragment.setDateCallback(new ProfileAdapter.DatePickerCallbacks() {
                @Override
                public void onDateSet(int year, int month, int day) {
                    calendar.set(year, month, day);

                }
            });
            datePickerFragment.show(this.getFragmentManager(), "DatePicker");
        }
    }

    ////

    private synchronized void hideSnoozeFloater() {
        if (!floaterHidden) {
            animateSnoozeFloater(0, floatingsnooze.getHeight(), new AccelerateInterpolator(1.5f));
            floaterHidden = true;
        }
    }

    private synchronized void showSnoozeFloater() {
        if (floaterHidden) {
            animateSnoozeFloater(floatingsnooze.getHeight(), 0, new DecelerateInterpolator(1.5f));
            floaterHidden = false;
            floatingsnooze.setVisibility(View.VISIBLE);
            showcase(this, SHOWCASE_REMINDER3);
        }
        hideKeyboard(recyclerView);
    }

    private void setFloaterText(String msg) {
        Log.d(TAG, "Setting floater text:" + msg);
        try {
            floaterText.setText(msg);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Unable to set floater text to: " + msg);
        }
    }

    private void animateSnoozeFloater(float start, float end, Interpolator interpolator) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(start, end);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                floatingsnooze.setTranslationY(value);
            }
        });

        valueAnimator.setInterpolator(interpolator);
        valueAnimator.setDuration(DEFAULT_ANIMATION_DURATION);
        valueAnimator.start();
    }

    public static void startReminderWithExtra(String extra, String text) {
        final Intent intent = new Intent(xdrip.getAppContext(), Reminders.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(extra, text);
        xdrip.getAppContext().startActivity(intent);
    }


    public void processIncomingBundle(Bundle bundle) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("reminder-bundler", 10000);
        if (bundle != null) {

            if ((bundle != null) && (d)) {
                for (String key : bundle.keySet()) {
                    Object value = bundle.get(key);
                    if (value != null) {
                        Log.d(TAG, String.format("Bundle: %s %s (%s)", key,
                                value.toString(), value.getClass().getName()));
                    }
                }
            }

            if (bundle.getString("snooze") != null) {
                long id = bundle.getLong("snooze_id");
                Log.d(TAG, "Reminder id for snooze: " + id);
                final Reminder reminder = Reminder.byid(id);
                if (reminder != null) {
                    UserError.Log.uel(TAG, "Reminder snooze for " + reminder.title);
                    final long snooze_time = reminder.last_snoozed_for > 0 ? reminder.last_snoozed_for : default_snooze;
                    snoozeReminder(reminder, snooze_time);
                    JoH.static_toast_long(xdrip.getAppContext().getString(R.string.snoozed_reminder_for) + " " + JoH.niceTimeScalar(snooze_time));
                    reloadList();
                    hideSnoozeFloater();
                    hideKeyboard(recyclerView);
                }

            } else if (bundle.getString("cancel") != null) {
                Log.d(TAG, "Cancel/Reschedule reminder from intent");
                long id = bundle.getLong("snooze_id");
                Log.d(TAG, "Reminder id for snooze: " + id);
                final Reminder reminder = Reminder.byid(id);
                if (reminder != null) {
                    UserError.Log.uel(TAG, "Reminder cancel for " + reminder.title);
                    if (rescheduleOrCancelReminder(reminder)) {
                        reloadList();
                        hideSnoozeFloater();
                        hideKeyboard(recyclerView);
                    }
                }
            } else {
                Log.d(TAG, "Processing non null default bundle");
                reloadList();
                hideSnoozeFloater();
                hideKeyboard(recyclerView);
                JoH.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        wakeUpScreen(false);
                    }
                });
                if (bundle.getString(REMINDER_WAKEUP) != null) {
                    try {
                        recyclerView.smoothScrollToPosition(getPositionFromReminderId(Integer.parseInt(bundle.getString(REMINDER_WAKEUP))));
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Couldn't scroll to position");
                    }
                    JoH.runOnUiThreadDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Checking for sensor: " + proximity);
                            if (!proximity) {
                                wakeUpScreen(true);
                            }
                        }
                    }, 2000);
                }
            }
        }
        // intentionally don't release wakelock for proximity detection etc
    }

    private void hideKeyboard(View v) {
        if (v == null) return;
        InputMethodManager inputMethodManager = (InputMethodManager) this
                .getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void wakeUpScreen(boolean unlock) {
        if (!JoH.isScreenOn() || (unlock)) {
            final int timeout = 60000;
            final int wakeUpFlags;

            Log.d(TAG, "Wake up screen called with unlock = " + unlock);

            if (unlock) {
                wakeUpFlags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        //WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
            } else {
                final PowerManager pm = (PowerManager) xdrip.getAppContext().getSystemService(Context.POWER_SERVICE);
                @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "reminder-lockscreen");
                wl.acquire(10000);
                wakeUpFlags =
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
            }
            final PowerManager.WakeLock wl = JoH.getWakeLock("reminder-full-wakeup", timeout + 1000);
            final Window win = getWindow();
            win.addFlags(wakeUpFlags);
            final Timer t = new Timer();
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    JoH.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            win.clearFlags(wakeUpFlags);
                        }
                    });

                    JoH.releaseWakeLock(wl);

                }
            }, timeout);
        } else {
            Log.d(TAG, "Screen is already on so not turning on");
        }
    }

    private static void showcase(final Activity activity, final int which) {

        final ViewTarget target;
        final String title;
        final String message;
        int size1 = 200;
        int size2 = 70;
        long delay = 1000;

        final boolean oneshot = true;
        final int option = which;
        if ((oneshot) && (ShotStateStore.hasShot(option))) return;


        switch (which) {
            case Home.SHOWCASE_REMINDER1:
                target = new ViewTarget(R.id.fab, activity);
                title = xdrip.getAppContext().getString(R.string.title_You_have_no_reminders_yet);
                message = xdrip.getAppContext().getString(R.string.message_reminders_explanation);
                delay = 200;
                break;

            case Home.SHOWCASE_REMINDER2:
                target = null;
                title = xdrip.getAppContext().getString(R.string.title_swipe_reminder);
                message = xdrip.getAppContext().getString(R.string.message_swipe_explanation);
                break;

            case Home.SHOWCASE_REMINDER3:
                target = new ViewTarget(R.id.imageButton5, activity);
                title = xdrip.getAppContext().getString(R.string.title_reminder_snooze_undo);
                message = xdrip.getAppContext().getString(R.string.message_snooze_explanaition_undo);
                break;

            case Home.SHOWCASE_REMINDER4:
                target = new ViewTarget(R.id.reminderTrashButton, activity);
                title = xdrip.getAppContext().getString(R.string.title_snooze_trash);
                message = xdrip.getAppContext().getString(R.string.message_snooze_trash);
                delay = 10;
                break;

            case Home.SHOWCASE_REMINDER5:
                target = new ViewTarget(R.id.imageButton7, activity);
                title = xdrip.getAppContext().getString(R.string.title_snooze_hide);
                message = xdrip.getAppContext().getString(R.string.message_snooze_hide);
                delay = 10;
                break;

            case Home.SHOWCASE_REMINDER6:
                target = new ViewTarget(R.id.button5, activity);
                title = xdrip.getAppContext().getString(R.string.title_snooze_times);
                message = xdrip.getAppContext().getString(R.string.message_snooze_times);
                delay = 10;
                break;

            default:
                return;
        }

        final int f_size1 = size1;
        final int f_size2 = size2;

        JoH.runOnUiThreadDelayed(new Runnable() {
            @Override
            public void run() {
                final ShowcaseView myShowcase;
                if (target != null) {

                    myShowcase = new ShowcaseView.Builder(activity)
                            .setStyle(R.style.CustomShowcaseTheme2)
                            .setContentTitle(title)
                            .setTarget(target)
                            .blockAllTouches()
                            .setContentText("\n" + message)
                            .setShowcaseDrawer(new JamorhamShowcaseDrawer(activity.getResources(), activity.getTheme(), f_size1, f_size2, 255))
                            .singleShot(oneshot ? option : -1)
                            .build();

                } else {
                    myShowcase = new ShowcaseView.Builder(activity)
                            .setStyle(R.style.CustomShowcaseTheme2)
                            .setContentTitle(title)
                            .blockAllTouches()
                            .setContentText("\n" + message)
                            .setShowcaseDrawer(new JamorhamShowcaseDrawer(activity.getResources(), activity.getTheme(), f_size1, f_size2, 255))
                            .singleShot(oneshot ? option : -1)
                            .build();
                }
                myShowcase.setTag(which);
                myShowcase.setBackgroundColor(Color.TRANSPARENT);
                myShowcase.setShouldCentreText(false);
                myShowcase.setOnShowcaseEventListener(new OnShowcaseEventListener() {
                                                          @Override
                                                          public void onShowcaseViewHide(ShowcaseView showcaseView) {

                                                          }

                                                          @Override
                                                          public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                                                              switch ((int) showcaseView.getTag()) {
                                                                  case Home.SHOWCASE_REMINDER3:
                                                                      showcase(activity, Home.SHOWCASE_REMINDER4);
                                                                      break;
                                                                  case Home.SHOWCASE_REMINDER4:
                                                                      showcase(activity, Home.SHOWCASE_REMINDER5);
                                                                      break;
                                                                  case Home.SHOWCASE_REMINDER5:
                                                                      showcase(activity, Home.SHOWCASE_REMINDER6);
                                                                      break;
                                                              }
                                                          }

                                                          @Override
                                                          public void onShowcaseViewShow(ShowcaseView showcaseView) {

                                                          }

                                                          @Override
                                                          public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {
                                                          }
                                                      }
                );


                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                int margin = (int) activity.getResources().getDimension(R.dimen.button_margin);
                params.setMargins(margin, margin, margin, margin);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                myShowcase.setButtonPosition(params);
                myShowcase.show();
            }
        }, delay);
    }
}
