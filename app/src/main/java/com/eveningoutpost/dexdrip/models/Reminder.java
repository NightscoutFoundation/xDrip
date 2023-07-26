package com.eveningoutpost.dexdrip.models;

import android.content.Context;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.Reminders;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.HomeWifi;
import com.google.gson.annotations.Expose;

import java.util.Calendar;
import java.util.List;


/**
 * Created by jamorham on 01/02/2017.
 */

@Table(name = "Reminder", id = BaseColumns._ID)
public class Reminder extends Model {


    private static final String TAG = "Reminder";
    private static boolean patched = false;
    public static final String REMINDERS_ALL_DISABLED = "reminders-all-disabled";
    public static final String REMINDERS_NIGHT_DISABLED = "reminders-at-night-disabled";
    public static final String REMINDERS_RESTART_TOMORROW = "reminders-restart-tomorrow";
    public static final String REMINDERS_ADVANCED_MODE = "reminders-advanced-mode";
    public static final String REMINDERS_CANCEL_DEFAULT = "reminders-cancel-default";
    public static final String REMINDERS_GRAPH_ICONS = "reminders-graph-icons";
    private static final String[] schema = {
            "CREATE TABLE Reminder (_id INTEGER PRIMARY KEY AUTOINCREMENT)",
            "ALTER TABLE Reminder ADD COLUMN next_due INTEGER",
            "ALTER TABLE Reminder ADD COLUMN period INTEGER",
            "ALTER TABLE Reminder ADD COLUMN snoozed_till INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN last_snoozed_for INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN last_fired INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN fired_times INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN alerted_times INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN title TEXT",
            "ALTER TABLE Reminder ADD COLUMN alt_title TEXT",
            "ALTER TABLE Reminder ADD COLUMN sound_uri TEXT",
            "ALTER TABLE Reminder ADD COLUMN ideal_time TEXT",
            "ALTER TABLE Reminder ADD COLUMN priority INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN enabled INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN weekdays INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN weekends INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN repeating INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN alternating INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN alternate INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN chime INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN homeonly INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN speak INTEGER DEFAULT 0",
            "ALTER TABLE Reminder ADD COLUMN graphicon INTEGER DEFAULT 0",
            "CREATE INDEX index_Reminder_next_due on Reminder(next_due)",
            "CREATE INDEX index_Reminder_enabled on Reminder(enabled)",
            "CREATE INDEX index_Reminder_weekdays on Reminder(weekdays)",
            "CREATE INDEX index_Reminder_homeonly on Reminder(homeonly)",
            "CREATE INDEX index_Reminder_weekends on Reminder(weekends)",
            "CREATE INDEX index_Reminder_priority on Reminder(priority)",
            "CREATE INDEX index_Reminder_timestamp on Reminder(timestamp)",
            "CREATE INDEX index_Reminder_snoozed_till on Reminder(snoozed_till)"
    };

    @Expose
    @Column(name = "title")
    public String title;

    @Expose
    @Column(name = "alt_title")
    public String alternate_title;

    @Expose
    @Column(name = "next_due", index = true)
    public long next_due;

    @Expose
    @Column(name = "period")
    public long period;

    @Expose
    @Column(name = "sound_uri")
    public String sound_uri;

    @Expose
    @Column(name = "enabled", index = true)
    public boolean enabled;

    @Expose
    @Column(name = "weekdays", index = true)
    public boolean weekdays;

    @Expose
    @Column(name = "weekends", index = true)
    public boolean weekends;

    @Expose
    @Column(name = "homeonly", index = true)
    public boolean homeonly;

    @Expose
    @Column(name = "speak")
    public boolean speak;

    @Expose
    @Column(name = "graphicon")
    public boolean graphicon;

    @Expose
    @Column(name = "repeating")
    public boolean repeating;

    @Expose
    @Column(name = "chime")
    public boolean chime_only;

    @Expose
    @Column(name = "alternating")
    public boolean alternating;

    @Expose
    @Column(name = "alternate")
    public boolean alternate;

    @Expose
    @Column(name = "snoozed_till", index = true)
    public long snoozed_till;

    @Expose
    @Column(name = "last_fired", index = true)
    public long last_fired;

