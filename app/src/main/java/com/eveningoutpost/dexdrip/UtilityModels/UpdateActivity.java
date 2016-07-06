package com.eveningoutpost.dexdrip.UtilityModels;

// jamorham

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class UpdateActivity extends AppCompatActivity {

    private static final String autoUpdatePrefsName = "auto_update_download";
    private static final String TAG = "jamorham update";
    private static OkHttpClient httpClient = null;
    public static double last_check_time = 0;
    private static SharedPreferences prefs;
    private static int versionnumber = 0;
    private static int newversion = 0;

    private static String DOWNLOAD_URL = "";

    public static void checkForAnUpdate(final Context context) {
        if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(autoUpdatePrefsName, true)) return;
        if ((JoH.ts() - last_check_time) > 86400000) {
            last_check_time = JoH.ts();

            String channel = prefs.getString("update_channel", "beta");
            Log.i(TAG, "Checking for a software update, channel: " + channel);

            final String CHECK_URL = context.getString(R.string.wserviceurl) + "/update-check/" + channel;
            DOWNLOAD_URL = "";
            newversion = 0;

            new Thread(new Runnable() {
                public void run() {
                    try {

                        if (httpClient == null) {
                            httpClient = new OkHttpClient();
                            httpClient.setConnectTimeout(30, TimeUnit.SECONDS);
                            httpClient.setReadTimeout(60, TimeUnit.SECONDS);
                            httpClient.setWriteTimeout(20, TimeUnit.SECONDS);
                        }
                        getVersionInformation(context);
                        if (versionnumber == 0) return;

                        String locale = "";
                        try {
                            locale = Locale.getDefault().toString();
                            if (locale == null) locale = "";
                        } catch (Exception e) {
                            // do nothing
                        }


                        Request request = new Request.Builder()
                                // Mozilla header facilitates compression
                                .header("User-Agent", "Mozilla/5.0")
                                .header("Connection", "close")
                                .url(CHECK_URL + "?r=" + Long.toString((System.currentTimeMillis() / 100000) % 9999999) + "&ln=" + JoH.urlEncode(locale))
                                .build();

                        Response response = httpClient.newCall(request).execute();
                        if (response.isSuccessful()) {

                            String lines[] = response.body().string().split("\\r?\\n");

                            if (lines.length > 1) {
                                try {
                                    newversion = Integer.parseInt(lines[0]);
                                    if (newversion > versionnumber) {
                                        if (lines[1].startsWith("http")) {
                                            Log.i(TAG, "Notifying user of new update available our version: " + versionnumber + " new: " + newversion);
                                            DOWNLOAD_URL = lines[1];
                                            Intent intent = new Intent(context, UpdateActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            context.startActivity(intent);

                                        } else {
                                            Log.e(TAG, "Error parsing second line of update reply");
                                        }
                                    } else {
                                        Log.i(TAG, "Our current version is the most recent: " + versionnumber + " vs " + newversion);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Got exception parsing update version: " + e.toString());
                                }
                            } else {
                                Log.d(TAG, "zero lines received in reply");
                            }
                            Log.i(TAG, "Success getting latest software version");
                        } else {
                            Log.d(TAG, "Failure getting update URL data: code: " + response.code());
                        }
                    } catch (Exception e) {
                        UserError.Log.e(TAG, "Exception in reading http update version " + e.toString());
                    }
                    httpClient = null; // for GC
                }
            }).start();
        }
    }

    private static void getVersionInformation(Context context) {
        // try {
        if (versionnumber == 0) {
            //versionnumber = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA).versionCode;
            versionnumber = BuildConfig.buildVersion;
        }
        // } catch (PackageManager.NameNotFoundException e) {
        //    Log.e(TAG, "PackageManager.NameNotFoundException:" + e.getMessage());
        // }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setContentView(R.layout.activity_update);
        JoH.fixActionBar(this);

        Switch autoUpdateSwitch = (Switch) findViewById(R.id.autoupdate);
        autoUpdateSwitch.setChecked(prefs.getBoolean(autoUpdatePrefsName, true));
        autoUpdateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(autoUpdatePrefsName, isChecked).commit();
                Log.d(TAG, "Auto Updates IsChecked:" + isChecked);
            }
        });

        TextView detail = (TextView) findViewById(R.id.updatedetail);
        detail.setText(getString(R.string.new_version_date_colon) + Integer.toString(newversion) + "\n"+getString(R.string.old_version_date_colon) + Integer.toString(versionnumber));
        TextView channel = (TextView) findViewById(R.id.update_channel);
        channel.setText(getString(R.string.update_channel_colon_space) + JoH.ucFirst(prefs.getString("update_channel", "beta")));
    }

    public void closeActivity(View myview) {
        finish();
    }

    public void downloadNow(View myview) {
        if (DOWNLOAD_URL.length() > 0) {
            Intent downloadActivity = new Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_URL + "&rr=" + JoH.qs(JoH.ts())));
            startActivity(downloadActivity);
            finish();
        } else {
            Log.e(TAG, "Download button pressed but no download URL");
        }
    }
}
