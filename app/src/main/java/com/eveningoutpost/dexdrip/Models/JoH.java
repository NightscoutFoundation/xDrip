package com.eveningoutpost.dexdrip.Models;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.utils.BestGZIPOutputStream;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

import static com.eveningoutpost.dexdrip.stats.StatsActivity.SHOW_STATISTICS_PRINT_COLOR;

/**
 * Created by jamorham on 06/01/16.
 * <p>
 * lazy helper class for utilities
 */
public class JoH {
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final static String TAG = "jamorham JoH";
    private final static boolean debug_wakelocks = false;

    private static double benchmark_time = 0;
    private static Map<String, Double> benchmarks = new HashMap<String, Double>();
    private static final Map<String, Long> rateLimits = new HashMap<String, Long>();

    // qs = quick string conversion of double for printing
    public static String qs(double x) {
        return qs(x, 2);
    }

    public static String qs(double x, int digits) {

        if (digits == -1) {
            digits = 0;
            if (((int) x != x)) {
                digits++;
                if ((((int) x * 10) / 10 != x)) {
                    digits++;
                    if ((((int) x * 100) / 100 != x)) digits++;
                }
            }
        }

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

    // TODO can we optimize this with System.currentTimeMillis ?
    public static long tsl() {
        return new Date().getTime();
    }

    public static long msSince(long when) {
        return (tsl() - when);
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

    public static byte[] hexStringToByteArray(String str) {
        try {
            str = str.toUpperCase().trim();
            if (str.length() == 0) return null;
            final int len = str.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
            }
            return data;
        } catch (Exception e) {
            Log.e(TAG, "Exception processing hexString: " + e);
            return null;
        }
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
            BestGZIPOutputStream gzipped_data = new BestGZIPOutputStream(output);
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
            BestGZIPOutputStream gzipped_data = new BestGZIPOutputStream(output);
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

    public static String base64encode(String input) {
        try {
            return new String(Base64.encode(input.getBytes("UTF-8"), Base64.NO_WRAP), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Got unsupported encoding: " + e);
            return "encode-error";
        }
    }

    public static String base64decode(String input) {
        try {
            return new String(Base64.decode(input.getBytes("UTF-8"), Base64.NO_WRAP), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Got unsupported encoding: " + e);
            return "decode-error";
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
            final StackTraceElement stackb = new Exception().getStackTrace()[3 + depth];
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

    // return true if below rate limit (persistent version)
    public static synchronized boolean pratelimit(String name, int seconds) {
        // check if over limit
        final long time_now = JoH.tsl();
        final long rate_time;
        if (!rateLimits.containsKey(name)) {
            rate_time = PersistentStore.getLong(name); // 0 if undef
        } else {
            rate_time = rateLimits.get(name);
        }
        if ((rate_time > 0) && (time_now - rate_time) < (seconds * 1000)) {
            Log.d(TAG, name + " rate limited: " + seconds + " seconds");
            return false;
        }
        // not over limit
        rateLimits.put(name, time_now);
        PersistentStore.setLong(name, time_now);
        return true;
    }

    // return true if below rate limit
    public static synchronized boolean ratelimit(String name, int seconds) {
        // check if over limit
        if ((rateLimits.containsKey(name)) && (JoH.tsl() - rateLimits.get(name) < (seconds * 1000))) {
            Log.d(TAG, name + " rate limited: " + seconds + " seconds");
            return false;
        }
        // not over limit
        rateLimits.put(name, JoH.tsl());
        return true;
    }

    // return true if below rate limit
    public static synchronized boolean ratelimitmilli(String name, int milliseconds) {
        // check if over limit
        if ((rateLimits.containsKey(name)) && (JoH.tsl() - rateLimits.get(name) < (milliseconds))) {
            Log.d(TAG, name + " rate limited: " + milliseconds + " milliseconds");
            return false;
        }
        // not over limit
        rateLimits.put(name, JoH.tsl());
        return true;
    }

    public static String getDeviceDetails() {
        final String manufacturer = Build.MANUFACTURER.replace(" ", "_");
        final String model = Build.MODEL.replace(" ", "_");
        final String version = Integer.toString(Build.VERSION.SDK_INT) + " " + Build.VERSION.RELEASE + " " + Build.VERSION.INCREMENTAL;
        return manufacturer + " " + model + " " + version;
    }

    public static String getVersionDetails() {
        try {
            return xdrip.getAppContext().getPackageManager().getPackageInfo(xdrip.getAppContext().getPackageName(), PackageManager.GET_META_DATA).versionName;
        } catch (Exception e) {
            return "Unknown version";
        }
    }

    public static boolean isOldVersion(Context context) {
        try {
            final Signature[] pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
            if (pinfo.length == 1) {
                final Checksum s = new CRC32();
                final byte[] ba = pinfo[0].toByteArray();
                s.update(ba, 0, ba.length);
                if (s.getValue() == 2009579833) return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "exception: " + e);
        }
        return false;
    }

    public static boolean getWifiSleepPolicyNever() {
        try {
            int policy = Settings.Global.getInt(xdrip.getAppContext().getContentResolver(), android.provider.Settings.Global.WIFI_SLEEP_POLICY);
            Log.d(TAG, "Current WifiPolicy: " + ((policy == Settings.Global.WIFI_SLEEP_POLICY_NEVER) ? "Never" : Integer.toString(policy)) + " " + Settings.Global.WIFI_SLEEP_POLICY_DEFAULT + " " + Settings.Global.WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED);
            return (policy == Settings.Global.WIFI_SLEEP_POLICY_NEVER);
        } catch (Exception e) {
            Log.e(TAG, "Exception during global settings policy");
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

    public static String hourMinuteString() {
        Date date = new Date();
        SimpleDateFormat sd = new SimpleDateFormat("HH:mm");
        return sd.format(date);
    }

    public static String hourMinuteString(long timestamp) {
        return android.text.format.DateFormat.format("kk:mm", timestamp).toString();
    }

    public static String dateTimeText(long timestamp) {
        return android.text.format.DateFormat.format("yyyy-MM-dd kk:mm:ss", timestamp).toString();
    }

    public static String dateText(long timestamp) {
        return android.text.format.DateFormat.format("yyyy-MM-dd", timestamp).toString();
    }

    public static double tolerantParseDouble(String str) throws NumberFormatException {
        return Double.parseDouble(str.replace(",", "."));

    }

    public static PowerManager.WakeLock getWakeLock(final String name, int millis) {
        final PowerManager pm = (PowerManager) xdrip.getAppContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, name);
        wl.acquire(millis);
        if (debug_wakelocks) Log.d(TAG, "getWakeLock: " + name + " " + wl.toString());
        return wl;
    }

    public static void releaseWakeLock(PowerManager.WakeLock wl) {
        if (debug_wakelocks) Log.d(TAG, "releaseWakeLock: " + wl.toString());
        if (wl.isHeld()) wl.release();
    }

    public static boolean isLANConnected() {
        final ConnectivityManager cm =
                (ConnectivityManager) xdrip.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        final boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        return isConnected && ((activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                || (activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET)
                || (activeNetwork.getType() == ConnectivityManager.TYPE_BLUETOOTH));
    }

    public static boolean isMobileDataOrEthernetConnected() {
        final ConnectivityManager cm =
                (ConnectivityManager) xdrip.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        final boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        return isConnected && ((activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) || (activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET));
    }

    public static boolean isAnyNetworkConnected() {
        final ConnectivityManager cm =
                (ConnectivityManager) xdrip.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnected();
    }

    public static boolean isScreenOn() {
        final PowerManager pm = (PowerManager) xdrip.getAppContext().getSystemService(Context.POWER_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return pm.isInteractive();
        } else {
            return pm.isScreenOn();
        }
    }

    public static boolean isOngoingCall() {
        try {
            AudioManager manager = (AudioManager) xdrip.getAppContext().getSystemService(Context.AUDIO_SERVICE);
            return (manager.getMode() == AudioManager.MODE_IN_CALL);
            // possibly should have MODE_IN_COMMUNICATION as well
        } catch (Exception e) {
            return false;
        }
    }

    public static String getWifiSSID() {
        try {
            final WifiManager wifi_manager = (WifiManager) xdrip.getAppContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi_manager.isWifiEnabled()) {
                final WifiInfo wifiInfo = wifi_manager.getConnectionInfo();
                if (wifiInfo != null) {
                    final NetworkInfo.DetailedState wifi_state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                    if (wifi_state == NetworkInfo.DetailedState.CONNECTED
                            || wifi_state == NetworkInfo.DetailedState.OBTAINING_IPADDR
                            || wifi_state == NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK) {
                        String ssid = wifiInfo.getSSID();
                        if (ssid.equals("<unknown ssid>")) return null; // WifiSsid.NONE;
                        if (ssid.charAt(0)=='"') ssid=ssid.substring(1);
                        if (ssid.charAt(ssid.length()-1)=='"') ssid=ssid.substring(0,ssid.length()-1);
                        return ssid;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception in getWifiSSID: " + e);
        }
        return null;
    }

    public static boolean getWifiFuzzyMatch(String local, String remote) {
        if ((local == null) || (remote == null) || (local.length() == 0) || (remote.length() == 0))
            return false;
        final int slen = Math.min(local.length(), remote.length());
        final int llen = Math.max(local.length(), remote.length());
        int matched = 0;
        for (int i = 0; i < slen; i++) {
            if (local.charAt(i) == (remote.charAt(i))) matched++;
        }
        boolean result = false;
        if (matched == slen) result = true; // shorter string is substring
        final double quota = (double) matched / (double) llen;
        final int dmatch = llen - matched;
        if (slen > 2) {
            if (dmatch < 3) result = true;
            if (quota > 0.80) result = true;
        }
        //Log.d(TAG, "l:" + local + " r:" + remote + " slen:" + slen + " llen:" + llen + " matched:" + matched + "  q:" + JoH.qs(quota, 2) + "  dm:" + dmatch + " RESULT: " + result);
        return result;
    }

    public static boolean runOnUiThread(Runnable theRunnable) {
        final Handler mainHandler = new Handler(xdrip.getAppContext().getMainLooper());
        return mainHandler.post(theRunnable);
    }

    public static boolean runOnUiThreadDelayed(Runnable theRunnable, long delay) {
        final Handler mainHandler = new Handler(xdrip.getAppContext().getMainLooper());
        return mainHandler.postDelayed(theRunnable, delay);
    }

    public static void static_toast(final Context context, final String msg, final int length) {
        try {
            if (!runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Toast.makeText(context, msg, length).show();
                        Log.i(TAG, "Displaying toast using fallback");
                    } catch (Exception e) {
                        Log.e(TAG, "Exception processing runnable toast ui thread: " + e);
                        Home.toaststatic(msg);
                    }
                }
            })) {
                Log.e(TAG, "Couldn't display toast via ui thread: " + msg);
                Home.toaststatic(msg);
            }
        } catch (Exception e) {
            Log.e(TAG, "Couldn't display toast due to exception: " + msg + " e: " + e.toString());
            Home.toaststatic(msg);
        }
    }

    public static void static_toast_long(final String msg) {
        static_toast(xdrip.getAppContext(), msg, Toast.LENGTH_LONG);
    }

    public static void static_toast_short(final String msg) {
        static_toast(xdrip.getAppContext(), msg, Toast.LENGTH_SHORT);
    }

    public static void static_toast_long(Context context, final String msg) {
        static_toast(context, msg, Toast.LENGTH_LONG);
    }

    public static String urlEncode(String source) {
        try {
            return URLEncoder.encode(source, "UTF-8");
        } catch (Exception e) {
            return "encoding-exception";
        }
    }

    public static Object cloneObject(Object obj) {
        try {
            Object clone = obj.getClass().newInstance();
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                field.set(clone, field.get(obj));
            }
            return clone;
        } catch (Exception e) {
            return null;
        }
    }

    public static void goFullScreen(boolean fullScreen, View decorView) {

        if (fullScreen) {
            if (Build.VERSION.SDK_INT >= 19) {
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        } else {
            decorView.setSystemUiVisibility(0); // TODO will this need revisiting in later android vers?
        }
    }


    public static Bitmap screenShot(View view, String annotation) {

        if (view == null) {
            static_toast_long("View is null in screenshot!");
            return null;
        }
        final int width = view.getWidth();
        final int height = view.getHeight();
        Log.d(TAG, "Screenshot called: " + width + "," + height);
        final Bitmap bitmap = Bitmap.createBitmap(width,
                height, Bitmap.Config.ARGB_8888);

        final Canvas canvas = new Canvas(bitmap);
        if (Home.getPreferencesBooleanDefaultFalse(SHOW_STATISTICS_PRINT_COLOR)) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, width, height, paint);
        }


        view.destroyDrawingCache();
        view.layout(0, 0, width, height);
        view.draw(canvas);

        if (annotation != null) {
            final int offset = (annotation != null) ? 40 : 0;
            final Bitmap bitmapf = Bitmap.createBitmap(width,
                    height + offset, Bitmap.Config.ARGB_8888);
            final Canvas canvasf = new Canvas(bitmapf);

            Paint paint = new Paint();
            if (Home.getPreferencesBooleanDefaultFalse(SHOW_STATISTICS_PRINT_COLOR)) {
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                canvasf.drawRect(0, 0, width, offset, paint);
                paint.setColor(Color.BLACK);
            } else {
                paint.setColor(Color.GRAY);
            }
            paint.setTextSize(20);
            // paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
            canvasf.drawBitmap(bitmap, 0, offset, paint);
            canvasf.drawText(annotation, 50, (offset / 2) + 5, paint);
            bitmap.recycle();
            return bitmapf;
        }

        return bitmap;
    }

    public static Bitmap screenShot2(View view) {
        Log.d(TAG, "Screenshot2 called: " + view.getWidth() + "," + view.getHeight());
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        final Bitmap bitmap = view.getDrawingCache(true);
        return bitmap;
    }


    public static void bitmapToFile(Bitmap bitmap, String path, String fileName) {

        if (bitmap == null) return;
        Log.d(TAG, "bitmapToFile: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        File dir = new File(path);
        if (!dir.exists())
            dir.mkdirs();
        final File file = new File(path, fileName);
        try {
            FileOutputStream output = new FileOutputStream(file);
            final boolean result = bitmap.compress(Bitmap.CompressFormat.PNG, 80, output);
            output.flush();
            output.close();
            Log.d(TAG, "Bitmap compress result: " + result);
        } catch (Exception e) {
            Log.e(TAG, "Got exception writing bitmap to file: " + e);
        }
    }

    public static void shareImage(Context context, File file) {
        Uri uri = Uri.fromFile(file);
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        try {
            context.startActivity(Intent.createChooser(intent, "Share"));
        } catch (ActivityNotFoundException e) {
            static_toast_long("No suitable app to show an image!");
        }
    }

    public static void wakeUpIntent(Context context, long delayMs, PendingIntent pendingIntent) {
        final long wakeTime = JoH.tsl() + delayMs;
        Log.d(TAG, "Scheduling wakeup intent: " + dateTimeText(wakeTime));
        final AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
        } else
            alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
    }

    public static void scheduleNotification(Context context, String title, String body, int delaySeconds, int notification_id) {
        final Intent notificationIntent = new Intent(context, Home.class).putExtra(Home.SHOW_NOTIFICATION, title).putExtra("notification_body", body).putExtra("notification_id", notification_id);
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, notification_id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.d(TAG, "Scheduling notification: " + title + " / " + body);
        wakeUpIntent(context, delaySeconds * 1000, pendingIntent);
    }


    public static void showNotification(String title, String content, PendingIntent intent, int notificationId, boolean sound, boolean vibrate, boolean onetime) {
        final NotificationCompat.Builder mBuilder = notificationBuilder(title, content, intent);
        final long[] vibratePattern = {0, 1000, 300, 1000, 300, 1000};
        if (vibrate) mBuilder.setVibrate(vibratePattern);
        mBuilder.setLights(0xff00ff00, 300, 1000);
        if (sound)
        {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mBuilder.setSound(soundUri);
        }

        final NotificationManager mNotifyMgr = (NotificationManager) xdrip.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (!onetime) mNotifyMgr.cancel(notificationId);

        mNotifyMgr.notify(notificationId, mBuilder.build());
    }

    private static NotificationCompat.Builder notificationBuilder(String title, String content, PendingIntent intent) {
        return new NotificationCompat.Builder(xdrip.getAppContext())
                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(intent);
    }


    public static void releaseOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static void lockOrientation(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();
        int height;
        int width;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
            height = display.getHeight();
            width = display.getWidth();
        } else {
            Point size = new Point();
            display.getSize(size);
            height = size.y;
            width = size.x;
        }
        switch (rotation) {
            case Surface.ROTATION_90:
                if (width > height)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_180:
                if (height > width)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case Surface.ROTATION_270:
                if (width > height)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            default:
                if (height > width)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    public static boolean isAirplaneModeEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public synchronized static void setBluetoothEnabled(Context context, boolean state) {
        try {

            if (isAirplaneModeEnabled(context)) {
                UserError.Log.e(TAG, "Not setting bluetooth to state: " + state + " due to airplane mode being enabled");
                return;
            }

            if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {

                final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                if (bluetoothManager == null) {
                    UserError.Log.e(TAG, "Couldn't get bluetooth in setBluetoothEnabled");
                    return;
                }
                BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter(); // local scope only
                if (mBluetoothAdapter == null) {
                    UserError.Log.e(TAG, "Couldn't get bluetooth adapter in setBluetoothEnabled");
                    return;
                }
                try {
                    if (state) {
                        UserError.Log.i(TAG, "Setting bluetooth enabled");
                        mBluetoothAdapter.enable();
                    } else {
                        UserError.Log.i(TAG, "Setting bluetooth disabled");
                        mBluetoothAdapter.disable();

                    }
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Exception when enabling/disabling bluetooth: " + e);
                }
            } else {
                UserError.Log.e(TAG, "Bluetooth low energy not supported");
            }
        } finally {
            //
        }
    }

    public static void niceRestartBluetooth(Context context) {
        if (!isOngoingCall()) {
            if (ratelimit("joh-restart-bluetooth", 600)) {
                restartBluetooth(context);
            }
        }
    }

    public synchronized static void restartBluetooth(final Context context) {
        restartBluetooth(context, 0);
    }

    public synchronized static void restartBluetooth(final Context context, final int startInMs) {
        new Thread() {
            @Override
            public void run() {
                final PowerManager.WakeLock wl = getWakeLock("restart-bluetooth", 60000);
                Log.d(TAG, "Restarting bluetooth");
                try {
                    if (startInMs > 0) {
                        try {
                            Thread.sleep(startInMs);
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Got interrupted waiting to start resetBluetooth");
                        }
                    }
                    setBluetoothEnabled(context, false);
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Got interrupted in resetBluetooth");
                    }
                    setBluetoothEnabled(context, true);
                } finally {
                    releaseWakeLock(wl);
                }
            }
        }.start();
    }
}
