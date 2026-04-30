package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import io.sentry.android.core.SentryAndroidOptions;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for SentryCrashReporting tag-building and gate logic.
 * Package-private methods are accessible because this class is in the same package.
 */
public class SentryCrashReportingTest extends RobolectricTestWithConfig {

    @Before
    public void before() {
        cleanup();
    }

    @After
    public void after() {
        cleanup();
    }

    private void cleanup() {
        Pref.removeItem(DexCollectionType.DEX_COLLECTION_METHOD);
        Pref.removeItem("enable_telemetry");
        Pref.removeItem("cloud_storage_api_enable");
        Pref.removeItem("cloud_storage_mongodb_enable");
        Pref.removeItem("cloud_storage_influxdb_enable");
        Pref.removeItem("cloud_storage_tidepool_enable");
        Pref.removeItem("share_upload");
        Pref.removeItem("health_connect_enable");
        Pref.removeItem("wear_sync");
        Pref.removeItem("broadcast_service_enabled");
        Pref.removeItem("auto_update_download");
    }

    // :: configureOptions

    @Test
    public void configureOptions_sendDefaultPii_isDisabled() {
        // :: Setup — start enabled to prove we explicitly disable it
        SentryAndroidOptions options = new SentryAndroidOptions();
        options.setSendDefaultPii(true);

        // :: Act
        SentryCrashReporting.configureOptions(options);

        // :: Verify
        assertWithMessage("sendDefaultPii must be explicitly disabled for GDPR compliance")
                .that(options.isSendDefaultPii()).isFalse();
    }

    // :: shouldSendWeeklyStatus

    @Test
    public void shouldSendWeeklyStatus_telemetryDisabled_returnsFalse() {
        // :: Setup
        Pref.setBoolean("enable_telemetry", false);

        // :: Verify
        assertWithMessage("gate off when telemetry disabled").that(SentryCrashReporting.shouldSendWeeklyStatus()).isFalse();
    }

    @Test
    public void shouldSendWeeklyStatus_telemetryEnabled_returnsTrue() {
        // :: Setup
        Pref.setBoolean("enable_telemetry", true);

        // :: Verify
        assertWithMessage("gate on when telemetry enabled").that(SentryCrashReporting.shouldSendWeeklyStatus()).isTrue();
    }

    // :: Uploaders

    @Test
    public void buildWeeklyStatusTags_noUploaders_noUploaderTags() {
        // :: Act
        Map<String, String> tags = SentryCrashReporting.buildWeeklyStatusTags();

        // :: Verify
        assertWithMessage("uploader.ns absent").that(tags).doesNotContainKey("uploader.ns");
        assertWithMessage("uploader.mongo absent").that(tags).doesNotContainKey("uploader.mongo");
        assertWithMessage("uploader.influx absent").that(tags).doesNotContainKey("uploader.influx");
        assertWithMessage("uploader.tidepool absent").that(tags).doesNotContainKey("uploader.tidepool");
        assertWithMessage("uploader.share absent").that(tags).doesNotContainKey("uploader.share");
        assertWithMessage("uploader.health absent").that(tags).doesNotContainKey("uploader.health");
    }

    @Test
    public void buildWeeklyStatusTags_nightscoutEnabled_setsNsTag() {
        // :: Setup
        Pref.setBoolean("cloud_storage_api_enable", true);

        // :: Act
        Map<String, String> tags = SentryCrashReporting.buildWeeklyStatusTags();

        // :: Verify
        assertWithMessage("uploader.ns").that(tags.get("uploader.ns")).isEqualTo("true");
        assertWithMessage("uploader.tidepool absent").that(tags).doesNotContainKey("uploader.tidepool");
    }

    @Test
    public void buildWeeklyStatusTags_multipleUploaders_setsIndividualTags() {
        // :: Setup
        Pref.setBoolean("cloud_storage_api_enable", true);
        Pref.setBoolean("cloud_storage_tidepool_enable", true);

        // :: Act
        Map<String, String> tags = SentryCrashReporting.buildWeeklyStatusTags();

        // :: Verify
        assertWithMessage("uploader.ns").that(tags.get("uploader.ns")).isEqualTo("true");
        assertWithMessage("uploader.tidepool").that(tags.get("uploader.tidepool")).isEqualTo("true");
        assertWithMessage("uploader.influx absent").that(tags).doesNotContainKey("uploader.influx");
    }

