package com.eveningoutpost.dexdrip.Models;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.Reminders;
import com.eveningoutpost.dexdrip.Services.MissedReadingService;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.annotations.Expose;

import java.util.List;


/**
 * Created by jamorham on 01/02/2017.
 */

@Table(name = "Reminder", id = BaseColumns._ID)
public class Reminder extends Model {


    private static final String TAG = "Reminder";
    private static boolean patched = false;
    private static final String[] schema = {
            "CREATE TABLE Reminder (_id INTEGER PRIMARY KEY AUTOINCREMENT)",
            "ALTER TABLE Reminder ADD COLUMN next_due INTEGER",
            "ALTER TABLE Reminder ADD COLUMN period INTEGER",
            "ALTER TABLE Reminder ADD COLUMN snoozed_till INTEGER",
            "ALTER TABLE Reminder ADD COLUMN last_snoozed_for INTEGER",
            "ALTER TABLE Reminder ADD COLUMN last_fired INTEGER",
            "ALTER TABLE Reminder ADD COLUMN fired_times INTEGER",
            "ALTER TABLE Reminder ADD COLUMN title TEXT",
            "ALTER TABLE Reminder ADD COLUMN alt_title TEXT",
            "ALTER TABLE Reminder ADD COLUMN sound_uri TEXT",
            "ALTER TABLE Reminder ADD COLUMN ideal_time TEXT",
            "ALTER TABLE Reminder ADD COLUMN priority INTEGER",
            "ALTER TABLE Reminder ADD COLUMN enabled INTEGER",
            "ALTER TABLE Reminder ADD COLUMN repeating INTEGER",
            "ALTER TABLE Reminder ADD COLUMN alternating INTEGER",
            "ALTER TABLE Reminder ADD COLUMN alternate INTEGER",
            "ALTER TABLE Reminder ADD COLUMN chime INTEGER",
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
        if (next_due <= JoH.tsl()) {
            return true;
        } else {
            return false;
        }
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

    public synchronized void schedule_next() {
        this.next_due = this.next_due + this.period;
        // check it is actually in the future
        while (this.next_due < JoH.tsl()) {
            this.next_due = this.next_due + this.period;
        }
        if (alternating) alternate = !alternate;
        save();
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
                .orderBy("priority desc, next_due asc")
                .execute();
        return reminders;
    }

    public static synchronized void processAnyDueReminders() {
        if (JoH.quietratelimit("reminder_due_check", 10)) {
            Reminder due_reminder = getNextActiveReminder();
            if (due_reminder != null) {
                UserError.Log.d(TAG, "Found due reminder! " + due_reminder.title);
                due_reminder.reminder_alert();
            }
        }
    }

    public static Reminder getNextActiveReminder() {
        fixUpTable(schema);
        final long now = JoH.tsl();
        final Reminder reminder = new Select()
                .from(Reminder.class)
                .where("enabled = ?", true)
                .where("next_due < ?", now)
                .where("snoozed_till < ?", now)
                .orderBy("priority desc, next_due asc")
                .executeSingle();
        return reminder;
    }

    public static List<Reminder> getAllReminders() {
        fixUpTable(schema);
        final List<Reminder> reminders = new Select()
                .from(Reminder.class)
                .orderBy("priority desc, next_due asc")
                .execute();
        return reminders;
    }

    public synchronized static void firstInit(Context context) {
        fixUpTable(schema);
        final Reminder reminder = new Select()
                .from(Reminder.class)
                .where("enabled = ?", true)
                .executeSingle();
        if (reminder != null) {
            PendingIntent serviceIntent = PendingIntent.getService(xdrip.getAppContext(), 0, new Intent(xdrip.getAppContext(), MissedReadingService.class), 0);
            JoH.wakeUpIntent(xdrip.getAppContext(), Constants.MINUTE_IN_MS, serviceIntent);
            UserError.Log.d(TAG, "Starting missed readings service");
        }
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
