package com.eveningoutpost.dexdrip.wearintegration;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;

import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for how {@link WatchUpdaterService} uses the default preference store: the settings listener it
 * registers when created, the "watch integration" check when a watch disconnects, and the settings a watch sends
 * back to the phone.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class WatchUpdaterServicePreferenceStoreTest extends RobolectricTestWithConfig {

    private static final String TAG = "jamorham watchupdater";
    private static final String PREF_DATA_PATH = "/xdrip_plus_watch_pref_data";
    private static final String WEAR_UPDATE_TASK = "wear-update-settings";

    private ServiceController<WatchUpdaterService> service;

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        store().edit().clear().commit();
        ShadowLog.clear();
        // A no-op holds the settings listener's task, so the listener's writes extend it rather than queue a push to
        // a watch that is not there (which would reach for the wearable API). It lapses 2 s after the last write.
        Inevitable.task(WEAR_UPDATE_TASK, 5 * Constants.MINUTE_IN_MS, () -> {});
    }

    /** Stops what a test left running, so it cannot reach into the next test. */
    @After
    public void tearDown() {
        if (service != null) service.destroy();
        Inevitable.kill(WEAR_UPDATE_TASK);
    }

    // ===== Settings listener =========================================================================================

    /** A setting changed while the service runs triggers an update of the watch's settings. */
    @Test
    public void aChangedSettingTriggersAWearUpdate() {
        // :: Setup
        startService();

        // :: Act
        store().edit().putBoolean("show_wear_treatments", true).commit();

        // :: Verify
        assertThat(serviceLog()).contains("Triggering Wear Settings Update due to key=show_wear_treatments");
    }

    // ===== Watch disconnects =========================================================================================

    /** With watch integration on, a disconnected watch makes the phone turn to its own collector. */
    @Test
    public void disconnectWithWatchIntegrationOnTurnsToThePhoneCollector() {
        // :: Setup
        store().edit().putBoolean("watch_integration", true).commit();
        startService();

        // :: Act
        service.get().onPeerDisconnected(watch());

        // :: Verify
        assertThat(serviceLog()).contains("onPeerDisconnected watch_integration=true Phone startBtService");
    }

    /** With watch integration off, a disconnected watch changes nothing. */
    @Test
    public void disconnectWithWatchIntegrationOffDoesNothing() {
        // :: Setup
        startService();

        // :: Act
        service.get().onPeerDisconnected(watch());

        // :: Verify
        assertThat(serviceLog()).doesNotContain("onPeerDisconnected watch_integration=true Phone startBtService");
    }

    // ===== Settings from the watch ===================================================================================

    /** A watch node sent by the watch is stored where the phone's settings live. */
    @Test
    public void watchNodeFromTheWatchIsStored() {
        // :: Setup
        startService(); // with wear sync off, so creating the service does not reach for the wearable API
        store().edit().putBoolean("wear_sync", true).commit();
        service.get().onStartCommand(null, 0, 1); // picks up wear sync; with no API client it only logs
        final DataMap fromWatch = new DataMap();
        fromWatch.putString("node_wearG5", "Watch|1234");

        // :: Act
        service.get().onMessageReceived(message(PREF_DATA_PATH, fromWatch.toByteArray()));

        // :: Verify
        assertThat(store().getString("node_wearG5", "")).isEqualTo("Watch|1234");
    }

    // =================================================================================================================

    private void startService() {
        service = Robolectric.buildService(WatchUpdaterService.class).create();
    }

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    private static List<String> serviceLog() {
        return ShadowLog.getLogsForTag(TAG).stream().map(l -> l.msg).collect(Collectors.toList());
    }

    private static Node watch() {
        return new Node() {
            @Override
            public String getId() {
                return "1234";
            }

            @Override
            public String getDisplayName() {
                return "Watch";
            }

            @Override
            public boolean isNearby() {
                return true;
            }
        };
    }

    private static MessageEvent message(final String path, final byte[] data) {
        return new MessageEvent() {
            @Override
            public int getRequestId() {
                return 1;
            }

            @Override
            public String getPath() {
                return path;
            }

            @Override
            public byte[] getData() {
                return data;
            }

            @Override
            public String getSourceNodeId() {
                return "1234";
            }
        };
    }
}
