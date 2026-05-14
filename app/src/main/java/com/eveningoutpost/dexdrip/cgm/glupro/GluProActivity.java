package com.eveningoutpost.dexdrip.cgm.glupro;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.databinding.ActivityGluProBinding;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.utils.LocationHelper;

import lombok.val;
import lwld.glucose.profile.iface.State;

/**
 * JamOrHam
 * <p>
 * Glucose Profile device selection activity
 */
public class GluProActivity extends ActivityWithMenu {

    private static final String TAG = GluProActivity.class.getSimpleName();

    public final ViewModel viewModel = ViewModelProvider.get();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        val binding = ActivityGluProBinding.inflate(getLayoutInflater());
        binding.setViewModel(viewModel);
        viewModel.setActivityCallback(GluProActivity.this::finish);
        setContentView(binding.getRoot());

        JoH.fixActionBar(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if ((LocationHelper.requestLocationForBluetooth(this))
                    && viewModel.lastState.get() == State.INSUFFICIENT_PERMISSIONS) {
                UserError.Log.d(TAG, "Got permission so restarting service");
                CollectionServiceStarter.restartCollectionServiceBackground();
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Exception requesting location: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        viewModel.setActivityCallback(null);
        super.onDestroy();
    }

    @Override
    public String getMenuName() {
        return "";
    }

}