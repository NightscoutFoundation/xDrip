package com.eveningoutpost.dexdrip.Services;

import android.widget.ListView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.robolectric.util.Logger;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Ob1G5CollectionServiceTests extends RobolectricTestWithConfig {

    private static Thread.UncaughtExceptionHandler handler;
    private static AtomicBoolean passFail = new AtomicBoolean(true);
    @BeforeClass
    public static void init() {
        handler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                passFail.set(false);
            }
        };
    }

    /**
     * Threading is a really interesting way of trying to pass unit tests. Doesn't work though
     */
    @Test(timeout = 20000)
    public void shouldWeAcceptNegativeValues() {
        Ob1G5CollectionService s = new Ob1G5CollectionService();
        Thread.setDefaultUncaughtExceptionHandler(handler);
        s.background_automata(-10000);

        final CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.error(("Our latch was interrupted...."));
        }
        Assert.assertTrue(passFail.get());

    }


    /**
     * But I'm sure there was a null test in there right?
     */
    @Test
    public void okLetsBeNice() {

        ListView listView = new ListView(null);
        JoH.screenShot(listView,"How about that");

    }

    /**
     * No?
     */
    @Test
    public void okAtLeastThisOneMustHaveANullCheck() {
        Ob1G5CollectionService s = new Ob1G5CollectionService();
        Ob1G5CollectionService.processCalibrationState(null);

    }

    @Test
    public void AtLeastThisShouldBeFailSafe() {
        Ob1G5CollectionService s = new Ob1G5CollectionService();
        char[] ohLama = new char[Integer.MAX_VALUE/2];
        Arrays.fill(ohLama,'o');
        Ob1G5CollectionService.msg(new String(ohLama));
    }

}
