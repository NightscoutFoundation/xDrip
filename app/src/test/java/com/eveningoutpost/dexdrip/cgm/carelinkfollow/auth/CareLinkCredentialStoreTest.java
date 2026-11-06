package com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.google.gson.GsonBuilder;

import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import static com.google.common.truth.Truth.assertThat;

/**
 * {@code CareLinkCredentialStore} saves a {@link CareLinkCredential} with a plain {@code GsonBuilder} and restores
 * it the same way on next app start. If a gson upgrade changes how {@code accessValidTo}/{@code refreshValidTo}
 * (both {@code java.util.Date}) round-trip, the restore's {@code catch (Exception)} drops the credential silently
 * and the CareLink follower is logged out after the update.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class CareLinkCredentialStoreTest extends RobolectricTestWithConfig {

    private static final String PREF_CARELINK_CREDENTIAL = "carelink_credential";

    // ===== Reading a credential written by the old gson ==============================================================

    /**
     * The store's restore runs only on the first {@code getInstance()} call per JVM, and the singleton can't be
     * reset through public API, so this parses gson 2.9.0's default {@code toJson} output for this credential
     * directly, with the store's own restore-path gson configuration. Gson's default Date format is written and
     * read in the JVM's default {@link TimeZone}, and a stored credential was written in the phone's zone, so the
     * default zone is pinned to the one the literal below was captured in (Europe/Oslo) for the test's duration.
     */
    @Test
    public void credential_storedByOldGson_parsesSameDates() {
        // :: Setup
        final TimeZone oldTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"));
        final String oldGsonJson =
                "{\"country\":\"us\",\"accessToken\":\"test-access-token\",\"accessValidTo\":\"Nov 14, 2023, "
                        + "11:13:20 PM\",\"refreshValidTo\":\"Nov 15, 2023, 12:13:20 AM\",\"authType\":\"Browser\"}";

        try {
            // :: Act
            final CareLinkCredential credential =
                    new GsonBuilder().create().fromJson(oldGsonJson, CareLinkCredential.class);

            // :: Verify
            assertThat(credential.accessValidTo.getTime()).isEqualTo(1_700_000_000_000L);
            assertThat(credential.refreshValidTo.getTime()).isEqualTo(1_700_003_600_000L);
            assertThat(credential.country).isEqualTo("us");
        } finally {
            TimeZone.setDefault(oldTimeZone);
        }
    }

    // ===== Round trip through the current gson =======================================================================

    /**
     * A credential saved through the public setter round-trips through the saved JSON to the same instant,
     * read back the same way {@code getInstance()} restores it.
     */
    @Test
    public void setBrowserCredential_roundTripsThroughSavedJson() {
        // :: Setup
        final CareLinkCredentialStore store = CareLinkCredentialStore.getInstance();
        final Date accessValidTo = new Date(1_700_100_000_000L);
        final Date refreshValidTo = new Date(1_700_200_000_000L);

        // :: Act
        store.setBrowserCredential("no", "test-access-token-2", accessValidTo, refreshValidTo, null);
        final String savedJson = PersistentStore.getString(PREF_CARELINK_CREDENTIAL, "");
        final CareLinkCredential reloaded = new GsonBuilder().create().fromJson(savedJson, CareLinkCredential.class);

        // :: Verify
        assertThat(reloaded.accessValidTo.getTime()).isEqualTo(1_700_100_000_000L);
        assertThat(reloaded.refreshValidTo.getTime()).isEqualTo(1_700_200_000_000L);
        assertThat(reloaded.country).isEqualTo("no");
    }

}
