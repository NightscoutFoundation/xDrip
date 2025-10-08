package com.eveningoutpost.dexdrip.cloud.backup;

import static com.eveningoutpost.dexdrip.models.JoH.showNotification;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.BACKUP_ACTIVITY_ID;
import static com.eveningoutpost.dexdrip.cloud.backup.Backup.cleanPhoneName;
import static com.eveningoutpost.dexdrip.cloud.backup.Backup.isBackupSuitableForAutomatic;
import static com.eveningoutpost.dexdrip.cloud.backup.DriveManager.BINARY_FILE_TYPE;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableField;
import android.net.Uri;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.receiver.InfoContentProvider;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PrefsViewImpl;
import com.eveningoutpost.dexdrip.databinding.ActivityBackupPickerBinding;
import com.eveningoutpost.dexdrip.ui.dialog.GenericConfirmDialog;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import java.io.IOException;

import lombok.val;

/**
 * JamOrHam
 * Storage Access Framework and xDrip Drive Manager based backup system
 */

@SuppressWarnings("ConstantConditions")
public class BackupActivity extends BackupBaseActivity implements BackupStatus {

    private static final int REQUEST_CODE_CHOOSE_FILE = 2008;
    private static final String TAG = BackupActivity.class.getSimpleName();

    private ActivityBackupPickerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBackupPickerBinding.inflate(getLayoutInflater());
        binding.setVm(new ViewModel());
        binding.setMap(binding.getVm().map);
        binding.setPrefs(new PrefsViewImpl());
        setContentView(binding.getRoot());
        JoH.fixActionBar(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.getVm().readMetaData();

    }

