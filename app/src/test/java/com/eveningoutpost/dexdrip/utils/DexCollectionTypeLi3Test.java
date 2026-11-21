package com.eveningoutpost.dexdrip.utils;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.li3.Li3Service;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Test;

/**
 * Tests for the Li3 collector's registration in {@link DexCollectionType}: the settings name it is stored under,
 * the Bluetooth and newer-collector sets, its NFC settings, its service, its default sample period, and that it is
 * never calibrated.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class DexCollectionTypeLi3Test extends RobolectricTestWithConfig {

    @After
    public void tearDown() {
        Pref.removeItem(DexCollectionType.DEX_COLLECTION_METHOD);
    }

    // ================================================= Registration ==================================================

    /** The collector is stored under a name of its own, so selecting it survives a restart. */
    @Test
    public void getType_internalName_isLi3() {
        // :: Act
        DexCollectionType type = DexCollectionType.getType("Li3");

        // :: Verify
        assertThat(type).isEqualTo(DexCollectionType.Li3);
    }

    /** Selected, it counts as a Bluetooth collector started the way the newer collectors are. */
    @Test
    public void selected_usesBluetoothAsNewerCollector() {
        // :: Setup
        DexCollectionType.setDexCollectionType(DexCollectionType.Li3);

        // :: Act
        boolean bluetooth = DexCollectionType.hasBluetooth();
        boolean newer = DexCollectionType.isNewerCollector();

        // :: Verify
        assertThat(bluetooth).isTrue();
        assertThat(newer).isTrue();
        assertThat(DexCollectionType.hasLibre()).isFalse();
    }

    /** The collector runs in its own service. */
    @Test
    public void getCollectorServiceClass_li3_isLi3Service() {
        // :: Act
        Class<?> service = DexCollectionType.getCollectorServiceClass(DexCollectionType.Li3);

        // :: Verify
        assertThat(service).isEqualTo(Li3Service.class);
    }

    // ==================================================== NFC scan ===================================================

    /** A Li3 sensor is started by an NFC scan, so the NFC settings show for it as for the older collectors. */
    @Test
    public void hasNfcScan_li3_true() {
        // :: Act
        boolean nfc = DexCollectionType.hasNfcScan(DexCollectionType.Li3);

        // :: Verify
        assertThat(nfc).isTrue();
    }

    /** The older collectors keep their NFC settings. */
    @Test
    public void hasNfcScan_libreAlarm_true() {
        // :: Act
        boolean nfc = DexCollectionType.hasNfcScan(DexCollectionType.LibreAlarm);

        // :: Verify
        assertThat(nfc).isTrue();
    }

    /** A collector without an NFC sensor does not get the NFC settings. */
    @Test
    public void hasNfcScan_dexcomG5_false() {
        // :: Act
        boolean nfc = DexCollectionType.hasNfcScan(DexCollectionType.DexcomG5);

        // :: Verify
        assertThat(nfc).isFalse();
    }

    // ================================================= Sample period =================================================

    /** Without the one-minute setting the collector reports every five minutes, as the older collectors do. */
    @Test
    public void getCollectorSamplePeriod_default_fiveMinutes() {
        // :: Act
        long period = DexCollectionType.getCollectorSamplePeriod(DexCollectionType.Li3);

        // :: Verify
        assertThat(period).isEqualTo(300_000L);
    }

    // ================================================== Calibration ==================================================

    /** Li3 is the only collector xDrip never calibrates. */
    @Test
    public void isNoCalibration_onlyLi3() {
        for (DexCollectionType type : DexCollectionType.values()) {
            // :: Setup
            DexCollectionType.setDexCollectionType(type);

            // :: Act
            boolean noCalibration = DexCollectionType.isNoCalibration();

            // :: Verify
            assertWithMessage(type.name()).that(noCalibration).isEqualTo(type == DexCollectionType.Li3);
        }
    }
}
