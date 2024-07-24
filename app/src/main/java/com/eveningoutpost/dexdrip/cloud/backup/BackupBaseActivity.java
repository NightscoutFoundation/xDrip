package com.eveningoutpost.dexdrip.cloud.backup;


import android.content.Intent;


import androidx.appcompat.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;

import lombok.val;

/**
 * JamOrHam
 * Base Activity for Google Drive backup
 */

public abstract class BackupBaseActivity extends AppCompatActivity {

    private static final String TAG = BackupBaseActivity.class.getSimpleName();

    protected static final int REQUEST_CODE_SIGN_IN = 6000;
    protected static final int REQUEST_CODE_PERMISSIONS = 4005;

    protected final Scope SCOPE_FILE = new Scope(Scopes.DRIVE_FILE);
    protected final Scope SCOPE_EMAIL = new Scope(Scopes.EMAIL); // have to request or account access doesn't work

    protected Runnable runAfterSignIn = null;
    protected Runnable runAfterOk = null;


    @Override
    protected synchronized void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            UserError.Log.i(TAG, "Received sign in result code: " + resultCode);
            if (resultCode == RESULT_OK) {
                UserError.Log.i(TAG, "Signed in successfully.");
                processRunnable();
            } else {
                UserError.Log.w(TAG, "Unable to sign in, result code: " + resultCode);
            }
        }
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (resultCode == RESULT_OK) {
                UserError.Log.d(TAG, "Authorization result ok");
                if (runAfterOk != null) {
                    UserError.Log.d(TAG, "Running runnable after authorization ok");
                    runAfterOk.run();
                    runAfterOk = null;
                }
            }
        }
    }

    public boolean isSignedInSignIn(final Runnable runAfterSignIn) {
        if (!isSignedIn()) {
            this.runAfterSignIn = runAfterSignIn;
            signIn(true);
            return false;
        } else {
            if (runAfterSignIn != null) {
                UserError.Log.d(TAG, "Running runnable immediately as already signed in");
                runAfterSignIn.run();
            }
            return true;
        }
    }

    public boolean isSignedIn() {
        val signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        return (signInAccount != null
                && signInAccount.getGrantedScopes().contains(SCOPE_FILE)
                && signInAccount.getGrantedScopes().contains(SCOPE_EMAIL));
    }

    private synchronized void processRunnable() {
        if (runAfterSignIn != null) {
            UserError.Log.d(TAG, "Processing runnable");
            runAfterSignIn.run();
            runAfterSignIn = null;
        }
    }

    private void signIn(boolean dialog) {
        if (isSignedIn()) {
            UserError.Log.d(TAG, "Already signed in...");
            return;
        }

        UserError.Log.i(TAG, "Start sign-in.");
        val mGoogleSignInClient = getGoogleSignInClient();
        mGoogleSignInClient.silentSignIn()
                .addOnSuccessListener(googleSignInAccount -> {
                    UserError.Log.d(TAG, "Silent sign-in worked");
                    processRunnable();
                }).addOnFailureListener(e -> {
            UserError.Log.d(TAG, "Silent sign-in failed");
            if (dialog) {
                startActivityForResult(
                        mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
            }
        });
    }

    protected GoogleSignInClient getGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(SCOPE_FILE, SCOPE_EMAIL)
                        .build();
        return GoogleSignIn.getClient(this, signInOptions);
    }

    protected void signOut(final Runnable runnable) {
        Backup.clearBackupUri();
        DriveManager.deleteInstance();
        getGoogleSignInClient().revokeAccess()
                .addOnCompleteListener(task -> getGoogleSignInClient().signOut()
                        .addOnCompleteListener(task1 -> {
                            if (runnable != null) {
                                runnable.run();
                            }
                        }));
    }

    protected boolean hasNeededPermissions() {
        return GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(getApplicationContext()),
                SCOPE_FILE, SCOPE_EMAIL);
    }

    protected void checkForGooglePermissions(final Runnable runAfterOk) {
        if (!hasNeededPermissions()) {
            this.runAfterOk = runAfterOk;
            JoH.show_ok_dialog(this, getString(R.string.please_grant_permission),
                    getString(R.string.drive_permission_blurb),
                    () -> GoogleSignIn.requestPermissions(
                            BackupBaseActivity.this,
                            REQUEST_CODE_PERMISSIONS,
                            GoogleSignIn.getLastSignedInAccount(getApplicationContext()),
                            SCOPE_FILE, SCOPE_EMAIL));
        } else {
            if (runAfterOk != null) {
                UserError.Log.d(TAG, "Running after permission check immediately");
                runAfterOk.run();
            }
        }
    }
}