    // :: Role

    @Test
    public void buildWeeklyStatusTags_nsFollowCollector() {
        // :: Setup
        DexCollectionType.setDexCollectionType(DexCollectionType.NSFollow);

        // :: Act
        Map<String, String> tags = SentryCrashReporting.buildWeeklyStatusTags();

        // :: Verify
        assertWithMessage("collector_type").that(tags.get("collector_type")).isEqualTo("NSFollow");
    }

    @Test
    public void buildWeeklyStatusTags_dexcomG5Collector() {
        // :: Setup
        DexCollectionType.setDexCollectionType(DexCollectionType.DexcomG5);

        // :: Act
        Map<String, String> tags = SentryCrashReporting.buildWeeklyStatusTags();

        // :: Verify
        assertWithMessage("collector_type").that(tags.get("collector_type")).isEqualTo("DexcomG5");
    }

    // :: Wear sync

    @Test
    public void buildWeeklyStatusTags_noWear_wearSyncFalse() {
        // :: Act
        Map<String, String> tags = SentryCrashReporting.buildWeeklyStatusTags();

        // :: Verify
        assertWithMessage("wear_sync").that(tags.get("wear_sync")).isEqualTo("false");
        assertWithMessage("watch_model absent").that(tags).doesNotContainKey("watch_model");
    }

    @Test
    public void buildWeeklyStatusTags_wearEnabled_wearSyncTrue() {
        // :: Setup
        // node_wearG5 format: "Display Name|nodeId" (display name may contain spaces)
        Pref.setBoolean("wear_sync", true);
        Pref.setString("node_wearG5", "Galaxy Watch 4|abc123def456");

        // :: Act
        Map<String, String> tags = SentryCrashReporting.buildWeeklyStatusTags();

        // :: Verify
        assertWithMessage("wear_sync").that(tags.get("wear_sync")).isEqualTo("true");
        assertWithMessage("watch_model absent").that(tags).doesNotContainKey("watch_model");
    }

    // :: Other tags

    @Test
    public void buildWeeklyStatusTags_autoUpdateEnabled_returnsTrue() {
        // :: Setup
        Pref.setBoolean("auto_update_download", true);

        // :: Act
        Map<String, String> tags = SentryCrashReporting.buildWeeklyStatusTags();

        // :: Verify
        assertWithMessage("auto_update").that(tags.get("auto_update")).isEqualTo("true");
    }

    @Test
    public void buildWeeklyStatusTags_autoUpdateDisabled_returnsFalse() {
        // :: Setup
        Pref.setBoolean("auto_update_download", false);

        // :: Act
        Map<String, String> tags = SentryCrashReporting.buildWeeklyStatusTags();

        // :: Verify
        assertWithMessage("auto_update").that(tags.get("auto_update")).isEqualTo("false");
    }

    @Test
    public void buildWeeklyStatusTags_localBroadcastEnabled_returnsTrue() {
        // :: Setup
        Pref.setBoolean("broadcast_service_enabled", true);

        // :: Act
        Map<String, String> tags = SentryCrashReporting.buildWeeklyStatusTags();

        // :: Verify
        assertWithMessage("local_broadcast").that(tags.get("local_broadcast")).isEqualTo("true");
    }

    @Test
    public void buildWeeklyStatusTags_allTagsPresent() {
        // :: Act
        Map<String, String> tags = SentryCrashReporting.buildWeeklyStatusTags();

        // :: Verify
        assertWithMessage("collector_type present").that(tags).containsKey("collector_type");
        assertWithMessage("wear_sync present").that(tags).containsKey("wear_sync");
        assertWithMessage("local_broadcast present").that(tags).containsKey("local_broadcast");
        assertWithMessage("auto_update present").that(tags).containsKey("auto_update");
    }
}
