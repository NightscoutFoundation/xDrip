package com.eveningoutpost.dexdrip.Models.usererror;


import android.util.Log;


public class UserErrorLog {
    private final static UserErrorStore store = UserErrorStore.get();
    static ExtraLogTags extraLogTags = new ExtraLogTags();

    public static void e(String a, String b){
        Log.e(a, b);
        store.createError(a, b);
    }

    public static void e(String tag, String b, Exception e){
        Log.e(tag, b, e);
        store.createError(tag, b + "\n" + e.toString());
    }

    public static void w(String tag, String b){
        Log.w(tag, b);
        store.createLow(tag, b);
    }
    public static void w(String tag, String b, Exception e){
        Log.w(tag, b, e);
        store.createLow(tag, b + "\n" + e.toString());
    }
    public static void wtf(String tag, String b){
        Log.wtf(tag, b);
        store.createHigh(tag, b);
    }
    public static void wtf(String tag, String b, Exception e){
        Log.wtf(tag, b, e);
        store.createHigh(tag, b + "\n" + e.toString());
    }
    public static void wtf(String tag, Exception e){
        Log.wtf(tag, e);
        store.createHigh(tag, e.toString());
    }

    public static void uel(String tag, String b) {
        Log.i(tag, b);
        store.createLow(tag, b);
    }

    public static void ueh(String tag, String b) {
        Log.i(tag, b);
        store.createMajor(tag, b);
    }

    public static void d(String tag, String b){
        Log.d(tag, b);
        if(ExtraLogTags.shouldLogTag(tag, Log.DEBUG)) {
            store.createLow(tag, b);
        }
    }

    public static void v(String tag, String b){
        Log.v(tag, b);
        if(ExtraLogTags.shouldLogTag(tag, Log.VERBOSE)) {
            store.createLow(tag, b);
        }
    }

    public static void i(String tag, String b){
        Log.i(tag, b);
        if(ExtraLogTags.shouldLogTag(tag, Log.INFO)) {
            store.createLow(tag, b);
        }
    }


}


