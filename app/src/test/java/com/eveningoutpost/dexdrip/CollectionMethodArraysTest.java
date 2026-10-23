package com.eveningoutpost.dexdrip;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import lombok.val;

/**
 * Pins the index pairing between the collection method labels and their stored values. Deleting an
 * item from either array silently reassigns the collection method of every user whose choice sits
 * after it.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class CollectionMethodArraysTest extends RobolectricTestWithConfig {

    // ===== dex_collection_method entries and entryValues ================================================================

    /** The stored values keep their exact order, and every one of them has a label. */
    @Test
    public void collectionMethodValuesKeepTheirOrder() {
        // :: Setup
        val resources = RuntimeEnvironment.getApplication().getResources();

        // :: Act
        val entries = resources.getStringArray(R.array.DexCollectionMethods);
        val values = resources.getStringArray(R.array.DexCollectionMethodValues);

        // :: Verify
        assertWithMessage("the stored collection method values are unchanged and in order")
                .that(values).asList().containsExactly(
                        "BluetoothWixel", "DexbridgeWixel", "WifiWixel", "WifiBlueToothWixel",
                        "WifiDexbridgeWixel", "DexcomG5", "LimiTTer", "LimiTTerWifi", "LibreWifi",
                        "Follower", "LibreAlarm", "LibreReceiver", "NSEmulator", "Medtrum",
                        "NSFollower", "SHFollower", "WebFollower", "UiBased", "GluPro",
                        "AidexReceiver", "CLFollower", "Disabled").inOrder();
        assertWithMessage("every stored value has a label to pair with")
                .that(entries.length).isEqualTo(values.length);
    }
}