    void selectAutomaticFile() {
        // If we are already signed in, give user option to change account/reset permissions
        if (isSignedIn() && hasNeededPermissions()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.change_google_account)
                    .setMessage("Automatic management appears to already be active.\nDo you want to change google account used with this feature?");

            builder.setNegativeButton(R.string.keep_same_account, (dialog, which) -> selectAutomaticFileReal());
            builder.setPositiveButton(R.string.change_account, (dialog, which) -> {
                binding.getVm().clear();
                signOut(this::selectAutomaticFileReal);
            });

            final AlertDialog dialog = builder.create();
            try {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            } catch (Exception e) {
                //
            }
            dialog.show();

        } else {
            selectAutomaticFileReal();
        }
    }

    void selectAutomaticFileReal() {
        isSignedInSignIn(() -> checkForGooglePermissions(() -> Inevitable.task("selectAutomaticFile", 100, () -> {
            try {
                UserError.Log.d(TAG, "Trying to get or make file");
                val file = DriveManager.getInstance().getOrCreateFileSync(Backup.getDefaultFolderName() + "/" + Backup.getDefaultFileName());
                UserError.Log.d(TAG, "Auto file: " + file.getName() + " " + file.getId());
                Backup.setXdripManagedBackupUri(file.getId(), file.getName());
                binding.getVm().readMetaData();
            } catch (NullPointerException | IOException e) {
                UserError.Log.e(TAG, "Failed creating file: " + e);
                if (e instanceof UserRecoverableAuthIOException) {
                    if (JoH.ratelimit("automatic-file-real-user-exception", 30)) {
                        runAfterOk = this::selectAutomaticFileReal; // retry
                        startActivityForResult(((UserRecoverableAuthIOException) e).getIntent(), REQUEST_CODE_PERMISSIONS);
                    }
                } else {
                    status(e.getMessage());
                    e.printStackTrace();
                }
            }
        })));
    }

    void selectFile(final boolean create) {
        val fileIntent = new Intent(create ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT);
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        fileIntent.setType(BINARY_FILE_TYPE);
        fileIntent.putExtra(Intent.EXTRA_TITLE, Backup.getDefaultFileName());
        startActivityForResult(Intent.createChooser(fileIntent, getString(R.string.select_alternate_file)), REQUEST_CODE_CHOOSE_FILE);
    }


    public class ViewModel {

        public final ObservableField<String> status = new ObservableField<>("Ready");
        public final ObservableField<Boolean> idle = new ObservableField<>(true);
        public final ObservableField<Boolean> showAuto = new ObservableField<>(false);
        public final ObservableArrayMap<String, String> map = new ObservableArrayMap<>();

        private BackupMetaData metaData = null;

        {
            clear();
        }

        public void clear() {
            final String[] texts = {"lastBackupTime", "lastAgoTime", "lastDevice", "selectedLocation", "stext"};
            for (val text : texts) {
                map.put(text, "");
            }
        }

        private void map(final String varname, final String name, final String value) {
            map.put(varname, value);
            map.put(varname + "String", name);
        }

        public void selectFile() {
            val t = BackupActivity.this;
            final AlertDialog.Builder builder = new AlertDialog.Builder(t)
                    .setTitle(R.string.choose_backup_file)
                    .setMessage(R.string.select_automatic_or_select_alternate);

            builder.setNegativeButton(R.string.use_alternate_file, (dialog, which) -> t.selectFile(false));
            builder.setPositiveButton(R.string.automatically_manage_backup, (dialog, which) -> selectAutomaticFile());

            final AlertDialog dialog = builder.create();
            try {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            } catch (Exception e) {
                //
            }
            dialog.show();
        }

        public void backupNow() {
            if (idle.get()) {
                if (Backup.isBackupUriSet()) {
                    idle.set(false);
                    Inevitable.task("do backup", 200, () -> {
                        try {
                            status(getString(R.string.backing_up));
                            val success = Backup.doCompleteBackup(BackupActivity.this);
                            if (success) {
                                status(getString(R.string.did_backup_ok));
                            } else {
                                status(getString(R.string.backup_failed));
                            }
                            Inevitable.task("reload meta data", 2000, () -> binding.getVm().readMetaData());
                        } finally {
                            idle.set(true);
                        }
                    });
                } else {
                    status(getString(R.string.no_backup_location_set));
                }
            }
        }

        public synchronized void restoreNow() {
            if (!Backup.isBackupUriSet() || metaData == null) {
                status(getString(R.string.nothing_to_restore_please_select));
            } else {
                Runnable restoreRunnable = () -> GenericConfirmDialog.show(BackupActivity.this, getString(R.string.are_you_really_sure_question), getString(R.string.restoring_backup_will_erase_warning), () -> restoreNowReal());
                if (metaData.sourceDevice != null && metaData.sourceDevice.equals(cleanPhoneName())) {
                    restoreRunnable.run();
                } else {
                    GenericConfirmDialog.show(BackupActivity.this, getString(R.string.backup_source_confirm), getString(R.string.this_backup_looks_like_came_from_different_format_string, metaData.sourceDevice), restoreRunnable);
                }
            }
        }

        private synchronized void restoreNowReal() {
            if (idle.get()) {
                idle.set(false);
                status(getString(R.string.attempting_restore));
                Inevitable.task("do backup", 200, () -> {
                    try {
                        val metaData = Backup.restoreBackFromDefaultUri();
                        status(metaData.successResult ? getString(R.string.restore_succeeded_restarting) : getString(R.string.restore_failed));
                        if (metaData.exception != null) {
                            status(getString(R.string.error_exclamation) + " " + metaData.exception);
                        }
                        InfoContentProvider.ping("pref");
                    } finally {
                        idle.set(true);
                    }
                });
            }
        }

        public void readMetaData() {
            showAuto.set(isBackupSuitableForAutomatic());
            if (Backup.isBackupUriSet()) {
                if (idle.get()) {
                    idle.set(false);
                    Inevitable.task("read backup meta data", 100, () -> {
                        try {
                            status(getString(R.string.checking_file));
                            metaData = Backup.getBackupMetaDataDefaultUri();
                            if (metaData.successResult) {
                                map("lastBackupTime", "Last backup time", metaData.getTimeStampString());
                                map("lastAgoTime", "Last backup was", metaData.getTimeSinceString());
                                map("lastDevice", "Backup made by", metaData.sourceDevice);
                                map("selectedLocation", "Selected location", metaData.displayName);
                                map("stext", "Backup size", getString(R.string.megabyte_format_string, metaData.getSizeInMb()));
                                status(getString(R.string.ready));
                            } else {
                                status(getString(R.string.failed_to_read_file) + ((metaData.exception != null && metaData.exception.getMessage() != null) ? (": " + metaData.exception.getMessage()) : ""));
                                if (metaData.exception != null && metaData.exception instanceof UserRecoverableAuthIOException) {
                                    // if permissions lost ask again
                                    if (JoH.ratelimit("reask-backup-perms", 20)) {
                                        status(getString(R.string.permission_problem_retry));
                                        startActivityForResult(((UserRecoverableAuthIOException) metaData.exception).getIntent(), REQUEST_CODE_PERMISSIONS);
                                    } else {
                                        if (JoH.ratelimit("reask-try-signout", 30)) {
                                            clear();
                                            signOut(() -> checkForGooglePermissions(() -> selectAutomaticFileReal()));
                                        }
                                    }
                                }
                            }

                        } finally {
                            idle.set(true);
                        }
                    });
                }
            } else {
                UserError.Log.d(TAG, "No URI set to read metadata from");
            }
        }
    }

    @Override
    public void status(final String msg) {
        binding.getVm().status.set(msg);
        UserError.Log.d(TAG, "Status: " + msg);
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE_FILE) {
            if (data != null) {
                final Uri selectedFileUri = data.getData();
                try {
                    UserError.Log.d(TAG, "URL: " + selectedFileUri);
                    if (isOnGDrive(selectedFileUri)) {
                        GenericConfirmDialog.show(this, getString(R.string.temporary_google_drive_file), getString(R.string.manual_selection_google_drive_file_warning), () -> useNewUri(selectedFileUri));
                    } else {
                        GenericConfirmDialog.show(this, getString(R.string.alternate_file_selected), getString(R.string.alternate_file_selected_warning), () -> useNewUri(selectedFileUri));
                    }

                } catch (Exception e) {
                    JoH.static_toast_long(getString(R.string.problem_with_file_selection) + " " + e.getMessage());
                }
            } else {
                status(getString(R.string.no_file_was_chosen));
            }
        }
        if (requestCode == REQUEST_CODE_SIGN_IN && resultCode != RESULT_OK) {
            status(getString(R.string.could_not_sign_in));
        }
    }

    private boolean isOnGDrive(final Uri selectedFileUri) {
        switch (selectedFileUri.getAuthority()) {
            case "com.google.android.apps.docs.storage":
            case "com.google.android.apps.docs.storage.legacy":
                return true;
            default:
                return false;
        }
    }

    private void useNewUri(final Uri selectedFileUri) {
        getContentResolver().takePersistableUriPermission(selectedFileUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        val fileUri = selectedFileUri.toString();
        Backup.setBackupUri(fileUri);
        status(getString(R.string.selected_file_location));
        binding.getVm().readMetaData();
    }

    static PendingIntent getStartIntent() {
        return PendingIntent.getActivity(xdrip.getAppContext(), 0, new Intent(xdrip.getAppContext(), BackupActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void notifySecurityError() {
        if (JoH.pratelimit("backup-security-notification-n", 60 * 60 * 12)) {
            showNotification(xdrip.gs(R.string.please_reselect_backup_file), xdrip.gs(R.string.backup_file_security_error_advice), getStartIntent(), BACKUP_ACTIVITY_ID, true, true, true);
            UserError.Log.uel(TAG, "Backup file is reporting security error. Re-select it to continue to allow xDrip access.");
        }
    }
}