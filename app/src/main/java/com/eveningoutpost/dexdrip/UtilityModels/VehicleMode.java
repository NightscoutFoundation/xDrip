package com.eveningoutpost.dexdrip.UtilityModels;

/**
 * jamorham
 *
 * Vehicle mode abstraction interface
 */

// TODO move elements relating only to vehicle mode from ActivityRecognizedService to here

import com.eveningoutpost.dexdrip.Services.ActivityRecognizedService;

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

}
