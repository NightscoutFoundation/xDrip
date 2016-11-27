package com.eveningoutpost.dexdrip.Models;

import android.os.AsyncTask;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.bugfender.sdk.Bugfender;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by stephenblack on 8/3/15.
 */

@Table(name = "UserErrors", id = BaseColumns._ID)
public class UserError extends Model {

    private final static String TAG = UserError.class.getSimpleName();
    
    @Column(name = "shortError")
    public String shortError; // Short error message to be displayed on table

    @Column(name = "message")
    public String message; // Additional text when error is expanded

    @Column(name = "severity", index = true)
    public int severity; // int between 1 and 3, 3 being most severe

    // 5 = internal lower level user events
    // 6 = higher granularity user events

    @Column(name = "timestamp", index = true)
    public double timestamp; // Time the error was raised

    //todo: rather than include multiples of the same error, should we have a "Count" and just increase that on duplicates?
    //or rather, perhaps we should group up the errors

    public String toString()
    {
        return severity+" ^ "+JoH.dateTimeText((long)timestamp)+" ^ "+shortError+" ^ "+message;
    }

    public UserError() {}

    public UserError(int severity, String shortError, String message) {
        this.severity = severity;
        this.shortError = shortError;
        this.message = message;
        this.timestamp = new Date().getTime();
        this.save();
        if (xdrip.useBF) {
            switch (severity) {
                case 2:
                case 3:
                    Bugfender.e(shortError, message);
                    break;
                case 5:
                case 6:
                    Bugfender.w(shortError, message);
                    break;
                default:
                    Bugfender.d(shortError, message);
                    break;
            }
        }
    }

    public UserError(String shortError, String message) {
        this(2, shortError, message);
    }

    public static UserError UserErrorHigh(String shortError, String message) {
        return new UserError(3, shortError, message);
    }

    public static UserError UserErrorLow(String shortError, String message) {
        return new UserError(1, shortError, message);
    }

    public static UserError UserEventLow(String shortError, String message) {
        return new UserError(5, shortError, message);
    }

    public static UserError UserEventHigh(String shortError, String message) {
        return new UserError(6, shortError, message);
    }


    public static void cleanup() {
       new Cleanup().execute(deletable());
    }

    public static List<UserError> all() {
        return new Select()
                .from(UserError.class)
                .orderBy("timestamp desc")
                .execute();
    }

    public static List<UserError> deletable() {
        List<UserError> userErrors = new Select()
                .from(UserError.class)
                .where("severity < ?", 3)
                .where("timestamp < ?", (new Date().getTime() - 1000 * 60 * 60 * 24))
                .orderBy("timestamp desc")
                .execute();
        List<UserError> highErrors = new Select()
                .from(UserError.class)
                .where("severity = ?", 3)
                .where("timestamp < ?", (new Date().getTime() - 1000*60*60*24*3))
                .orderBy("timestamp desc")
                .execute();
        List<UserError> events = new Select()
                .from(UserError.class)
                .where("severity > ?", 3)
                .where("timestamp < ?", (new Date().getTime() - 1000*60*60*24*7))
                .orderBy("timestamp desc")
                .execute();
        userErrors.addAll(highErrors);
        userErrors.addAll(events);
        return userErrors;
    }

    public static List<UserError> bySeverity(Integer[] levels) {
        String levelsString = " ";
        for (int level : levels) {
            levelsString += level + ",";
        }
        Log.d("UserError", "severity in ("+levelsString.substring(0,levelsString.length() - 1)+")");
        return new Select()
                .from(UserError.class)
                .where("severity in ("+levelsString.substring(0,levelsString.length() - 1)+")")
                .orderBy("timestamp desc")
                .execute();
    }

