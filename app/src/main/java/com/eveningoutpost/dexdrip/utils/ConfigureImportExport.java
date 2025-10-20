package com.eveningoutpost.dexdrip.utils;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.base.Strings;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by gruoner on 26/06/2022.
 */

public class ConfigureImportExport extends BaseAppCompatActivity {

    private static final String TAG = "ConfigureImportExport";
    private static final int BUFFER_SIZE = 4096;

    public enum ImportSelection { old, localStorage, WebDAVStorage }
    private Button cancelBtn;
    private Button saveBtn;
    private Button undoBtn;
    private Switch localStorageSwitch;
    private Switch webdavStorageSwitch;
    private Switch webdavStorageSSLSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configureimportexportsettings_editor);

        undoBtn = (Button) findViewById(R.id.importexportUndoBtn);
        saveBtn = (Button) findViewById(R.id.importexportSaveBtn);
        cancelBtn = (Button) findViewById(R.id.importexportCancelbtn);
        localStorageSwitch = (Switch) findViewById(R.id.localstorageswitch);
        localStorageSwitch.setOnCheckedChangeListener( (v, p) -> {
            if (localStorageSwitch.isChecked())
                ((LinearLayout)findViewById(R.id.localstoragelayout)).setVisibility(View.VISIBLE);
            else ((LinearLayout)findViewById(R.id.localstoragelayout)).setVisibility(View.INVISIBLE);
        });
        webdavStorageSwitch = (Switch) findViewById(R.id.webdavstorageswitch);
        webdavStorageSwitch.setOnCheckedChangeListener( (v, p) -> {
            if (webdavStorageSwitch.isChecked())
                ((LinearLayout)findViewById(R.id.webdavstoragelayout)).setVisibility(View.VISIBLE);
            else ((LinearLayout)findViewById(R.id.webdavstoragelayout)).setVisibility(View.INVISIBLE);
        });
        webdavStorageSSLSwitch = (Switch) findViewById(R.id.webdavstoragesslswitch);

        loadFromPrefs();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void CancelButton(View myview) {
        finish();
    }

    public void SaveButton(View myview) {
        Pref.setString("importexport2localstorage_path", getStringFromText(R.id.localstoragepath));
        Pref.setBoolean("importexport2localstorage_enabled", localStorageSwitch.isChecked());
        Pref.setBoolean("importexport2webdavstorage_ssl", webdavStorageSSLSwitch.isChecked());
        Pref.setString("importexport2webdavstorage_server", getStringFromText(R.id.webdavstorageserver));
        Pref.setString("importexport2webdavstorage_path", getStringFromText(R.id.webdavstoragepath));
        Pref.setString("importexport2webdavstorage_user", getStringFromText(R.id.webdavstorageuser));
        Pref.setString("importexport2webdavstorage_password", getStringFromText(R.id.webdavstoragepassword));
        Pref.setBoolean("importexport2webdavstorage_enabled", webdavStorageSwitch.isChecked());
        finish();
    }

    public void UndoButton(View myview) {
        Pref.removeItem("importexport2localstorage_enabled");
        Pref.removeItem("importexport2localstorage_path");
        Pref.removeItem("importexport2webdavstorage_enabled");
        Pref.removeItem("importexport2webdavstorage_server");
        Pref.removeItem("importexport2webdavstorage_ssl");
        Pref.removeItem("importexport2webdavstorage_path");
        Pref.removeItem("importexport2webdavstorage_user");
        Pref.removeItem("importexport2webdavstorage_password");
        finish();
    }

    private String getStringFromText(final int name)  {
        TextInputEditText v = (TextInputEditText) findViewById(name);
        return v.getText().toString();
    }
    private void setStringToText(final int name, String w)  {
        TextInputEditText v = (TextInputEditText) findViewById(name);
        v.setText(w);
    }

    private void loadFromPrefs()
    {
        String defaultServerName = "nightscout";
        String url = "";
        if (Pref.isPreferenceSet("nsfollow_url"))
            url = Pref.getString("nsfollow_url", "");
        else if (Pref.isPreferenceSet("cloud_storage_api_base"))
            url = Pref.getString("cloud_storage_api_base", "");
        if (!Strings.isNullOrEmpty(url)) {
            String[] s = url.split("/");
            if (s[2].contains("@")) {
                String[] h = s[2].split("@");
                defaultServerName = h[1];
            } else defaultServerName = s[2];
        }

        localStorageSwitch.setChecked(Pref.getBooleanDefaultFalse("importexport2localstorage_enabled"));
        setStringToText(R.id.localstoragepath, Pref.getString("importexport2localstorage_path",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/xdrip/backup"));
        webdavStorageSwitch.setChecked(Pref.getBooleanDefaultFalse("importexport2webdavstorage_enabled"));
        webdavStorageSSLSwitch.setChecked(Pref.getBooleanDefaultFalse("importexport2webdavstorage_ssl"));
        setStringToText(R.id.webdavstorageserver, Pref.getString("importexport2webdavstorage_server", defaultServerName));
        setStringToText(R.id.webdavstoragepath, Pref.getString("importexport2webdavstorage_path", "/webdav/xdrip/backup"));
        setStringToText(R.id.webdavstorageuser, Pref.getString("importexport2webdavstorage_user", "xdrip"));
        setStringToText(R.id.webdavstoragepassword, Pref.getStringDefaultBlank("importexport2webdavstorage_password"));
    }

    public static boolean isLocalStorageEnabled() {
        return Pref.getBooleanDefaultFalse("importexport2localstorage_enabled");
    }
    public static boolean isWebDAVStorageEnabled() {
        return Pref.getBooleanDefaultFalse("importexport2webdavstorage_enabled");
    }

    /*
    ==============================================
    Functions for exporting to external storage
    ==============================================
     */
    public static void dispatchAdditionalExports(String filename, Boolean attachPrefs, Boolean need2extendTimestamp) {
        if ((!Pref.getBooleanDefaultFalse("importexport2localstorage_enabled")) &&
                (!Pref.getBooleanDefaultFalse("importexport2webdavstorage_enabled")))
            return;
        String newFileName = new File(filename).getName();
        if (need2extendTimestamp) {
            final StringBuilder sb = new StringBuilder();
            sb.append(newFileName.substring(0, newFileName.lastIndexOf(".")));
            sb.append("-");
            sb.append(DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()));
            sb.append(".");
            sb.append(newFileName.substring(newFileName.lastIndexOf(".") + 1));
            newFileName = sb.toString();
        }

        File toDelete = null;
        if (attachPrefs) {
            File tempZip = new File(xdrip.getAppContext().getFilesDir() + "/" + new File(filename).getName());
            toDelete = tempZip;
            File prefsFile = new File(xdrip.getAppContext().getFilesDir().getParent() + "/" + SdcardImportExport.PREFERENCES_FILE);
            File dbFile = new File(filename);
            InputStream fiStream = null;
            FileOutputStream foStream = null;
            try {
                fiStream = new FileInputStream(dbFile);
                foStream = new FileOutputStream(tempZip, true);
                ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(foStream));
                zipOutputStream.putNextEntry(new ZipEntry(dbFile.getName()));
                byte buffer[] = new byte[BUFFER_SIZE];
                int count;
                while ((count = fiStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
                    zipOutputStream.write(buffer, 0, count);
                }
                fiStream.close();
                fiStream = new FileInputStream(prefsFile);
                zipOutputStream.putNextEntry(new ZipEntry(prefsFile.getName()));
                while ((count = fiStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
                    zipOutputStream.write(buffer, 0, count);
                }
                fiStream.close();
                zipOutputStream.close();
            } catch (FileNotFoundException e) {
                return;
            } catch (IOException e) {
                return;
            }
            filename = tempZip.toString();
        }

        if (Pref.getBooleanDefaultFalse("importexport2localstorage_enabled"))
            export2localStorage(filename, newFileName);
        if (Pref.getBooleanDefaultFalse("importexport2webdavstorage_enabled"))
            export2WebDAV(filename, newFileName);

        if (toDelete != null)
            toDelete.delete();          // delete the temporary ZIP file
        new File(filename).delete();    // as we are in a local oder webdav export, delete the exported database file
    }

    public static void export2localStorage(String filename, String nameOfFile) {
        String localExportPath = Pref.getString("importexport2localstorage_path", "");
        if (Strings.isNullOrEmpty(localExportPath)) return; // does not have a localpath specified
        if (filename.contains(localExportPath)) return;     // localpath is part of the currently exported file - no need to export twice
        File dir = new File (localExportPath);
        if (!dir.exists())
            dir.mkdirs();
        SdcardImportExport.directCopyFile(new File(filename), new File(localExportPath + "/" + nameOfFile));
    }

    public static void export2WebDAV(String filename, String nameOfFile) {
        String server = Pref.getStringDefaultBlank("importexport2webdavstorage_server");
        String path = Pref.getStringDefaultBlank("importexport2webdavstorage_path");
        String user = Pref.getStringDefaultBlank("importexport2webdavstorage_user");
        String password = Pref.getStringDefaultBlank("importexport2webdavstorage_password");
        String url = "http";
        if (Pref.getBooleanDefaultFalse("importexport2webdavstorage_ssl"))
            url = "https";
        try {
            Sardine sardine = new OkHttpSardine();
            sardine.setCredentials(user, password);
            sardine.put(url + "://" + server + path + "/" + nameOfFile, new File(filename), "application/octet-stream");
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
            JoH.static_toast_long(e.getLocalizedMessage());
        }
    }

    /*
    ==============================================
    Functions for listing Storage content and finding backups
    ==============================================
     */
    public static ArrayList<File> findAllBackups(ImportSelection s) {
        switch (s) {
            default:
                return new ArrayList<>();
            case localStorage:
                return findAllLocalBackups();
            case WebDAVStorage:
                final ArrayList<File>[] ret = new ArrayList[]{new ArrayList<>()};
                AsyncTask t = new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        ret[0] = findAllWebDAVBackups();
                        return "success";
                    }
                }.execute();
                try {
                    t.get();
                } catch (ExecutionException e) {
                } catch (InterruptedException e) {
                }
                return ret[0];
        }
    }
    public static ArrayList<File> findAllLocalBackups() {
        ArrayList<File> databases = new ArrayList<>();
        String localExportPath = Pref.getString("importexport2localstorage_path", "");
        if (Strings.isNullOrEmpty(localExportPath)) return databases; // does not have a localpath specified

        File dir = new File (localExportPath);
        if (!FileUtils.makeSureDirectoryExists(dir.getAbsolutePath())) {
            return databases;
        }

        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getPath().endsWith(".zip");
            }
        });
        if ((databases != null) && (files != null)) {
            Collections.addAll(databases, files);
        }
        return databases;
    }

    public static ArrayList<File> findAllWebDAVBackups() {
        String server = Pref.getStringDefaultBlank("importexport2webdavstorage_server");
        String path = Pref.getStringDefaultBlank("importexport2webdavstorage_path");
        String user = Pref.getStringDefaultBlank("importexport2webdavstorage_user");
        String password = Pref.getStringDefaultBlank("importexport2webdavstorage_password");
        String url = "http";
        if (Pref.getBooleanDefaultFalse("importexport2webdavstorage_ssl"))
            url = "https";
        ArrayList<File> ret = new ArrayList<>();
        try {
            Sardine sardine = new OkHttpSardine();
            sardine.setCredentials(user, password);
            List<DavResource> d = sardine.list(url + "://" + server + path + "/", 1, false);
            for (DavResource r: d)
                if (r.getContentType().toLowerCase().equals("application/octet-stream"))
                    ret.add(new File(r.toString()));
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
            JoH.static_toast_long(e.getLocalizedMessage());
        }
        return ret;
    }

    /*
    ==============================================
    Functions for fetching backup from external storage content and extracting content
    ==============================================
    */
    public static File fetchBackup(ImportSelection s, File f) {
        switch (s) {
            default:
                return null;
            case localStorage:
                return fetchBackupFromLocal(f);
            case WebDAVStorage:
                final File[] ret = new File[]{f};
                AsyncTask t = new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        ret[0] = fetchBackupFromWebDAV(f);
                        return "success";
                    }
                }.execute();
                try {
                    t.get();
                } catch (ExecutionException e) {
                } catch (InterruptedException e) {
                }
                return ret[0];
        }
    }
    public static File fetchBackupFromLocal(File f) {   // because the file is still on local storage we can extract the backup directly; no need for copying
        return f;
    }
    public static File fetchBackupFromWebDAV(File f) {
        String server = Pref.getStringDefaultBlank("importexport2webdavstorage_server");
        String path = Pref.getStringDefaultBlank("importexport2webdavstorage_path");
        String user = Pref.getStringDefaultBlank("importexport2webdavstorage_user");
        String password = Pref.getStringDefaultBlank("importexport2webdavstorage_password");
        String url = "http";
        if (Pref.getBooleanDefaultFalse("importexport2webdavstorage_ssl"))
            url = "https";
        File ret = new File(xdrip.getAppContext().getFilesDir() + "/tmp_" + f.getName());
        try {
            FileOutputStream foStream = new FileOutputStream(ret, true);
            Sardine sardine = new OkHttpSardine();
            sardine.setCredentials(user, password);
            InputStream fiStream = sardine.get(url + "://" + server + path + "/" + f.getName());
            byte buffer[] = new byte[BUFFER_SIZE];
            int count;
            while ((count = fiStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
                foStream.write(buffer, 0, count);
            }
            fiStream.close();
            foStream.close();
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
            JoH.static_toast_long(e.getLocalizedMessage());
        }
        return ret;
    }

    private static File extractFromLocalZip(File f, String pattern) {
        File r = null;
        if (f.getAbsolutePath().endsWith(".zip")) {
            try {
                final FileInputStream fileInputStream = new FileInputStream(f.getAbsolutePath());
                final ZipInputStream zip_stream = new ZipInputStream(new BufferedInputStream(fileInputStream));
                ZipEntry zipEntry = null;
                while ((zipEntry = zip_stream.getNextEntry()) != null) {
                    String filename = zipEntry.getName();
                    if (filename.endsWith(pattern)) {
                        String output_filename = xdrip.getAppContext().getFilesDir() + "/" + filename;
                        FileOutputStream fout = new FileOutputStream(output_filename);
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int count = 0;
                        while ((count = zip_stream.read(buffer)) != -1) {
                            fout.write(buffer, 0, count);
                        }
                        fout.close();
                        r = new File(output_filename);
                    }
                    zip_stream.closeEntry();
                }
                zip_stream.close();
                fileInputStream.close();
            } catch (IOException e) {
                String msg = "Could not open file";
                JoH.static_toast_long(msg);
                return null;
            }
        }
        return r;
    }

    public static File extractDBBackup(ImportSelection s, File f) {
        switch (s) {
            default:
                return null;
            case localStorage:
                return extractDBBackupFromLocal(f);
            case WebDAVStorage:
                return extractDBBackupFromWebDAV(f);
        }
    }
    public static File extractDBBackupFromLocal(File f) {
        return extractFromLocalZip(f, ".zip");
    }
    public static File extractDBBackupFromWebDAV(File f) {
        return extractDBBackupFromLocal(f);
    }

    public static File extractPrefsFile(ImportSelection s, File f) {
        switch (s) {
            default:
                return null;
            case localStorage:
                return extractPrefsFileFromLocal(f);
            case WebDAVStorage:
                return extractPrefsFileFromWebDAV(f);
        }
    }
    public static File extractPrefsFileFromLocal(File f) {
        return extractFromLocalZip(f, "_preferences.xml");
    }
    public static File extractPrefsFileFromWebDAV(File f) {
        return extractPrefsFileFromLocal(f);
    }

    public static void cleanup(ImportSelection s, File f) {
        switch (s) {
            default:
                return;
            case localStorage:
                cleanupFromLocal(f);
                return;
            case WebDAVStorage:
                cleanupFromWebDAV(f);
                return;
        }
    }
    public static void cleanupFromLocal(File f) {   // no need for cleaning something up as there is no local copy
    }
    public static void cleanupFromWebDAV(File f) {
        f.delete();
    }
}


