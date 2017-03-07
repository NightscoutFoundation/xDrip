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
            "ALTER TABLE Reminder ADD COLUMN sound_uri TEXT",
            "ALTER TABLE Reminder ADD COLUMN ideal_time TEXT",
            "ALTER TABLE Reminder ADD COLUMN priority INTEGER",
            "ALTER TABLE Reminder ADD COLUMN enabled INTEGER",
            "ALTER TABLE Reminder ADD COLUMN repeating INTEGER",
            "CREATE INDEX index_Reminder_timestamp on Reminder(timestamp)",
            "CREATE INDEX index_Reminder_snoozed_till on Reminder(snoozed_till)"
    };

    @Expose
    @Column(name = "title")
    public String title;

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
        last_fired = JoH.tsl();
        if (last_fired < next_due) fired_times++;
        save();
    }

    public synchronized void reminder_alert() {
        Reminders.doAlert(this);
        notified();
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
        reminder.period = period;
        reminder.next_due = JoH.tsl() + period;
        reminder.enabled = true;
        reminder.snoozed_till = 0;
        reminder.last_snoozed_for = 0;
        reminder.last_fired = 0;
        reminder.fired_times = 0;
        reminder.repeating = true;
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

    public static void processAnyDueReminders() {
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
