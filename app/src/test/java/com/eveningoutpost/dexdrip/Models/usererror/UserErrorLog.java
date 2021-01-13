package com.eveningoutpost.dexdrip.Models.usererror;

import android.util.Log;

/**
 * While I do approve of the use of Mockito and similar, an oddity such as UserError was and
 * to some extent still is as it exists everywhere in the entire codebase, needs to be removed.
 * Using mock classes like this is generally less prone to errors. But, then again. What do I
 * know.
 */
public class UserErrorLog {

    public static void e(String a, String b){
        Log.e(a, b);
    }

    public static void e(String tag, String b, Exception e){
        Log.e(tag, b, e);
    }

    public static void w(String tag, String b){
        Log.w(tag, b);
    }
    public static void w(String tag, String b, Exception e){
        Log.w(tag, b, e);
    }
    public static void wtf(String tag, String b){
        Log.wtf(tag, b);
    }
    public static void wtf(String tag, String b, Exception e){
        Log.wtf(tag, b, e);
    }
    public static void wtf(String tag, Exception e){
        Log.wtf(tag, e);
    }

    public static void uel(String tag, String b) {
        Log.i(tag, b);
    }

    public static void ueh(String tag, String b) {
        Log.i(tag, b);
    }

    public static void d(String tag, String b){
        Log.d(tag, b);
    }

    public static void v(String tag, String b){
        Log.v(tag, b);
    }

    public static void i(String tag, String b){
        Log.i(tag, b);
    }
}
