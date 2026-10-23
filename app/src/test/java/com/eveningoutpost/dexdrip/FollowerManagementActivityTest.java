package com.eveningoutpost.dexdrip;

import android.app.Dialog;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;

import java.util.List;
import java.util.ArrayList;

import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for {@link FollowerManagementActivity}.
 * <p>
 * The screen lists the account's Dexcom Share followers when it opens, and its invite dialog creates a contact and
 * sends it an invitation. Dexcom Share is replaced by a local stand-in: the shared HTTP client is swapped for one
 * whose interceptor answers every call, so no request leaves the machine. Calls run inline on the test thread, the
 * way Retrofit hands responses to the main thread on a device.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class FollowerManagementActivityTest extends RobolectricTestWithConfig {

    private static final String SESSION_ID_KEY = "dexcom_share_session_id";
    private static final String CONTACTS_PATH = "Publisher/ListPublisherAccountSubscriptions";
    private static final String CREATE_CONTACT_PATH = "Publisher/CreateContact";
    private static final String INVITE_PATH = "Publisher/CreateSubscriptionInvitation";

    private final List<String> requestedPaths = new ArrayList<>();
    private MockedStatic<OkHttpWrapper> httpClient;

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        ShadowToast.reset();
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putString(SESSION_ID_KEY, "a-session").commit(); // skips the Share login round-trip
        httpClient = Mockito.mockStatic(OkHttpWrapper.class);
        httpClient.when(OkHttpWrapper::getClient).thenReturn(shareStandIn());
    }

    @After
    public void tearDown() {
        httpClient.close();
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().remove(SESSION_ID_KEY).commit();
    }

    // ===== Follower list =============================================================================================

    /** Opening the screen lists the followers Dexcom Share reports for the account. */
    @Test
    public void open_listsFollowersFromShare() {
        // :: Act
        final FollowerManagementActivity activity = openScreen();
        final ListView list = activity.findViewById(R.id.followerList);

        // :: Verify
        assertThat(list.getAdapter().getCount()).isEqualTo(2);
        assertThat(followerNameAt(list, 0)).isEqualTo("Alice");
        assertThat(followerNameAt(list, 1)).isEqualTo("Bob");
    }

    // ===== Invite ====================================================================================================

    /** A complete invite creates the contact, invites it, reports success, refreshes the list and closes the dialog. */
    @Test
    public void invite_completeForm_sendsInvitation() {
        // :: Setup
        final FollowerManagementActivity activity = openScreen();
        final Dialog dialog = openInviteDialog(activity);
        fillInviteForm(dialog, "Carol", "Mum", "carol@example.com");

        // :: Act
        dialog.findViewById(R.id.saveButton).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Follower invite sent succesfully");
        assertThat(requestedPaths).containsAtLeast(CREATE_CONTACT_PATH, INVITE_PATH, CONTACTS_PATH).inOrder();
        assertThat(dialog.isShowing()).isFalse();
    }

    /** An invite with a field left empty sends nothing, but still closes the dialog. */
    @Test
    public void invite_missingEmail_sendsNothing() {
        // :: Setup
        final FollowerManagementActivity activity = openScreen();
        final Dialog dialog = openInviteDialog(activity);
        fillInviteForm(dialog, "Carol", "Mum", "");

        // :: Act
        dialog.findViewById(R.id.saveButton).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(requestedPaths).doesNotContain(CREATE_CONTACT_PATH);
        assertThat(ShadowToast.shownToastCount()).isEqualTo(0);
        assertThat(dialog.isShowing()).isFalse();
    }

    // ===== Helpers ===================================================================================================

    private FollowerManagementActivity openScreen() {
        final FollowerManagementActivity activity =
                Robolectric.buildActivity(FollowerManagementActivity.class).setup().get();
        shadowOf(Looper.getMainLooper()).idle();
        requestedPaths.clear(); // keep only what the test's own action sends
        return activity;
    }

    private static Dialog openInviteDialog(final FollowerManagementActivity activity) {
        activity.findViewById(R.id.inviteFollower).performClick();
        final Dialog dialog = ShadowDialog.getLatestDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        return dialog;
    }

    private static void fillInviteForm(final Dialog dialog, final String name, final String nickname,
                                       final String email) {
        ((EditText) dialog.findViewById(R.id.followerNameField)).setText(name);
        ((EditText) dialog.findViewById(R.id.followerDisplayNameField)).setText(nickname);
        ((EditText) dialog.findViewById(R.id.followerEmailField)).setText(email);
    }

    private static String followerNameAt(final ListView list, final int position) {
        return ((TextView) list.getAdapter().getView(position, null, list).findViewById(R.id.follwerName))
                .getText().toString();
    }

    /** A client that answers Dexcom Share calls locally and records which endpoints were called. */
    private OkHttpClient shareStandIn() {
        final Dispatcher inline = new Dispatcher(MoreExecutors.newDirectExecutorService());
        return new OkHttpClient.Builder().dispatcher(inline).addInterceptor(chain -> {
            final String path = chain.request().url().encodedPath().replace("/ShareWebServices/Services/", "");
            requestedPaths.add(path);
            final String body;
            switch (path) {
                case CONTACTS_PATH:
                    body = "[{\"ContactName\":\"Alice\"},{\"ContactName\":\"Bob\"}]";
                    break;
                case CREATE_CONTACT_PATH:
                    body = "\"contact-1\"";
                    break;
                default:
                    body = "\"ok\"";
            }
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(body, MediaType.parse("application/json")))
                    .build();
        }).build();
    }
}
