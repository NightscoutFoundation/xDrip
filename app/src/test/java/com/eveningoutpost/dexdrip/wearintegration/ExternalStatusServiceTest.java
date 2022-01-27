package com.eveningoutpost.dexdrip.wearintegration;

import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.EXTERNAL_STATUS_STORE;
import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.EXTERNAL_STATUS_STORE_TIME;
import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.getTBR;

import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.getTBRInt;
import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;

import org.junit.Test;

import lombok.val;

// jamorham

public class ExternalStatusServiceTest extends RobolectricTestWithConfig {

    final String tbr1 =  "Loop Disabled\n10% 0.55U 0g"; // typical
    final String tbr2 =  "Loop Disabled\n999% 15% 0.55U 0g"; // weird
    final String tbr3 =  "235%"; // weird

    @Test
    public void getTBRTest() {

        val tbnull = getTBR(null);
        assertWithMessage("TBR null status err").that(tbnull).isEqualTo("");
        val tbempty = getTBR("");
        assertWithMessage("TBR empty status err").that(tbempty).isEqualTo("");
        val tbrandom = getTBR("hello world");
        assertWithMessage("TBR invalid status default").that(tbrandom).isEqualTo("100%");

        val tb1 = getTBR(tbr1);
        assertWithMessage("TBR 1 status ok").that(tb1).isEqualTo("10%");
        val tb2 = getTBR(tbr2);
        assertWithMessage("TBR 2 status ok").that(tb2).isEqualTo("15%");
        val tb3 = getTBR(tbr3);
        assertWithMessage("TBR 3 status ok").that(tb3).isEqualTo("235%");

        PersistentStore.setLong(EXTERNAL_STATUS_STORE_TIME, JoH.tsl());
        PersistentStore.setString(EXTERNAL_STATUS_STORE, tbr1);
        assertWithMessage("TBR 1 int ok").that(getTBRInt()).isEqualTo(10);
        PersistentStore.setString(EXTERNAL_STATUS_STORE, tbr2);
        assertWithMessage("TBR 2 int ok").that(getTBRInt()).isEqualTo(15);
        PersistentStore.setString(EXTERNAL_STATUS_STORE, tbr3);
        assertWithMessage("TBR 3 int ok").that(getTBRInt()).isEqualTo(235);
        PersistentStore.setString(EXTERNAL_STATUS_STORE, ""); // clear out value after test
    }

}
