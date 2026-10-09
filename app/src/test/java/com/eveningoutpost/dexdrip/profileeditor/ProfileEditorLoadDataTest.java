package com.eveningoutpost.dexdrip.profileeditor;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests that a stored insulin profile, written by an earlier xDrip version, is read back unchanged.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class ProfileEditorLoadDataTest extends RobolectricTestWithConfig {

    // ===== Stored profile ============================================================================================

    /** A saved profile list is read back with its carb ratio and sensitivity intact. */
    @Test
    public void loadData_readsSavedProfileWrittenByOldVersion() {
        // :: Setup
        Pref.setString("saved_profile_list_json_working", "");
        Pref.setString("saved_profile_list_json",
                "[{\"day_of_week\":0,\"start_min\":0,\"end_min\":719,\"carb_ratio\":10.0,\"sensitivity\":54.0,"
                        + "\"absorption_rate\":35.0},{\"day_of_week\":0,\"start_min\":720,\"end_min\":1439,"
                        + "\"carb_ratio\":12.5,\"sensitivity\":60.0,\"absorption_rate\":35.0}]");

        // :: Act
        final List<ProfileItem> items = ProfileEditor.loadData(false);

        // :: Verify
        assertThat(items).hasSize(2);
        assertThat(items.get(0).end_min).isEqualTo(719);
        assertThat(items.get(0).carb_ratio).isEqualTo(10.0);
        assertThat(items.get(0).sensitivity).isEqualTo(54.0);
        assertThat(items.get(1).start_min).isEqualTo(720);
        assertThat(items.get(1).carb_ratio).isEqualTo(12.5);
    }
}
