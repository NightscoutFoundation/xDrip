
package com.eveningoutpost.dexdrip.utilitymodels.pebble.watchface;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.util.Log;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static com.eveningoutpost.dexdrip.xdrip.gs;

/**
 * Created by jamorham on 22/04/2016.
 * modified by Andy, to be able to add additional PebbleWtatchFace, which can just extend this
 */
public class InstallPebbleWatchFace extends BaseAppCompatActivity {

    private final static int MY_PERMISSIONS_REQUEST_STORAGE = 99;
    private static String TAG = "InstallPebbleWatchFace";

    protected String getTag() {
        return TAG;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppThemeToolBarLite); // for toolbar mode
        super.onCreate(savedInstanceState);
        if (installFile()) {
            finish();
        }
    }

    protected boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (!checkPermissions()) return false;
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                final Activity activity = this;
                JoH.show_ok_dialog(activity, gs(R.string.please_allow_permission), "Need storage permission to install watchface", new Runnable() {
                    @Override
                    public void run() {
                        ActivityCompat.requestPermissions(activity,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE);
                    }
                });
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                installFile();
                finish();
            } else {
                toast("Need permission to write watchface");
                finish();
            }
        }
    }


    protected InputStream openRawResource() {
        return getResources().openRawResource(R.raw.xdrip_pebble);
    }

    protected String getOutputFilename() {
        return "xDrip-plus-pebble-auto-install.pbw";
    }


    protected void toast(String msg) {
        try {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            Log.d(getTag(), "Toast msg: " + msg);
        } catch (Exception e) {
            Log.e(getTag(), "Couldn't display toast: " + msg);
        }
    }

    public boolean installFile() {
        if (!isExternalStorageWritable()) {
            toast("External storage is not writable!");
            return false;
        }
        // create the file where pebble helper can process it
        try {
            // Confirmed as freely redistributable by jstevensog - source https://github.com/jstevensog/xDrip-pebble
            InputStream in = openRawResource();

            String dest_filename = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + getOutputFilename();
            OutputStream out = new FileOutputStream(dest_filename);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            // open the file - trigger pebble helper app
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = FileProvider.getUriForFile(this.getApplicationContext(), this.getPackageName() + ".provider", new File(dest_filename));
            intent.setDataAndType(uri, "application/octet-stream");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        } catch (Exception e) {
            UserError.Log.e(getTag(), " Got exception: " + e.toString());
            toast("Error: "+e.getLocalizedMessage());
        }
        return true;
    }

    public void askToInstall() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Pebble Install");
        builder.setMessage("Install Pebble Watchface?");

        builder.setPositiveButton(gs(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                installFile();
                finish();
            }
        });

        builder.setNegativeButton(gs(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
}
