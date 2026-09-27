package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowMediaPlayer;
import org.robolectric.shadows.util.DataSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for how {@code AlertPlayer.playFile} treats a system-default sound URI.
 * <p>
 * A ringtone picker hands back {@code content://settings/system/...} when the user picks "Default",
 * and the notification preferences persisted the same URIs as their defaults until #4680. The player
 * treats every such value as "unset" and never opens it, but used to substitute the bundled glucose
 * alarm at every priority, which is how a reminder ended up sounding like a low-glucose alert. Below
 * priority 80 the substitute must be the soft notification sound, exactly as for the plain "default"
 * marker; at 80 and above the alarm is deliberate, since the legacy alert rows for high and low
 * glucose carry the same URI as an old default.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class AlertPlayerSystemSoundTest extends RobolectricTestWithConfig {

    private static final String SYSTEM_NOTIFICATION_SOUND = "content://settings/system/notification_sound";
    private static final String REMINDER = "reminder";               // priority 50
    private static final String PREDICTED_GLUCOSE = "bg_predict_alert"; // priority 80
    private static final String HIGH_GLUCOSE = "high_glucose_level";  // priority 85

    /** The sounds the media player was given, by name, in the order it was given them. */
    private final List<String> requestedSounds = new ArrayList<>();

    @Before
    public void recordEverySoundTheMediaPlayerIsGiven() {
        // A raw resource is opened at a fixed offset inside the resource container, which is the only
        // stable part of its descriptor: the default transform keys on the descriptor's identity.
        DataSource.setFileDescriptorTransform((fd, offset) -> "resource@" + offset);
        final Map<DataSource, String> names = new HashMap<>();
        names.put(rawResourceSource(R.raw.default_notification), "default_notification");
        names.put(rawResourceSource(R.raw.default_alert), "default_alert");
        names.put(DataSource.toDataSource(context(), Uri.parse(SYSTEM_NOTIFICATION_SOUND)), SYSTEM_NOTIFICATION_SOUND);
        ShadowMediaPlayer.setMediaInfoProvider(dataSource -> {
            requestedSounds.add(names.getOrDefault(dataSource, "unknown source"));
            return new ShadowMediaPlayer.MediaInfo(1000, 0);
        });
        // A tag left behind by another test class, whose media player may still be playing, would
        // silently block every sound of a lower priority.
        AlertPlayer.activeTag = "";
    }

    @After
    public void clearStateLeftForOtherTestClasses() {
        ShadowMediaPlayer.resetStaticState(); // Also resets the file descriptor transform
        AlertPlayer.activeTag = ""; // Cleared by a media player callback the test does not control
    }

    /** A reminder set to the system default sound gets the soft notification sound, not the glucose alarm. */
    @Test
    public void lowPriorityAlertWithASystemDefaultSoundPlaysTheSoftSound() {
        // :: Act
        triggerSound(SYSTEM_NOTIFICATION_SOUND, REMINDER);

        // :: Verify
        assertWithMessage("sounds requested for a reminder with the system default sound")
                .that(requestedSounds)
                .containsExactly("default_notification");
    }

    /** Priority 80 itself is on the alarm side of the boundary. */
    @Test
    public void alertAtTheBoundaryPriorityKeepsTheBundledAlarmForASystemDefaultSound() {
        // :: Act
        triggerSound(SYSTEM_NOTIFICATION_SOUND, PREDICTED_GLUCOSE);

        // :: Verify
        assertWithMessage("sounds requested for a priority-80 alert with the system default sound")
                .that(requestedSounds)
                .containsExactly("default_alert");
    }

    /** Above the boundary the bundled alarm is kept too, here at the priority of the high-glucose alert. */
    @Test
    public void glucoseLevelAlertKeepsTheBundledAlarmForASystemDefaultSound() {
        // :: Act
        triggerSound(SYSTEM_NOTIFICATION_SOUND, HIGH_GLUCOSE);

        // :: Verify
        assertWithMessage("sounds requested for a glucose alert with the system default sound")
                .that(requestedSounds)
                .containsExactly("default_alert");
    }

    /** The plain "default" marker keeps its meaning next to the URI handling: the soft sound below 80. */
    @Test
    public void lowPriorityAlertWithThePlainDefaultMarkerPlaysTheSoftSound() {
        // :: Act
        triggerSound("default", REMINDER);

        // :: Verify
        assertWithMessage("sounds requested for a reminder with the plain default marker")
                .that(requestedSounds)
                .containsExactly("default_notification");
    }

    /** A missing sound name at low priority still falls back to the bundled alarm, as it always has. */
    @Test
    public void lowPriorityAlertWithoutASoundNameKeepsTheBundledAlarm() {
        // :: Act
        triggerSound(null, REMINDER);

        // :: Verify
        assertWithMessage("sounds requested for a reminder without a sound name")
                .that(requestedSounds)
                .containsExactly("default_alert");
    }

    /**
     * Triggers the sound as an alert that overrides silent mode, so the player neither acts on the
     * ringer mode nor refuses to play at a volume of zero. The volume it reads comes from the
     * {@code AudioManager} it took when it was first constructed, which belongs to whichever test class
     * created it and is not one this test can set.
     */
    private void triggerSound(String soundUri, String type) {
        AlertPlayer.getPlayer().triggerSoundAndVibration(context(), true, soundUri, true, 0.3f, type, false, null);
    }

    /** The source the player asks for when it opens a raw resource, keyed the same way as in production. */
    private DataSource rawResourceSource(int resourceId) {
        try (AssetFileDescriptor descriptor = context().getResources().openRawResourceFd(resourceId)) {
            return DataSource.toDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
        } catch (IOException e) {
            throw new AssertionError("raw resource " + resourceId + " could not be opened", e);
        }
    }

    /** The context the player resolves for itself. Which application it belongs to is irrelevant here: a data
     * source is keyed on the URI or the resource offset, not on the context that opened it. */
    private Context context() {
        return xdrip.getAppContext();
    }
}
