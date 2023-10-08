package com.eveningoutpost.dexdrip.utilitymodels;

/**
 * jamorham
 *
 * Vehicle mode abstraction interface
 */

// TODO move elements relating only to vehicle mode from ActivityRecognizedService to here

import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.services.ActivityRecognizedService;

import static com.eveningoutpost.dexdrip.utilitymodels.Intents.ACTION_VEHICLE_MODE;
import static com.eveningoutpost.dexdrip.utilitymodels.Intents.EXTRA_VEHICLE_MODE_ENABLED;

public class VehicleMode {

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse("vehicle_mode_enabled");
    }

    public static boolean viaCarAudio() {
        return Pref.getBooleanDefaultFalse("vehicle_mode_via_car_audio");
    }

    public static boolean shouldPlaySound() {
        return Pref.getBooleanDefaultFalse("play_sound_in_vehicle_mode");
    }

    public static boolean shouldUseSpeech() {
        return Pref.getBooleanDefaultFalse("speak_readings_in_vehicle_mode");
    }

    public static boolean shouldSpeak() {
        return isEnabled() && shouldUseSpeech() && isVehicleModeActive();
    }

    // TODO extract functionality for this from ActivityRecognizedService
    public static boolean isVehicleModeActive() {
        return ActivityRecognizedService.is_in_vehicle_mode();
    }

    public static void setVehicleModeActive(final boolean active) {
        ActivityRecognizedService.set_vehicle_mode(active);
    }

    public static void sendBroadcast() {
        if (SendXdripBroadcast.enabled()) {
            final Bundle bundle = new Bundle();
            bundle.putString(EXTRA_VEHICLE_MODE_ENABLED, isVehicleModeActive() ? "true" : "false");
            SendXdripBroadcast.send(new Intent(ACTION_VEHICLE_MODE), bundle);
        }
    }
}
