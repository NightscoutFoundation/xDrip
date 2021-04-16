package com.eveningoutpost.dexdrip.deposit;

import android.app.Activity;
import android.databinding.Observable;
import android.databinding.ObservableField;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.PrefsViewImpl;
import com.eveningoutpost.dexdrip.databinding.ActivityDepositActivityBinding;
import com.eveningoutpost.dexdrip.ui.dialog.GenericConfirmDialog;

import android.support.annotation.RequiresApi;
import lombok.RequiredArgsConstructor;

import static com.eveningoutpost.dexdrip.deposit.WebDeposit.getSerialInfo;

// jamorham

// UI for Web Deposit

public class DepositActivity extends AppCompatActivity {

    private ActivityDepositActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding = ActivityDepositActivityBinding.inflate(getLayoutInflater());
            binding.setVm(new ViewModel(this));
            binding.setPrefs(new PrefsViewImpl());
            setContentView(binding.getRoot());
            JoH.fixActionBar(this);
        } else {
            JoH.static_toast_long("Needs Android 7+");
            finish();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @RequiresApi(api = Build.VERSION_CODES.N)
    @RequiredArgsConstructor
    public static class ViewModel {
        Activity activity;

        public final ObservableField<String> serialInfo = new ObservableField<>(getSerialInfo());
        public final ObservableField<String> statusString = new ObservableField<>("Ready");
        public final ObservableField<Long> startTime = new ObservableField<>(0L);
        public final ObservableField<Long> endTime = new ObservableField<>(0L);
        public final ObservableField<String> startTimeString = new ObservableField<>();
        public final ObservableField<String> endTimeString = new ObservableField<>();
        public final ObservableField<Boolean> showButton = new ObservableField<>(true);


        ViewModel(Activity activity) {
            this.activity = activity;
        }

        {
            // follow start and end timestamps and convert to date strings
            startTime.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    startTimeString.set(JoH.dateTimeText(startTime.get()));
                }
            });
            endTime.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    endTimeString.set(JoH.dateTimeText(endTime.get()));
                }
            });
            getNextTimeBlock();
        }


        F success =
                s -> {
                    UserError.Log.d("Deposit Success", s);
                    showButton.set(true);
                    if (s.equals("OK")) {
                        UserError.Log.d("Deposit Success", "Deposit worked");
                        statusString.set("Succeeded Ok!");
                        WebDeposit.setNextTime(endTime.get() + (long) (Constants.HOUR_IN_MS * 3.9));
                        getNextTimeBlock();
                    } else {
                        statusString.set("Failed with message: " + s);
                    }
                };

        F failure =
                s -> {
                    UserError.Log.d("Deposit Failure", s);
                    statusString.set("Failure: " + s);
                    showButton.set(true);
                };
        F status =
                s -> {
                    statusString.set(s);
                    UserError.Log.d("Deposit Status", s);
                };


        private void getNextTimeBlock() {
            startTime.set(WebDeposit.getNextTime());
            endTime.set(Math.min(startTime.get() + Constants.MONTH_IN_MS, JoH.tsl() - Constants.HOUR_IN_MS * 8));
        }

        public synchronized void resetButton() {

            GenericConfirmDialog.show(this.activity, "Confirm Reset",
                    "Resetting could cause data overlap or other problems, you must be absolutely sure before using it",
                    () -> {
                        // call reset
                        WebDeposit.setNextTime(0L);
                        getNextTimeBlock();
                        statusString.set("Reset data sequence!");
                    });
        }

        public synchronized void depositButton() {
            showButton.set(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                new Thread(() -> WebDeposit.doUpload(startTime.get(), endTime.get(), success, failure, status)).start();
            } else {
                JoH.static_toast_long("Needs Android 7 or above");
            }
        }
    }
}
