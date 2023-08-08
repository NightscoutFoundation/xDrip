package com.eveningoutpost.dexdrip.wearintegration;

import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.EXTERNAL_STATUS_STORE;
import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.EXTERNAL_STATUS_STORE_TIME;
import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.getAbsoluteBR;
import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.getAbsoluteBRDouble;
import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.getTBR;

import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.getTBRInt;
import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import org.junit.Test;

import lombok.val;

// jamorham

public class ExternalStatusServiceTest extends RobolectricTestWithConfig {

    final String tbr1 =  "Loop Disabled\n10% 0.55U 0g"; // typical
    final String tbr2 =  "Loop Disabled\n999% 15% 0.55U 0g"; // weird
    final String tbr3 =  "235%"; // weird
    final String tbr4 =  "0.00U/h 1.44U(3.12|-1.69)"; // typical
    final String tbr5 =  "Loop Disabled 1.234U/h 1.44U(3.12|-1.69)"; // typical 2
    final String tbr6 =  "Bonjour 2,345U/h 1,04U(1,11|-0,07) -0,35 4(20)g"; // typical 3 french locale

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
        val tb4 = getTBR(tbr4);
        assertWithMessage("TBR 4 status pass thru").that(tb4).isEqualTo("100%");
        val tb5 = getTBR(tbr5);
        assertWithMessage("TBR 5 status pass thru").that(tb5).isEqualTo("100%");

        PersistentStore.setLong(EXTERNAL_STATUS_STORE_TIME, JoH.tsl());
        PersistentStore.setString(EXTERNAL_STATUS_STORE, tbr1);
        assertWithMessage("TBR 1 int ok").that(getTBRInt()).isEqualTo(10);
        PersistentStore.setString(EXTERNAL_STATUS_STORE, tbr2);
        assertWithMessage("TBR 2 int ok").that(getTBRInt()).isEqualTo(15);
        PersistentStore.setString(EXTERNAL_STATUS_STORE, tbr3);
        assertWithMessage("TBR 3 int ok").that(getTBRInt()).isEqualTo(235);
        PersistentStore.setString(EXTERNAL_STATUS_STORE, ""); // clear out value after test
    }

    @Test
    public void getAbsoluteBrTest() {
        assertWithMessage("TBR 1 non").that(getAbsoluteBR(tbr1)).isNull();
        assertWithMessage("TBR 2 non").that(getAbsoluteBR(tbr2)).isNull();
        assertWithMessage("TBR 3 non").that(getAbsoluteBR(tbr3)).isNull();
        assertWithMessage("AB 4 match").that(getAbsoluteBR(tbr4)).isEqualTo("0.00U/h");
        assertWithMessage("AB 5 match").that(getAbsoluteBR(tbr5)).isEqualTo("1.234U/h");
        PersistentStore.setLong(EXTERNAL_STATUS_STORE_TIME, JoH.tsl());
        PersistentStore.setString(EXTERNAL_STATUS_STORE, tbr4);
        assertWithMessage("AB 4 match double").that(getAbsoluteBRDouble()).isEqualTo(0.00d);
        PersistentStore.setString(EXTERNAL_STATUS_STORE, tbr5);
        assertWithMessage("AB 5 match double").that(getAbsoluteBRDouble()).isEqualTo(1.234d);
        PersistentStore.setString(EXTERNAL_STATUS_STORE, tbr6);
        assertWithMessage("AB 6 match double").that(getAbsoluteBRDouble()).isEqualTo(2.345d);
        PersistentStore.setString(EXTERNAL_STATUS_STORE, tbr1);
        assertWithMessage("TBR 1 pass thru").that(getAbsoluteBRDouble()).isNull();
        PersistentStore.setString(EXTERNAL_STATUS_STORE, tbr2);
        assertWithMessage("TBR 2 pass thru").that(getAbsoluteBRDouble()).isNull();
        PersistentStore.setString(EXTERNAL_STATUS_STORE, tbr3);
        assertWithMessage("TBR 3 pass thru").that(getAbsoluteBRDouble()).isNull();
        PersistentStore.setString(EXTERNAL_STATUS_STORE, ""); // clear out value after test
    }

}
