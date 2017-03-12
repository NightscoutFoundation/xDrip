package com.eveningoutpost.dexdrip;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Reminder;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.JamorhamShowcaseDrawer;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.ShotStateStore;
import com.eveningoutpost.dexdrip.profileeditor.DatePickerFragment;
import com.eveningoutpost.dexdrip.profileeditor.ProfileAdapter;
import com.eveningoutpost.dexdrip.profileeditor.TimePickerFragment;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.rits.cloning.Cloner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static lecho.lib.hellocharts.animation.ChartDataAnimator.DEFAULT_ANIMATION_DURATION;

// TODO Odd/Even reminders
// TODO swipe right reschedule options
// TODO wake up option

public class Reminders extends ActivityWithRecycler implements SensorEventListener {

    private static final String TAG = "Reminders";
    private static final int NOTIFICATION_ID = 765;
    private static final String REMINDER_WAKEUP = "REMINDER_WAKEUP";

    public final List<Reminder> reminders = new ArrayList<>();

    private AlertDialog dialog;
    private EditText reminderDaysEdt;
    private View dialogView;
    private CardView floatingsnooze;
    private TextView floaterText;
    private boolean floaterHidden = true;

    private Reminder last_undo, last_swiped;
    private int last_undo_pos;

    private long default_snooze = Constants.MINUTE_IN_MS * 15;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private boolean proximity = true; // default to near

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);
        setTheme(R.style.AppThemeToolBarLite); // for toolbar mode

        //setSupportActionBar((Toolbar)findViewById(R.id.reminder_toolbar));
        JoH.fixActionBar(this);

        setTitle("xDrip+ Reminders");
        // TODO subtitle with summary
        recyclerView = (RecyclerView) findViewById(R.id.reminder_recycler);
        floatingsnooze = (CardView) findViewById(R.id.floatingsnooze);
        floaterText = (TextView) findViewById(R.id.floaterText);

        floatingsnooze.setVisibility(View.GONE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        mAdapter = new RecyclerAdapter(this, reminders);

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

    @Override
    protected void onResume() {
        PowerManager.WakeLock wl = JoH.getWakeLock("reminders-onresume", 15000);
        super.onResume();
        if (JoH.ratelimit("proximity-reset", 5)) {
            Log.d(TAG, "Initializing proximity as true");
            proximity = true; // default to near
        }
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        reloadList();
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
            Log.d(TAG, "Sensor: " + event.values[0] + " " + mProximity.getMaximumRange());
            if (event.values[0] <= (Math.min(mProximity.getMaximumRange() / 2, 10))) {
                proximity = true; // near
            } else {
                proximity = false; // far
            }
            Log.d(TAG, "Proxmity set to: " + proximity);
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
                        notifyItemChanged(pos);
                    }
                    return handled;
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
        public void onBindViewHolder(final ActivityWithRecycler.MyViewHolder base_holder, int position) {
            MyViewHolder holder = (MyViewHolder) base_holder; // cast it
            final Reminder reminder_item = reminders.get(position);
            holder.position = position; // should we use getadapterposition instead?
            holder.title_text.setText(reminder_item.title);


            String lastDetails = "";
            if (reminder_item.fired_times > 0) {
                if (JoH.msSince(reminder_item.last_fired) < Constants.DAY_IN_MS) {
                    lastDetails += JoH.hourMinuteString(reminder_item.last_fired);
                } else {
                    lastDetails += JoH.dateTimeText(reminder_item.last_fired);
                }
                lastDetails += " (" + reminder_item.fired_times + ")";
            }
            String nextDetails = "";
            if (reminder_item.enabled) {
                if (Math.abs(JoH.msSince(reminder_item.next_due)) < Constants.DAY_IN_MS) {
                    nextDetails += JoH.hourMinuteString(reminder_item.next_due);
                } else {
                    final String dstring = JoH.dateTimeText(reminder_item.next_due);
                    nextDetails += dstring.substring(0, dstring.length() - 3); // remove seconds
                }
            }
            holder.small_text.setText(lastDetails + " " + nextDetails);

            final long duein = Math.max(JoH.msTill(reminder_item.next_due), JoH.msTill(reminder_item.snoozed_till));
            holder.wholeBlock.setBackgroundColor(Color.TRANSPARENT);
            String firstpart = "";
            if ((reminder_item.snoozed_till > JoH.tsl()) && (reminder_item.next_due < JoH.tsl())) {
                firstpart = "Snoozed for " + JoH.niceTimeTill(reminder_item.snoozed_till);
            } else {
                if (duein >= 0) {
                    String natural_due = JoH.niceTimeScalarNatural(duein);
                    if (natural_due.matches("^[0-9]+.*")) {
                        natural_due = "in" + " " + natural_due;
                    }
                    firstpart = "Due " + natural_due;
                } else {
                    firstpart = "DUE " + JoH.hourMinuteString(reminder_item.next_due);
                    holder.wholeBlock.setBackgroundColor(Color.parseColor("#660066"));
                }
            }
            holder.next_due_text.setText(firstpart + (reminder_item.repeating ? ", repeats every " + JoH.niceTimeScalarRedux(reminder_item.period) : ""));

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

                // swipe left
                snoozeReminder(remind, remind.last_snoozed_for > 0 ? remind.last_snoozed_for : default_snooze);

                last_swiped = remind;
                last_undo_pos = position;
                reminders.remove(position);
                notifyItemRemoved(position);
                showSnoozeFloater();
            } else {
                // swipe right

                if (remind.repeating || !remind.isDue()) {

                    remind.next_due = remind.next_due + remind.period;
                    // check it is actually in the future
                    while (remind.next_due < JoH.tsl()) {
                        remind.next_due = remind.next_due + remind.period;
                    }
                    setFloaterText(remind.title + " next in " + JoH.niceTimeTill(remind.next_due));
                    remind.save();
                } else if (!remind.repeating) {
                    setFloaterText(remind.title + " completed");
                    remind.enabled = false;
                    remind.save();
                }
                //if (remind.isDue()) {
                //    remind.snoozed_till = JoH.tsl() + default_snooze;
                //    setFloaterText(remind.title + " snoozed for " + JoH.niceTimeScalar(default_snooze));
                // } else {
                //     remind.next_due = JoH.tsl() + default_snooze;
                //      setFloaterText(remind.title + " postponed for " + JoH.niceTimeScalar(default_snooze));
                //  }
                last_swiped = remind;
                last_undo_pos = position;
                reminders.remove(position);
                notifyItemRemoved(position);
                showSnoozeFloater();
            }
            JoH.runOnUiThreadDelayed(new Runnable() {
                @Override
                public void run() {
                    reinject(remind);
                }
            }, 500);
        }

    }

    private void snoozeReminder(Reminder remind, long snooze_time) {
        if (remind == null) return;
        remind.last_snoozed_for = snooze_time;
        if (remind.isDue()) {
            remind.snoozed_till = JoH.tsl() + snooze_time;
            setFloaterText(remind.title + " snoozed for " + JoH.niceTimeScalar(snooze_time));
        } else {
            remind.snoozed_till = remind.next_due + snooze_time;
            setFloaterText(remind.title + " postponed for " + JoH.niceTimeScalar(snooze_time));
        }
        remind.save();
    }

    //////////// Button pushes
    public void newReminder(View v) {
        showNewReminderDialog(v, 0, 0);
    }

    public void snoozeAdjust(View v) {
        final String button_text = ((Button) v).getText().toString();
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
        reminderDaysEdt.setText(Integer.toString(Integer.parseInt(reminderDaysEdt.getText().toString()) + 1));
    }

    public void reminderDownButton(View v) {
        reminderDaysEdt.setText(Integer.toString(Integer.parseInt(reminderDaysEdt.getText().toString()) - 1));
        if (reminderDaysEdt.getText().toString().equals("0")) {
            reminderDaysEdt.setText("1");
        }
    }

    public synchronized void reminderTrashButton(View v) {
        if (last_undo != null) {
            dismissDoppelgangerItem(last_undo);
            JoH.static_toast_long("Deleted: " + last_undo.title);
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
            reminders.add(last_undo_pos, last_undo);
            mAdapter.notifyItemInserted(last_undo_pos);
            last_undo = null;
            hideSnoozeFloater();
        } else {
            JoH.static_toast_short("Nothing to undo!");
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

    private synchronized void reinject(Reminder reminder) {
        if (reminder == null) return;
        int i = reinjectionPosition(reminder);
        // TODO lock?
        reminders.add(i, reminder);
        mAdapter.notifyItemInserted(i);
    }

    private synchronized int reinjectionPosition(Reminder reminder) {
        if (reminder == null) return 0;
        synchronized (reminders) {
            for (int i = 0; i < reminders.size(); i++) {
                // TODO consider snooze?
                if (reminder.next_due < reminders.get(i).next_due) {
                    return i;
                }
            }
        }
        return reminders.size(); // is this right?
    }

    private void showNewReminderDialog(View myitem, final long timestamp, final double position) {
        final Activity activity = this;
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        dialogView = inflater.inflate(R.layout.reminder_new_dialog, null);
        dialogBuilder.setView(dialogView);

        // New Reminder title
        final EditText edt = (EditText) dialogView.findViewById(R.id.reminder_edit_new);

        final RadioButton rbday = (RadioButton) dialogView.findViewById(R.id.reminderDayButton);
        final RadioButton rbhour = (RadioButton) dialogView.findViewById(R.id.reminderHourButton);
        final RadioButton rbweek = (RadioButton) dialogView.findViewById(R.id.reminderWeekButton);


        rbday.setChecked(PersistentStore.getBoolean("reminders-rbday"));
        rbhour.setChecked(PersistentStore.getBoolean("reminders-rbhour"));
        rbweek.setChecked(PersistentStore.getBoolean("reminders-rbweek"));

        // first run if nothing set default to day
        if (!rbday.isChecked() && !rbhour.isChecked() && !rbweek.isChecked()) {
            rbday.setChecked(true);
        }

        edt.addTextChangedListener(new TextWatcher() {

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
        repeatingCheckbox.setChecked(PersistentStore.getLong("reminders-last-repeating") != 2);


        try {
            reminderDaysEdt.setText(!PersistentStore.getString("reminders-last-number").equals("") ? Integer.toString(Integer.parseInt(PersistentStore.getString("reminders-last-number"))) : "1");
        } catch (Exception e) {
            //
        }


        dialogBuilder.setTitle("Add Reminder");
        //dialogBuilder.setMessage("Enter text below");
        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String reminder_title = edt.getText().toString().trim();
                Log.d(TAG, "Got reminder title: " + reminder_title);
                // get and scale period
                long period = Integer.parseInt(reminderDaysEdt.getText().toString());
                if (rbday.isChecked()) {
                    period = period * Constants.DAY_IN_MS;
                } else if (rbhour.isChecked()) {
                    period = period * Constants.HOUR_IN_MS;
                } else if (rbweek.isChecked()) {
                    period = period * Constants.WEEK_IN_MS;
                }

                PersistentStore.setBoolean("reminders-rbday", rbday.isChecked());
                PersistentStore.setBoolean("reminders-rbhour", rbhour.isChecked());
                PersistentStore.setBoolean("reminders-rbweek", rbweek.isChecked());

                // TODO handle non-repeating init
                Reminder new_reminder = Reminder.create(reminder_title, period);
                if (new_reminder != null) {
                    reminders.add(0, new_reminder);
                    mAdapter.notifyItemInserted(0);

                    new_reminder.repeating = repeatingCheckbox.isChecked();
                    new_reminder.save();

                    // TODO scroll to position?
                    PersistentStore.setString("reminders-last-number", reminderDaysEdt.getText().toString());
                    PersistentStore.setLong("reminders-last-repeating", repeatingCheckbox.isChecked() ? 1 : 2);
                    showcase(activity, Home.SHOWCASE_REMINDER2);
                } else {
                    JoH.static_toast_long("Something went wrong creating the reminder");
                }

                dialog = null;
                dialogView = null;
            }
        });
        dialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //  if (getPreferencesBooleanDefaultFalse("default_to_voice_notes")) showcasemenu(SHOWCASE_NOTE_LONG);
                dialog = null;
                dialogView = null;

            }
        });
        dialog = dialogBuilder.create();
        edt.setInputType(InputType.TYPE_CLASS_TEXT);
        edt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (dialog != null)
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);

    }

    ////
    public static void doAlert(final Reminder reminder) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("reminder-alert-wakeup", 20000);
        Log.d(TAG, "Scheduling alert reminder in 10 seconds time");
        // avoid conflicts with other notification alerts in first 10 seconds
        JoH.runOnUiThreadDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "delayed alert firing");
                startReminderWithExtra(REMINDER_WAKEUP, REMINDER_WAKEUP);
                final Intent notificationIntent = new Intent(xdrip.getAppContext(), Reminders.class).putExtra("reminder_id", reminder.getId().toString());
                final Intent notificationDeleteIntent = new Intent(xdrip.getAppContext(), Reminders.class).putExtra("snooze_id", reminder.getId()).putExtra("snooze", "true");
                final PendingIntent pendingIntent = PendingIntent.getActivity(xdrip.getAppContext(), NOTIFICATION_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                final PendingIntent deleteIntent = PendingIntent.getActivity(xdrip.getAppContext(), NOTIFICATION_ID, notificationDeleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                // TODO add sound uri
                JoH.showNotification(reminder.title, "Reminder due " + JoH.hourMinuteString(reminder.next_due), pendingIntent, NOTIFICATION_ID, true, true, deleteIntent, null);
                UserError.Log.ueh("Reminder Alert", reminder.title + " due: " + JoH.dateTimeText(reminder.next_due) + ((reminder.snoozed_till > reminder.next_due) ? " snoozed till: " + JoH.dateTimeText(reminder.snoozed_till) : ""));

            }
        }, 10000);
        // intentionally don't release wakelock to ensure sound plays
    }

    ////


    private void askTime(final int position) {
        final Calendar calendar = Calendar.getInstance();
        final Reminder reminder = reminders.get(position);
        calendar.setTimeInMillis(reminder.next_due);

        final TimePickerFragment timePickerFragment = new TimePickerFragment();
        timePickerFragment.setTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        timePickerFragment.setTitle("What time of day?");
        timePickerFragment.setTimeCallback(new ProfileAdapter.TimePickerCallbacks() {
            @Override
            public void onTimeUpdated(int newmins) {
                int min = newmins % 60;
                int hour = (newmins - min) / 60;
                calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), hour, min);
                reminder.next_due = calendar.getTimeInMillis();
                reminder.save();
                mAdapter.notifyItemChanged(position);
            }
        });
        timePickerFragment.show(this.getFragmentManager(), "TimePicker");

        // appears on top
        if (JoH.msTill(reminder.next_due) > Constants.DAY_IN_MS) {
            final DatePickerFragment datePickerFragment = new DatePickerFragment();
            datePickerFragment.setAllowFuture(true);
            datePickerFragment.setEarliestDate(JoH.tsl());
            datePickerFragment.setInitiallySelectedDate(reminder.next_due);
            datePickerFragment.setTitle("Which day?");
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
        }
        hideKeyboard(recyclerView);
    }

    private void setFloaterText(String msg) {
        Log.d(TAG, "Setting floater text:" + msg);
        floaterText.setText(msg);
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


    private void processIncomingBundle(Bundle bundle) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("reminder-bundler", 10000);
        if (bundle != null) {
            if (bundle.getString("snooze") != null) {
                long id = bundle.getLong("snooze_id");
                Log.d(TAG, "Reminder id for snooze: " + id);
                Reminder reminder = Reminder.byid(id);
                if (reminder != null) {
                    final long snooze_time = reminder.last_snoozed_for > 0 ? reminder.last_snoozed_for : default_snooze;
                    snoozeReminder(reminder, snooze_time);
                    JoH.static_toast_long("Snoozed reminder for " + JoH.niceTimeScalar(snooze_time));
                    reloadList();
                    hideSnoozeFloater();
                    hideKeyboard(recyclerView);
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
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "reminder-lockscreen");
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

    private static void showcase(final Activity activity, int which) {

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
                title = "You have no reminders yet";
                message = "Reminders can be used for things like a pump site change or anything else.\n\nThey can repeat, be set for various time periods. Be snoozed, postponed, rescheduled etc.\n\nClick the + button to add a reminder.";
                break;

            case Home.SHOWCASE_REMINDER2:
                target = null;
                title = "Swipe left or right";
                message = "\u2190 Swipe LEFT to delay or snooze a reminder\n\n\u2192 Swipe RIGHT schedule the next future reminder\n\nTapping on the title or time allows editing.";
                delay = 1000;
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

                myShowcase.setBackgroundColor(Color.TRANSPARENT);
                myShowcase.setShouldCentreText(false);

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
