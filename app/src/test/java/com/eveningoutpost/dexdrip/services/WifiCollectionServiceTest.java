package com.eveningoutpost.dexdrip.services;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import lombok.val;

/**
 * Pins the uploader battery row on the System Status page across the rename.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class WifiCollectionServiceTest extends RobolectricTestWithConfig {

    @Before
    public void before() {
        cleanup();
    }

    @After
    public void after() {
        cleanup();
    }

    private void cleanup() {
        Pref.removeItem("parakeet_battery");
    }

    // ===== megaStatus ===================================================================================================

    /** A stored uploader battery produces a status row carrying that percentage. */
    @Test
    public void megaStatusReportsTheUploaderBattery() {
        // :: Setup
        Pref.setInt("parakeet_battery", 42);

        // :: Act
        val items = WifiCollectionService.megaStatus(RuntimeEnvironment.getApplication().getApplicationContext());

        // :: Verify
        String uploaderValue = null;
        for (val item : items) {
            if ("Uploader Battery".equals(item.name)) uploaderValue = item.value;
        }
        assertWithMessage("the uploader battery row shows the stored percentage")
                .that(uploaderValue).isEqualTo("42%");
    }
}
