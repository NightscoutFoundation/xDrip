package com.eveningoutpost.dexdrip;

/**
 * Created by jamorham on 01/10/2017.
 */


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.schibsted.spain.barista.BaristaClickActions.click;
import static com.schibsted.spain.barista.BaristaScrollActions.scrollTo;
import static com.schibsted.spain.barista.custom.NestedEnabledScrollToAction.scrollTo;
import static org.hamcrest.core.AllOf.allOf;

import android.app.Activity;
import android.content.Context;
import android.view.WindowManager;

import androidx.test.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.espresso.ViewInteraction;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.schibsted.spain.barista.flakyespresso.AllowFlaky;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.File;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HomeEspressoTest {

    private static boolean onetime = false;
    @UiThreadTest
    @Before
    public synchronized void setUp() throws Exception {
        if (!onetime) {
            onetime = true;
            clearAllPrefs();
        }

        final Activity activity = mActivityRule.getActivity();
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    @After
    public synchronized void tearDown() throws Exception {
    }

    //@Rule public ClearPreferencesRule clearPreferencesRule = new ClearPreferencesRule(); // Clear all app's SharedPreferences before every test
    //@Rule public ClearDatabaseRule clearDatabaseRule = new ClearDatabaseRule(); // Delete all tables from all the app's SQLite Databases
    //@Rule public ClearFilesRule clearFilesRule = new ClearFilesRule(); // Delete all files in getFilesDir() and getCacheDir()

    @Rule
    public ActivityTestRule<Home> mActivityRule =
            new ActivityTestRule<>(Home.class);

    @Test
    @AllowFlaky(attempts = 5)
    public void A0_clear_prefs() {
        // null test just to reset state
        clearAllPrefs();
    }

    @Test
    @AllowFlaky(attempts = 5)
    public void A1_accept_warning() {
        // accept the warning
        scrollTo(R.id.saveButton2);
        click(R.id.agreeCheckBox2);

        click(R.id.saveButton2);

    }

    @Test
    @AllowFlaky(attempts = 5)
    public void A2_accept_license() {
        // accept the license agreement
        // TODO note espresso recorder uses hardcoded strings
        ViewInteraction appCompatCheckBox2 = onView(
                allOf(withId(R.id.agreeCheckBox), withText("I UNDERSTAND AND AGREE")));
        appCompatCheckBox2.perform(scrollTo(), click());

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.saveButton), withText("Save")));
        appCompatButton2.perform(scrollTo(), click());

    }

    @Test
    @AllowFlaky(attempts = 5)
    public void B1_checkExperienceDialogAppears() {
        onView(withText("5.5"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click());
    }


    @Test
    @AllowFlaky(attempts = 5)
    public void C1_checkNoteDialogAppears() {
        // check the add note dialog appears
        onView(withId(R.id.btnNote)).perform(click());
        onView(withId(R.id.default_to_voice_input)).check(matches(withText(R.string.default_to_voice_input_next_time)));
    }


    private static void clearAllPrefs() {
        System.out.println("Clearing Prefs");
        File root = InstrumentationRegistry.getTargetContext().getFilesDir().getParentFile();
        String[] sharedPreferencesFileNames = new File(root, "shared_prefs").list();
        for (String fileName : sharedPreferencesFileNames) {
            InstrumentationRegistry.getTargetContext().getSharedPreferences(fileName.replace(".xml", ""), Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

    // sleeps for a period of seconds
    private static void sleep(long timeout) {
        try {
            Thread.sleep(timeout * 1000);
        } catch (InterruptedException e) {
            //
        }
    }

}