    @Expose
    @Column(name = "last_snoozed_for")
    public long last_snoozed_for;

    @Expose
    @Column(name = "fired_times")
    public long fired_times;

    @Expose
    @Column(name = "alerted_times")
    public long alerted_times;

    @Expose
    @Column(name = "priority")
    public long priority;

    @Expose
    @Column(name = "ideal_time")
    public String ideal_time;


    public boolean isAlternate() {
        if (alternating && (alternate_title != null)) {
            return alternate;
        } else {
            return false;
        }
    }

    public String getTitle() {
        return isAlternate() ? alternate_title : title;
    }

    public String getAlternateTitle() {
        return alternate_title != null ? alternate_title : title + " alternate";
    }

    public void updateTitle(String new_title) {
        if (isAlternate()) {
            alternate_title = new_title;
        } else {
            title = new_title;
        }
        save();
    }

    public boolean isHoursPeriod() {
        return (period >= Constants.HOUR_IN_MS) && (period < Constants.DAY_IN_MS);
    }

    public boolean isDaysPeriod() {
        return (period >= Constants.DAY_IN_MS) && (period < (Constants.WEEK_IN_MS * 2));
    }

    public boolean isWeeksPeriod() {
        return (period >= (2 * Constants.WEEK_IN_MS));
    }

    public long periodInUnits() {
        if (isDaysPeriod()) return period / Constants.DAY_IN_MS;
        if (isHoursPeriod()) return period / Constants.HOUR_IN_MS;
        if (isWeeksPeriod()) return period / Constants.WEEK_IN_MS;
        return -1; // ERROR
    }

    public boolean isDue() {
        return (enabled) && (next_due <= JoH.tsl());
    }

    public boolean isOverdueBy(long ms) {
       return isDue() && next_due <= (JoH.tsl() - ms);
    }

    public boolean isOverdue() {
        return isOverdueBy(Constants.DAY_IN_MS);
    }

