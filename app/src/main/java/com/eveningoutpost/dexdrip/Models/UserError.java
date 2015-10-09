package com.eveningoutpost.dexdrip.Models;

import android.os.AsyncTask;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.Date;
import java.util.List;

/**
 * Created by stephenblack on 8/3/15.
 */

@Table(name = "UserErrors", id = BaseColumns._ID)
public class UserError extends Model {

    @Column(name = "shortError")
    public String shortError; // Short error message to be displayed on table

    @Column(name = "message")
    public String message; // Additional text when error is expanded

    @Column(name = "severity", index = true)
    public int severity; // int between 1 and 3, 3 being most severe

    @Column(name = "timestamp", index = true)
    public double timestamp; // Time the error was raised

    //todo: rather than include multiples of the same error, should we have a "Count" and just increase that on duplicates?
    //or rather, perhaps we should group up the errors


    public UserError() {}

    public UserError(int severity, String shortError, String message) {
        this.severity = severity;
        this.shortError = shortError;
        this.message = message;
        this.timestamp = new Date().getTime();
        this.save();
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
        userErrors.addAll(highErrors);
        return userErrors;
    }

    public static List<UserError> bySeverity(Integer[] levels) {
        String levelsString = " ";
        for (int level : levels) {
            levelsString += level + ",";
        }
        Log.d("TEST", "severity in ("+levelsString.substring(0,levelsString.length() - 1)+")");
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

        public static void e(String a, String b, Exception e){
            android.util.Log.e(a, b, e);
            new UserError(a, b + "\n" + e.toString());
        }

        public static void w(String a, String b){
            android.util.Log.w(a, b);
            UserError.UserErrorLow(a, b);
        }
        public static void w(String a, String b, Exception e){
            android.util.Log.w(a, b, e);
            UserError.UserErrorLow(a, b + "\n" + e.toString());
        }
        public static void wtf(String a, String b){
            android.util.Log.wtf(a, b);
            UserError.UserErrorHigh(a, b);
        }
        public static void wtf(String a, String b, Exception e){
            android.util.Log.wtf(a, b, e);
            UserError.UserErrorHigh(a, b + "\n" + e.toString());
        }
        public static void wtf(String a, Exception e){
            android.util.Log.wtf(a, e);
            UserError.UserErrorHigh(a, e.toString());
        }
        public static void d(String a, String b){
            android.util.Log.d(a, b);
        }

        public static void v(String a, String b){
            android.util.Log.v(a, b);
        }

        public static void i(String a, String b){
            android.util.Log.i(a, b);
        }
    }
}
