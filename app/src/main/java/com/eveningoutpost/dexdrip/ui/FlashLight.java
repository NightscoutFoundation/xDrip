package com.eveningoutpost.dexdrip.ui;

import static android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.xdrip;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * Use camera flash to attract attention
 */


public class FlashLight {

    private static final String TAG = "Torch";
    private static final AtomicBoolean running = new AtomicBoolean();

    public static void torchPulse() {
        Inevitable.task("torch-pulse", 100, () -> torchPulseReal(30, true));
    }

    private static synchronized void torchPulseReal(final int seconds, final boolean stopIfNoAlert) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (running.get()) {
                Log.e(TAG, "Already flashing flashlight - skipping");
                return;
            }
            val manager = (CameraManager) xdrip.getAppContext().getSystemService(Context.CAMERA_SERVICE);
            if (manager == null) {
                Log.e(TAG, "Cannot get camera manager");
                return;
            }

            val flashes = new ArrayList<String>();
            val rnd = new SecureRandom();

            try {
                running.set(true);

                val cameraIds = manager.getCameraIdList();
                // reduce list to cameras which say they have flash
                for (val camera : cameraIds) {
                    val characteristics = manager.getCameraCharacteristics(camera);
                    val flashy = characteristics.get(FLASH_INFO_AVAILABLE);
                    if (flashy == null || flashy) {
                        flashes.add(camera);
                    }
                }

                val timeEnd = tsl() + Constants.SECOND_IN_MS * seconds;

                // Flash light randomly until time period is elapsed
                int i = 0;
                while (tsl() < timeEnd) {
                    setCameraList(manager, flashes, i % 2 == 0);
                    JoH.threadSleep(100 + Math.abs(rnd.nextInt() % (Math.abs(400 - i))));
                    i++;
                    if (stopIfNoAlert && JoH.quietratelimit("check-bgalert", 1)) {
                        if (!ActiveBgAlert.currentlyAlerting()) {
                            Log.d(TAG, "Exiting flash sequence as no active alert");
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Cannot access flashlight: " + e);
            } finally {
                setCameraList(manager, flashes, false); // turn off at end
                running.set(false);
            }
        } else {
            Log.e(TAG, "Flashlight torch access requires at least Android 6");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void setCameraList(final CameraManager manager, final ArrayList<String> flashes, final boolean on) {
        try {
            for (val camera : flashes) {
                try {
                    manager.setTorchMode(camera, on);
                } catch (CameraAccessException | IllegalArgumentException e) {
                    // doesn't have flash
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in setCameraList: " + e);
        }
    }
}