    private static class Cleanup extends AsyncTask<List<UserError>, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(List<UserError>... errors) {
            try {
                for(UserError userError : errors[0]) {
                    userError.delete();
                }
                return true;
            } catch(Exception e) {
                return false;
            }
        }
    }

    public static List<UserError> bySeverity(int level) {
        return bySeverity(new Integer[]{level});
    }
    public static List<UserError> bySeverity(int level, int level2) {
        return bySeverity(new Integer[]{ level, level2 });
    }
    public static List<UserError> bySeverity(int level, int level2, int level3) {
        return bySeverity(new Integer[]{ level, level2, level3 });
    }


    public static class Log {
        public static void e(String a, String b){
            android.util.Log.e(a, b);
            new UserError(a, b);
        }

        public static void e(String tag, String b, Exception e){
            android.util.Log.e(tag, b, e);
            new UserError(tag, b + "\n" + e.toString());
        }

        public static void w(String tag, String b){
            android.util.Log.w(tag, b);
            UserError.UserErrorLow(tag, b);
        }
        public static void w(String tag, String b, Exception e){
            android.util.Log.w(tag, b, e);
            UserError.UserErrorLow(tag, b + "\n" + e.toString());
        }
        public static void wtf(String tag, String b){
            android.util.Log.wtf(tag, b);
            UserError.UserErrorHigh(tag, b);
        }
        public static void wtf(String tag, String b, Exception e){
            android.util.Log.wtf(tag, b, e);
            UserError.UserErrorHigh(tag, b + "\n" + e.toString());
        }
        public static void wtf(String tag, Exception e){
            android.util.Log.wtf(tag, e);
            UserError.UserErrorHigh(tag, e.toString());
        }

        public static void uel(String tag, String b) {
            android.util.Log.i(tag, b);
            UserError.UserEventLow(tag, b);
        }

        public static void ueh(String tag, String b) {
            android.util.Log.i(tag, b);
            UserError.UserEventHigh(tag, b);
        }

        public static void d(String tag, String b){
            android.util.Log.d(tag, b);
            if(ExtraLogTags.shouldLogTag(tag, android.util.Log.DEBUG)) {
                UserErrorLow(tag, b);
            }
        }

        public static void v(String tag, String b){
            android.util.Log.v(tag, b);
            if(ExtraLogTags.shouldLogTag(tag, android.util.Log.VERBOSE)) {
                UserErrorLow(tag, b);
            }           
        }

        public static void i(String tag, String b){
            android.util.Log.i(tag, b);
            if(ExtraLogTags.shouldLogTag(tag, android.util.Log.INFO)) {
                UserErrorLow(tag, b);
            }
        }
        
        static ExtraLogTags extraLogTags = new ExtraLogTags();
    }
    
    public static class ExtraLogTags {

        static Hashtable <String, Integer> extraTags;
        ExtraLogTags () {
            extraTags = new Hashtable <String, Integer>();
            String extraLogs = Home.getPreferencesStringDefaultBlank("extra_tags_for_logging");
            readPreference(extraLogs);
        }
        
        /*
         * This function reads a string representing tags that the user wants to log
         * Format of string is tag1:level1,tag2,level2
         * Example of string is Alerts:i,BG:W
         * 
         */
        public static void readPreference(String extraLogs) {
            if (extraLogs.length() > 0) UserErrorLow(TAG, "called with string " + extraLogs);
            extraTags.clear();
            
            String []tags = extraLogs.split(",");
            if(tags.length == 0) {
                return;
            }
            
            // go over all tags and parse them
            for(String tag : tags) {
                if (tag.length() > 0) parseTag(tag);
            }
        }
        
        static void parseTag(String tag) {
            // Format is tag:level for example  Alerts:i
            String[] tagAndLevel = tag.split(":");
            if(tagAndLevel.length != 2) {
                Log.e(TAG, "Failed to parse " + tag);
                return;
            }
            String level =  tagAndLevel[1];
            String tagName = tagAndLevel[0].toLowerCase();
            if (level.compareTo("d") == 0) {
                extraTags.put(tagName, android.util.Log.DEBUG);
                UserErrorLow(TAG, "Adding tag with DEBUG " + tagAndLevel[0] );
                return;
            }
            if (level.compareTo("v") == 0) {
                extraTags.put(tagName, android.util.Log.VERBOSE);
                UserErrorLow(TAG,"Adding tag with VERBOSE " + tagAndLevel[0] );
                return;
            }
            if (level.compareTo("i") == 0) {
                extraTags.put(tagName, android.util.Log.INFO);
                UserErrorLow(TAG, "Adding tag with info " + tagAndLevel[0] );
                return;
            }
            Log.e(TAG, "Unknown level for tag " + tag + " please use d v or i");

        }
        
        static boolean shouldLogTag(String tag, int level) {
            Integer levelForTag = extraTags.get(tag.toLowerCase());
            if(levelForTag == null) {
                return false;
            }
            
            if(level >= levelForTag) {
                return true;
            }
            
            return false;
        }
        
    }
}
