package com.eveningoutpost.dexdrip.UtilityModels;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.databinding.ActivityXdripDreamSettingsBinding;

public class XDripDreamSettingsActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityXdripDreamSettingsBinding binding = ActivityXdripDreamSettingsBinding.inflate(getLayoutInflater());
        binding.setPrefs(new PrefsViewImpl());
        setContentView(binding.getRoot());

    }

}
