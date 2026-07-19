package com.eveningoutpost.dexdrip.cgm.dex;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the G7 "soak & handoff" scheduling logic.
 *
 * These cover the quiet failure modes: a switch firing at the wrong time, a
 * delay that clamps to an immediate switch, or a queued id being lost.
 */
public class SoakScheduleTest extends RobolectricTestWithConfig {

    private static final long NOW = 1_700_000_000_000L; // fixed, arbitrary "now"
    private static final long MIN = 60000L;

    @Before
    public void resetSchedule() {
        SoakSchedule.clearAll();
    }

    // ---- isDue predicate (pure) -------------------------------------------

    @Test
    public void notDueBeforeGraceWindow() {
        final long switchTime = NOW + 10 * MIN;
        assertFalse(SoakSchedule.isDue("22AB", switchTime, switchTime - SoakSchedule.GRACE_MS - 1));
    }

    @Test
    public void dueExactlyAtGraceWindow() {
        final long switchTime = NOW + 10 * MIN;
        assertTrue(SoakSchedule.isDue("22AB", switchTime, switchTime - SoakSchedule.GRACE_MS));
    }

    @Test
    public void dueAfterSwitchTime() {
        assertTrue(SoakSchedule.isDue("22AB", NOW, NOW + 5000));
    }

    @Test
    public void notDueWhenIdBlankOrNullOrTimeZero() {
        assertFalse("blank id", SoakSchedule.isDue("", NOW, NOW + 10 * MIN));
        assertFalse("null id", SoakSchedule.isDue(null, NOW, NOW + 10 * MIN));
        assertFalse("zero time", SoakSchedule.isDue("22AB", 0, NOW + 10 * MIN));
    }

    // ---- clampDelay (pure) ------------------------------------------------

    @Test
    public void clampsBelowOneUpToOne() {
        assertThat(SoakSchedule.clampDelay(0, 0), is(1));
        assertThat(SoakSchedule.clampDelay(-5, 0), is(1));
    }

    @Test
    public void noCapWhenRemainingUnknown() {
        assertThat(SoakSchedule.clampDelay(500, 0), is(500));
        assertThat(SoakSchedule.clampDelay(500, -1), is(500));
    }

    @Test
    public void capsToRemainingSensorLife() {
        final long remaining = 120 * MIN;
        assertThat("over cap is trimmed", SoakSchedule.clampDelay(500, remaining), is(120));
        assertThat("within cap is unchanged", SoakSchedule.clampDelay(60, remaining), is(60));
    }

    @Test
    public void refusesWhenUnderOneMinuteLeft() {
        // Regression: <1 min remaining previously clamped to 0 and fired the switch immediately.
        assertThat(SoakSchedule.clampDelay(30, 59_000L), is(SoakSchedule.CANNOT_SCHEDULE));
        assertThat(SoakSchedule.clampDelay(1, 30_000L), is(SoakSchedule.CANNOT_SCHEDULE));
    }

    @Test
    public void allowsExactlyOneMinuteLeft() {
        assertThat(SoakSchedule.clampDelay(30, MIN), is(1));
    }

    // ---- switchTimeFor (pure) ---------------------------------------------

    @Test
    public void switchTimeIsNowPlusDelay() {
        assertThat(SoakSchedule.switchTimeFor(NOW, 60), is(NOW + 60 * MIN));
        assertThat(SoakSchedule.switchTimeFor(NOW, 0), is(NOW));
    }

    // ---- queue / cancel lifecycle (Pref-backed) ---------------------------

    @Test
    public void queueSetsAllThreePrefsConsistently() {
        final long switchTime = SoakSchedule.queue("22AB", 60, NOW);
        assertThat(switchTime, is(NOW + 60 * MIN));
        assertThat(SoakSchedule.pendingId(), is("22AB"));
        assertThat(SoakSchedule.switchTime(), is(NOW + 60 * MIN));
        assertThat(SoakSchedule.storedDelayMinutes(), is(60));
        assertTrue(SoakSchedule.isPending());
        assertTrue("due at switch time", SoakSchedule.isDue(switchTime));
        assertFalse("not due at queue time", SoakSchedule.isDue(NOW));
    }

    @Test
    public void deactivateClearsIdAndTimeButRemembersDelay() {
        SoakSchedule.queue("22AB", 45, NOW);
        SoakSchedule.deactivate();
        assertThat(SoakSchedule.pendingId(), is(""));
        assertThat(SoakSchedule.switchTime(), is(0L));
        assertFalse(SoakSchedule.isPending());
        assertThat("delay remembered for next changeover", SoakSchedule.delayMinutesOrDefault(), is(45));
    }

    @Test
    public void clearAllAlsoForgetsDelay() {
        SoakSchedule.queue("22AB", 45, NOW);
        SoakSchedule.clearAll();
        assertThat(SoakSchedule.pendingId(), is(""));
        assertThat(SoakSchedule.switchTime(), is(0L));
        assertThat(SoakSchedule.storedDelayMinutes(), is(0));
        assertThat(SoakSchedule.delayMinutesOrDefault(), is(SoakSchedule.DEFAULT_DELAY_MINUTES));
    }

    @Test
    public void delayPreFillDefaultsWhenNothingStored() {
        assertThat(SoakSchedule.delayMinutesOrDefault(), is(SoakSchedule.DEFAULT_DELAY_MINUTES));
        assertThat(SoakSchedule.storedDelayMinutes(), is(0));
    }

    // The delay-dialog OK path: an empty / non-numeric delay falls back to the default
    // and still stores the id, rather than silently queueing nothing.
    @Test
    public void blankDelayFallsBackToDefaultAndStillQueuesId() {
        final int requested = JoH.tolerantParseInt("", SoakSchedule.DEFAULT_DELAY_MINUTES);
        final int minutes = SoakSchedule.clampDelay(requested, 0);
        SoakSchedule.queue("33CD", minutes, NOW);
        assertThat(SoakSchedule.pendingId(), is("33CD"));
        assertThat(SoakSchedule.storedDelayMinutes(), is(SoakSchedule.DEFAULT_DELAY_MINUTES));
        assertTrue(SoakSchedule.isPending());
    }
}
