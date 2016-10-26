package com.eveningoutpost.dexdrip;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utils.DatabaseUtil;
import com.eveningoutpost.dexdrip.utils.FileUtils;
import com.eveningoutpost.dexdrip.utils.ListActivityWithMenu;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ImportDatabaseActivity extends ListActivityWithMenu {
    private final static String TAG = ImportDatabaseActivity.class.getSimpleName();
    public static String menu_name = "Import Database";
    AlertDialog progressDialog;
    private Handler mHandler;
    private ArrayList<String> databaseNames;
    private ArrayList<File> databases;
    private final static int MY_PERMISSIONS_REQUEST_STORAGE = 132;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.OldAppTheme); // or null actionbar
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mHandler = new Handler();
        setContentView(R.layout.activity_import_db);

        showWarningAndInstructions();
    }

    private void generateDBGui() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED && findAllDatabases()) {
            sortDatabasesAlphabetically();
            showDatabasesInList();
        } else if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            JoH.static_toast_long("Need permission for saved files");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_STORAGE);
        } else {
            postImportDB("\'xdrip\' is not a directory... aborting.");
        }
    }

    private void showWarningAndInstructions() {
        LayoutInflater inflater= LayoutInflater.from(this);
        View view=inflater.inflate(R.layout.import_db_warning, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Import Instructions");
        alertDialog.setView(view);
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                generateDBGui();
            }
        });
        AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void sortDatabasesAlphabetically() {
        Collections.sort(databases, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                //descending sort
                return rhs.getName().compareTo(lhs.getName());
            }
        });
    }

    private boolean findAllDatabases() {
        databases = new ArrayList<>();

        File file = new File(FileUtils.getExternalDir());
        if (!FileUtils.makeSureDirectoryExists(file.getAbsolutePath())) {
            return false;
        }

        // add from "root"
        addAllDatabases(file, databases);

        // add from level below (Andriod usually unzips to a subdirectory)
        File[] subdirectories = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File path) {
                return path.isDirectory();
            }
        });
        for (File subdirectory : subdirectories) {
            addAllDatabases(subdirectory, databases);
        }
        return true;
    }

    private void showDatabasesInList() {
        databaseNames = new ArrayList<>();

        //show found databases in List
        for (File db : databases) {
            databaseNames.add(db.getName());
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, databaseNames);
        setListAdapter(adapter);

        if (databaseNames.size() == 0) {
            postImportDB("No databases found.");
        }
    }

    private void addAllDatabases(File file, ArrayList<File> databases) {
        File[] files = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getPath().endsWith(".sqlite");
            }
        });
        if ((databases != null) && (files != null)) {
            Collections.addAll(databases, files);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, final int position, long id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                importDB(position);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //do nothing
            }
        });
        builder.setTitle("Confirm Import");
        builder.setMessage("Do you really want to import '" + databases.get(position).getName() + "'?\n This may negatively affect the data integrity of your system!");
        AlertDialog dialog = builder.create();
        dialog.show();


    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public String getMenuName() {
        return menu_name;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateDBGui();
            } else {
                finish();
            }
        }
    }

    public int getDBVersion() {

        int version = -1;
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            version = bundle.getInt("AA_DB_VERSION");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }


    public void importDB(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Importing, please wait");
        builder.setMessage("Importing, please wait");
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setMessage("Step 1: checking prerequisites");
        dialog.setCancelable(false);
        LoadTask lt = new LoadTask(dialog, databases.get(position));
        lt.execute();
    }

    protected void postImportDB(String result) {

        if (mPrefs.getBoolean("wear_sync", false)) {//KS
            Log.d(TAG, "start WatchUpdaterService with ACTION_SYNC_DB");
            startService(new Intent(this, WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_SYNC_DB));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                returnToHome();
            }
        });
        builder.setTitle("Import Result");
        builder.setMessage(result);
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public void returnToHome() {
        Intent intent = new Intent(this, Home.class);
        CollectionServiceStarter.restartCollectionService(getApplicationContext());
        startActivity(intent);
        finish();
    }

    private class LoadTask extends AsyncTask<Void, Void, String> {

        private final AlertDialog statusDialog;
        private final File dbFile;

        LoadTask(AlertDialog statusDialog, File dbFile) {
            super();
            this.statusDialog = statusDialog;
            this.dbFile = dbFile;
        }


        protected String doInBackground(Void... args) {
            //Check if db has the correct version:
            try {
                SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                int version = db.getVersion();
                db.close();
                if (getDBVersion() != version) {
                    statusDialog.dismiss();
                    return "Wrong Database version.\n(" + version + " instead of " + getDBVersion() + ")";
                }
            } catch (SQLiteException e){
                statusDialog.dismiss();
                return "Database cannot be opened... aborting.";
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    statusDialog.setMessage("Step 2: exporting current DB");
                }
            });

            String export = DatabaseUtil.saveSql(getBaseContext());


            if (export == null) {
                statusDialog.dismiss();
                return "Exporting database not successfull... aborting.";
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    statusDialog.setMessage("Step 3: importing DB");
                }
            });

            String result = DatabaseUtil.loadSql(getBaseContext(), dbFile.getAbsolutePath());

            statusDialog.dismiss();
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            postImportDB(result);

        }
    }
}
