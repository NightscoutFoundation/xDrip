package com.eveningoutpost.dexdrip.ui.activities;

import androidx.databinding.ObservableField;
import android.os.Bundle;
import android.os.Handler;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.databinding.ActivitySelectAudioDeviceBinding;
import com.eveningoutpost.dexdrip.utils.HeadsetStateReceiver;

/**
 * jamorham
 *
 * Live selection of current audio device for vehicle mode
 *
 * UI responds dynamically to changes in data if a new audio device is connected
 */


public class SelectAudioDevice extends BaseAppCompatActivity {

    private static final String PREF_MAC_STORE = "vehicle-mode-audio-mac";

    ActivitySelectAudioDeviceBinding binding;
    final Handler handler = new Handler();
    private boolean stopping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySelectAudioDeviceBinding.inflate(getLayoutInflater());
        binding.setI(new AudioDeviceOJO());
        binding.setVm(this);

        if (binding.getI().mac.get().equals("")) {
            JoH.static_toast_long(getString(R.string.no_bluetooth_audio_found));
            finish();
            return;
        }

        setContentView(binding.getRoot());
        JoH.fixActionBar(this);

    }


    @Override
    protected void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    protected void onPause() {
        stopPolling();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopPolling();
        super.onDestroy();
    }

    public class AudioDeviceOJO {
        public final ObservableField<String> name = new ObservableField<>();
        public final ObservableField<String> mac = new ObservableField<>();

        AudioDeviceOJO() {
            refresh();
        }

        public void refresh() {
            this.name.set(HeadsetStateReceiver.getLastConnectedName());
            this.mac.set(HeadsetStateReceiver.getLastConnectedMac());
        }
    }


    private void startPolling() {
        stopPolling();
        stopping = false;
        poll.run();
    }

    private void stopPolling() {
        stopping = true;
        handler.removeCallbacks(poll);
    }

    private Runnable poll = new Runnable() {
        @Override
        public void run() {
            binding.getI().refresh();
            if (!stopping) handler.postDelayed(this, 500);
        }
    };


    public void stop() {
        finish();
    }

    public void save() {
        final String mac = binding.getI().mac.get();
        setAudioMac(mac);
        JoH.static_toast_long("Set to: " + binding.getI().name.get() + " " + mac);
        HeadsetStateReceiver.reprocessConnectionIfAlreadyConnected(mac);
        finish();
    }


    public static void setAudioMac(final String mac) {
        if (mac == null || mac.length() == 0) return;
        Pref.setString(PREF_MAC_STORE, mac);
    }

    public static String getAudioMac() {
        return Pref.getString(PREF_MAC_STORE, "<NOT SET>");
    }


}
