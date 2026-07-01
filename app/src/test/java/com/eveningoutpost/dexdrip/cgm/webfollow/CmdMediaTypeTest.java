package com.eveningoutpost.dexdrip.cgm.webfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.lang.reflect.Field;

import okhttp3.MediaType;

/**
 * Characterization test pinning the private {@code Cmd.JSON} media type constant.
 *
 * The full body-building path runs inside {@link Cmd#process(String)} with heavy
 * context, so this reflects on the constant directly. It pins exactly the surface the
 * Phase C5 {@code MediaType.parse -> MediaType.get} change could alter.
 *
 * @author Asbjørn Aarrestad
 */
public class CmdMediaTypeTest extends RobolectricTestWithConfig {

    @Test
    public void jsonConstant_isApplicationJson() throws Exception {
        // :: Setup
        final Field jsonField = Cmd.class.getDeclaredField("JSON");
        jsonField.setAccessible(true);

        // :: Act
        final MediaType json = (MediaType) jsonField.get(null);

        // :: Verify
        assertThat(json).isNotNull();
        assertThat(json.toString()).isEqualTo("application/json");
    }
}
