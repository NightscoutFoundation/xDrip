package com.eveningoutpost.dexdrip.utilitymodels;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.eveningoutpost.dexdrip.utils.FileUtils.makeSureDirectoryExists;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Saves xDrip logs to storage.
// SendFeedBack sends logs to the lead developer.
// This does the same thing for saving logs to storage.
// Navid200
// July 2024

public class SaveLogs extends BaseAppCompatActivity {

    private static final String TAG = "save logs";
    private String LOG_FILE_PATH = "/Download/xDrip-export"; // Path to where we save the log file
    private String LOG_FILE_NAME = "xDrip-log.txt"; // Log file name
    private final static int MY_PERMISSIONS_REQUEST_STORAGE = 104;
    private String log_data = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_logs);

        Intent intent = getIntent();
        if (intent != null) {
            final Bundle bundle = intent.getExtras();
            if (bundle != null) {
                final String str2 = bundle.getString("generic_text");
                if (str2 != null) {
                    log_data = str2;
                    ((TextView) findViewById(R.id.yourSaveText)).setText(log_data.length() > 300 ? "\n\nAttached " + log_data.length() + " characters of log data. (hidden)\n\n" : log_data);
                }
            }
        }
    }

    public void closeActivity(View myview) {
        finish();
    }

    public void saveLogs(View myview) {
        if (saveLogsToStorage(log_data)) {
            UserError.Log.e(TAG, "Saved log file to /Download/xDrip-export/xDrip-log.txt");
        } else {
            UserError.Log.e(TAG, "Could not write log file");
        }
        log_data = "";
        closeActivity(null); // Let's close the menu
    }

    public boolean saveLogsToStorage(String contents) {
        if (isStorageWritable(this, MY_PERMISSIONS_REQUEST_STORAGE)) {
            try {
                final StringBuilder sb = new StringBuilder();
                sb.append(Environment.getExternalStorageDirectory().getAbsolutePath());
                sb.append(LOG_FILE_PATH);
                final String dir = sb.toString();
                makeSureDirectoryExists(dir);
                final String pathPlusFileName = dir + "/" + LOG_FILE_NAME;
                final File myExternalFile = new File(pathPlusFileName);
                FileOutputStream fos = new FileOutputStream(myExternalFile);
                fos.write(contents.getBytes());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else {
            JoH.static_toast_long("getString(R.string.sdcard_not_writable_cannot_save)");
            return false;
        }
    }

    public static boolean isStorageWritable(Activity context, int request_code) { // Get write permission if not & return false.  Return true if yes and not tied up.
        if (ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    request_code);
            UserError.Log.e(TAG, "Did not have write permission, but should have it now");
            return false;
        }
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

}

