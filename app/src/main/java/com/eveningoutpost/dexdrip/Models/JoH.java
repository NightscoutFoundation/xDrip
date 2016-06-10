package com.eveningoutpost.dexdrip.Models;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

/**
 * Created by jamorham on 06/01/16.
 * <p/>
 * lazy helper class for utilities
 */
public class JoH {
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final static String TAG = "jamorham JoH";
    private final static boolean debug_wakelocks = false;

    private static double benchmark_time = 0;
    private static Map<String, Double> benchmarks = new HashMap<String, Double>();
    private static final Map<String, Double> rateLimits = new HashMap<String, Double>();

    // qs = quick string conversion of double for printing
    public static String qs(double x) {
        return qs(x, 2);
    }

    public static String qs(double x, int digits) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#", symbols);
        df.setMaximumFractionDigits(digits);
        df.setMinimumIntegerDigits(1);
        return df.format(x);
    }

    public static double ts() {
        return new Date().getTime();
    }

    public static long tsl() {
        return new Date().getTime();
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String compressString(String source) {
        try {

            Deflater deflater = new Deflater();
            deflater.setInput(source.getBytes(Charset.forName("UTF-8")));
            deflater.finish();

            byte[] buf = new byte[source.length() + 256];
            int count = deflater.deflate(buf);
            // check count
            deflater.end();
            return Base64.encodeToString(buf, 0, count, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] compressStringToBytes(String string) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(string.length());
            GZIPOutputStream gzipped_data = new GZIPOutputStream(output);
            gzipped_data.write(string.getBytes(Charset.forName("UTF-8")));
            gzipped_data.close();
            byte[] compressed = output.toByteArray();
            output.close();
            return compressed;
        } catch (Exception e) {
            Log.e(TAG, "Exception in compress: " + e.toString());
            return new byte[0];
        }
    }

    public static byte[] compressBytesToBytes(byte[] bytes) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(bytes.length);
            GZIPOutputStream gzipped_data = new GZIPOutputStream(output);
            gzipped_data.write(bytes);
            gzipped_data.close();
            byte[] compressed = output.toByteArray();
            output.close();
            return compressed;
        } catch (Exception e) {
            Log.e(TAG, "Exception in compress: " + e.toString());
            return new byte[0];
        }
    }

    public static byte[] decompressBytesToBytes(byte[] bytes) {
        try {
            Log.d(TAG, "Decompressing  bytes size: " + bytes.length);
            byte[] buffer = new byte[8192];
            int bytes_read;
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            ByteArrayOutputStream output = new ByteArrayOutputStream(bytes.length);
            GZIPInputStream gzipped_data = new GZIPInputStream(input, buffer.length);
            while ((bytes_read = gzipped_data.read(buffer)) != -1) {
                output.write(buffer, 0, bytes_read);
            }
            gzipped_data.close();
            input.close();
            // output.close();
            return output.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "Exception in decompress: " + e.toString());
            return new byte[0];
        }
    }


    public static String uncompressString(String input) {
        try {
            byte[] bytes = Base64.decode(input, Base64.NO_WRAP);
            Inflater inflater = new Inflater();
            inflater.setInput(bytes);
            inflater.finished();

            byte[] buf = new byte[10000]; // max packet size because not using stream
            int count = inflater.inflate(buf);
            inflater.end();
            Log.d(TAG, "Inflated bytes: " + count);
            return new String(buf, 0, count, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "Got exception uncompressing string");
            return null;
        }
    }

    public static String ucFirst(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    public static class DecimalKeyListener extends DigitsKeyListener {
        private final char[] acceptedCharacters =
                new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                        new DecimalFormat().getDecimalFormatSymbols().getDecimalSeparator()};

        @Override
        protected char[] getAcceptedChars() {
            return acceptedCharacters;
        }

        public int getInputType() {
            return InputType.TYPE_CLASS_NUMBER;
        }

    }

    public static String backTrace() {
        return backTrace(1);
    }

    public static String backTrace(int depth) {
        try {
            StackTraceElement stack = new Exception().getStackTrace()[2 + depth];
            StackTraceElement stackb = new Exception().getStackTrace()[3 + depth];
            String[] stackclassa = stack.getClassName().split("\\.");
            String[] stackbclassa = stackb.getClassName().split("\\.");

            return stackbclassa[stackbclassa.length - 1] + "::" + stackb.getMethodName()
                    + " -> " + stackclassa[stackclassa.length - 1] + "::" + stack.getMethodName();
        } catch (Exception e) {
            return "unknown backtrace: " + e.toString();
        }
    }

    public static String backTraceShort(int depth) {
        try {
            StackTraceElement stackb = new Exception().getStackTrace()[3 + depth];
            return stackb.getMethodName();
        } catch (Exception e) {
            return "unknown backtrace: " + e.toString();
        }
    }

    public static void benchmark(String name) {
        if (name == null) {
            if (benchmark_time == 0) {
                benchmark_time = ts();
            } else {
                Log.e(TAG, "Cannot start a benchmark as one is already running - cancelling");
                benchmark_time = 0;
            }
        } else {
            if (benchmark_time == 0) {
                Log.e(TAG, "Benchmark: " + name + " no benchmark set!");
            } else {
                Log.i(TAG, "Benchmark: " + name + " " + (ts() - benchmark_time) + " ms");
                benchmark_time = 0;
            }
        }
    }

    // return true if below rate limit
    public static synchronized boolean ratelimit(String name, int seconds)
    {
            // check if over limit
            if ((rateLimits.containsKey(name)) && (JoH.ts()-rateLimits.get(name)<(seconds*1000))) {
                Log.d(TAG,name+" rate limited: "+seconds+" seconds");
                return false;
            }
            // not over limit
            rateLimits.put(name,JoH.ts());
            return true;
    }

    // return true if below rate limit
    public static synchronized boolean ratelimitmilli(String name, int milliseconds)
    {
        // check if over limit
        if ((rateLimits.containsKey(name)) && (JoH.ts()-rateLimits.get(name)<(milliseconds))) {
            Log.d(TAG,name+" rate limited: "+milliseconds+" milliseconds");
            return false;
        }
        // not over limit
        rateLimits.put(name,JoH.ts());
        return true;
    }

    public static boolean getWifiSleepPolicyNever()
    {
        try {
            int policy = Settings.Global.getInt(xdrip.getAppContext().getContentResolver(), android.provider.Settings.Global.WIFI_SLEEP_POLICY);
            Log.d(TAG,"Current WifiPolicy: "+ ((policy == Settings.Global.WIFI_SLEEP_POLICY_NEVER) ? "Never" : Integer.toString(policy))+" "+Settings.Global.WIFI_SLEEP_POLICY_DEFAULT+" "+Settings.Global.WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED);
            return (policy == Settings.Global.WIFI_SLEEP_POLICY_NEVER);
        } catch (Exception e) {
            Log.e(TAG,"Exception during global settings policy");
            return true; // we don't know anything
        }
    }

    public static void benchmark_method_start() {
        benchmarks.put(backTrace(0), ts());
    }

    public static void benchmark_method_end() {
        String name = backTrace(0);
        try {

            double timing = ts() - benchmarks.get(name);
            Log.i(TAG, "Benchmark: " + name + " " + timing + "ms");
        } catch (Exception e) {
            Log.e(TAG, "Benchmark: " + name + " no benchmark set!");
        }
    }

    public static void fixActionBar(AppCompatActivity context) {
        try {
            context.getSupportActionBar().setDisplayShowHomeEnabled(true);
            context.getSupportActionBar().setIcon(R.drawable.ic_launcher);
        } catch (Exception e) {
            Log.e(TAG, "Got exception with supportactionbar: " + e.toString());

        }
    }

    public static HashMap<String, Object> JsonStringtoMap(String json) {
        return new Gson().fromJson(json, new TypeToken<HashMap<String, Object>>() {
        }.getType());
    }

    public static String hourMinuteString()
    {
        Date date = new Date();
        SimpleDateFormat sd = new SimpleDateFormat("HH:mm");
        return sd.format(date);
    }

    public static String dateTimeText(long timestamp)
    {
        return android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", timestamp).toString();
    }

    public static double tolerantParseDouble(String str) throws NumberFormatException {
        return Double.parseDouble(str.replace(",", "."));

    }

    public static PowerManager.WakeLock getWakeLock(final String name, int millis) {
        final PowerManager pm = (PowerManager) xdrip.getAppContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, name);
        wl.acquire(millis);
        if (debug_wakelocks) Log.d(TAG,"getWakeLock: "+name+" "+wl.toString());
        return wl;
    }

    public static void releaseWakeLock(PowerManager.WakeLock wl)
    {
        if (debug_wakelocks) Log.d(TAG,"releaseWakeLock: "+wl.toString());
        if (wl.isHeld()) wl.release();
    }

    public static boolean isLANConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) xdrip.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        final boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        return isConnected && ((activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) || (activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET));
    }
    public static boolean isMobileDataOrEthernetConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) xdrip.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        final boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        return isConnected && ((activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) || (activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET));
    }

    public static void static_toast(final Context context, final String msg, final int length) {
        try {
            Activity activity = (Activity) context;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, length).show();
                }
            });
            Log.d(TAG, "Toast msg: " + msg);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't display toast: " + msg + " e: "+e.toString());
            Home.toaststatic(msg);
        }
    }

}