    public boolean isSnoozed() {
        if (snoozed_till > JoH.tsl()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean shouldNotify() {
        return enabled && isDue() && !isSnoozed();
    }

    public synchronized void notified() {
        if (last_fired < next_due) fired_times++;
        alerted_times++;
        last_fired = JoH.tsl();
        if (chime_only) {
            if (repeating) {
                UserError.Log.d(TAG, "Rescheduling next");
                schedule_next();
            } else {
                enabled = false;
            }
        }
        save();
    }

    public synchronized void reminder_alert() {
        Reminders.doAlert(this);
    }

    public synchronized long getPotentialNextSchedule() {
        long now = JoH.tsl();
        long next = this.next_due + this.period;
        // check it is actually in the future
        while (next < now) {
            next += this.period;
        }
        return next;
    }

    public synchronized void schedule_next(long when) {
        this.next_due = when;
        UserError.Log.uel(TAG, "Scheduling next for: " + this.title + " to " + JoH.dateTimeText(this.next_due));
        if (alternating) alternate = !alternate;
        alerted_times = 0; // reset counter
        save();
    }

    public synchronized void schedule_next() {
        schedule_next(getPotentialNextSchedule());
    }

    protected synchronized static void fixUpTable(String[] schema) {
        if (patched) return;
        for (String patch : schema) {
            try {
                SQLiteUtils.execSql(patch);
            } catch (Exception e) {
                //
            }
        }
        patched = true;
    }

    public static Reminder create(String title, long period) {
        fixUpTable(schema);
        Reminder reminder = new Reminder();
        reminder.title = title;
        reminder.alternate_title = title + " alternate";
        reminder.period = period;
        reminder.next_due = JoH.tsl() + period;
        reminder.enabled = true;
        reminder.snoozed_till = 0;
        reminder.last_snoozed_for = 0;
        reminder.last_fired = 0;
        reminder.fired_times = 0;
        reminder.repeating = true;
        reminder.alternating = false;
        reminder.alternate = false;
        reminder.chime_only = false;
        reminder.homeonly = false;
        reminder.ideal_time = JoH.hourMinuteString();
        reminder.priority = 5; // default
        reminder.save();
        return reminder;
    }

    public static List<Reminder> getActiveReminders() {
        fixUpTable(schema);
        final long now = JoH.tsl();
        final List<Reminder> reminders = new Select()
                .from(Reminder.class)
                .where("enabled = ?", true)
                .where("next_due < ?", now)
                .where("snoozed_till < ?", now)
                .orderBy("enabled desc, next_due asc")
                .execute();
        return reminders;
    }

    private static boolean isNight() {
        final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour < 9; // midnight to 9am we say is night
    }

    public static synchronized void processAnyDueReminders() {
        if (JoH.quietratelimit("reminder_due_check", 10)) {
            if (!Pref.getBooleanDefaultFalse(REMINDERS_ALL_DISABLED)
                    && (!Pref.getBooleanDefaultFalse(REMINDERS_NIGHT_DISABLED) || !isNight())) {
                final Reminder due_reminder = getNextActiveReminder();
                if (due_reminder != null) {
                    UserError.Log.d(TAG, "Found due reminder! " + due_reminder.title);
                    due_reminder.reminder_alert();
                }
            } else {
                // reminders are disabled - should we re-enable them?
                if (Pref.getBooleanDefaultFalse(REMINDERS_RESTART_TOMORROW)) {
                    // temporary testing logic
                    final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    if (hour == 10) {
                        if (JoH.pratelimit("restart-reminders", 7200)) {
                            UserError.Log.d(TAG, "Re-enabling reminders as its morning time");
                            Pref.setBoolean(REMINDERS_ALL_DISABLED, false);
                        }
                    }
                }
            }
        }
    }

    public static Reminder getNextActiveReminder() {
        fixUpTable(schema);
        final boolean onHomeWifi = !HomeWifi.isSet() || HomeWifi.isConnected();
        final long now = JoH.tsl();
        final Reminder reminder = new Select()
                .from(Reminder.class)
                .where("enabled = ?", true)
                .where("next_due < ?", now)
                .where("snoozed_till < ?", now)
                .where("last_fired < (? - (600000 * alerted_times))", now)
                // if on home wifi or not set then anything otherwise only home only = false
                .where(onHomeWifi ? "homeonly > -1 " : "homeonly = 0")
                .orderBy("enabled desc, priority desc, next_due asc")
                .executeSingle();
        return reminder;
    }

    public static List<Reminder> getAllReminders() {
        fixUpTable(schema);
        final List<Reminder> reminders = new Select()
                .from(Reminder.class)
                .orderBy("enabled desc, priority desc, next_due asc")
                .execute();
        return reminders;
    }

    public synchronized static void firstInit(Context context) {
        fixUpTable(schema);
      /*  Inevitable.task("reminders-first-init", 2000, new Runnable() {
            @Override
            public void run() {
                try {
                    final Reminder reminder = new Select()
                            .from(Reminder.class)
                            .where("enabled = ?", true)
                            .executeSingle();
                    if (reminder != null) {
                        // PendingIntent serviceIntent = PendingIntent.getService(xdrip.getAppContext(), 0, new Intent(xdrip.getAppContext(), MissedReadingService.class), PendingIntent.FLAG_UPDATE_CURRENT);
                        // PendingIntent serviceIntent = WakeLockTrampoline.getPendingIntent(MissedReadingService.class);
                        //  JoH.wakeUpIntent(xdrip.getAppContext(), Constants.MINUTE_IN_MS, serviceIntent);
                        //  UserError.Log.ueh(TAG, "Starting missed readings service");
                    }
                } catch (NullPointerException e) {
                    UserError.Log.wtf(TAG, "Got nasty initial concurrency exception: " + e);
                }
            }
        });
        */
    }

    public static Reminder byid(long id) {
        return new Select()
                .from(Reminder.class)
                .where("_ID = ?", id)
                .executeSingle();
    }


    /*static Reminder getForPreciseTimestamp(double timestamp, double precision, String plugin) {
        fixUpTable(schema);
        final Reminder Reminder = new Select()
                .from(Reminder.class)
                .where("timestamp <= ?", (timestamp + precision))
                .where("timestamp >= ?", (timestamp - precision))
                .where("plugin = ?", plugin)
                .orderBy("abs(timestamp - " + timestamp + ") asc")
                .executeSingle();
        if (Reminder != null && Math.abs(Reminder.timestamp - timestamp) < precision) {
            return Reminder;
        }
        return null;
    }
*/
}
