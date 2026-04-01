package com.eveningoutpost.dexdrip.sharemodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies AuthenticatingCallback re-authentication and retry behaviour.
 * <p>
 * The callback intercepts HTTP 500 responses, re-authenticates via getSessionId,
 * and retries the original request. A second 500 is forwarded directly to the delegate.
 *
 * @author Asbjørn Aarrestad
 */
@SuppressWarnings("unchecked")
public class AuthenticatingCallbackTest extends RobolectricTestWithConfig {

    private ShareRest shareRest;
    private DexcomShare mockApi;
    private Call<String> mockGetSessionIdCall;

    @Before
    public void setUpCallbackTest() throws Exception {
        shareRest = new ShareRest(RuntimeEnvironment.application, new OkHttpClient());

        mockApi = mock(DexcomShare.class);
        mockGetSessionIdCall = mock(Call.class);
        when(mockApi.getSessionId(anyMap())).thenReturn(mockGetSessionIdCall);

        setField("dexcomShareApi", mockApi);
        setField("password", "testpassword");
        setField("username", "testuser");
    }

    // :: Tests

    /**
     * A 500 response triggers re-authentication via getSessionId, saves the new session ID to
     * SharedPreferences, and then calls onRetry().
     */
    @Test
    public void authenticatingCallback_on500_callsGetSessionIdAndThenOnRetry() {
        // :: Setup
        simulateSuccessfulReAuth("new-session-id");

        AtomicBoolean retryWasCalled = new AtomicBoolean(false);
        ShareRest.AuthenticatingCallback<ResponseBody> callback =
                shareRest.new AuthenticatingCallback<ResponseBody>(noOpCallback()) {
                    @Override
                    public void onRetry() {
                        retryWasCalled.set(true);
                    }
                };

        // :: Act
        callback.onResponse(dummyCall(), Response.error(500, ResponseBody.create(null, "")));

        // :: Verify
        assertThat(retryWasCalled.get()).isTrue();
        verify(mockApi).getSessionId(anyMap());
        assertThat(android.preference.PreferenceManager
                .getDefaultSharedPreferences(RuntimeEnvironment.application)
                .getString("dexcom_share_session_id", null))
                .isEqualTo("new-session-id");
    }

    /**
     * When re-authentication returns a non-2xx HTTP response (e.g. 401), the failure is forwarded
     * to the delegate and onRetry() is not called.
     */
    @Test
    public void authenticatingCallback_on500_whenReAuthReturnsNon2xx_delegatesFailureToDelegate() {
        // :: Setup
        simulateReAuthWithHttpError(401);

        AtomicReference<Throwable> capturedThrowable = new AtomicReference<>();
        AtomicBoolean retryWasCalled = new AtomicBoolean(false);
        Callback<ResponseBody> delegate = new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                capturedThrowable.set(t);
            }
        };

        ShareRest.AuthenticatingCallback<ResponseBody> callback =
                shareRest.new AuthenticatingCallback<ResponseBody>(delegate) {
                    @Override
                    public void onRetry() {
                        retryWasCalled.set(true);
                    }
                };

        // :: Act
        callback.onResponse(dummyCall(), Response.error(500, ResponseBody.create(null, "")));

        // :: Verify
        assertThat(retryWasCalled.get()).isFalse();
        assertThat(capturedThrowable.get()).isNotNull();
        assertThat(capturedThrowable.get().getMessage()).contains("401");
    }

    /**
     * When re-authentication itself fails with a network error, the failure is forwarded to the delegate.
     */
    @Test
    public void authenticatingCallback_on500_whenReAuthFails_delegatesFailureToDelegate() {
        // :: Setup
        RuntimeException networkError = new RuntimeException("network error");
        simulateFailedReAuth(networkError);

        AtomicReference<Throwable> capturedThrowable = new AtomicReference<>();
        Callback<ResponseBody> delegate = new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                capturedThrowable.set(t);
            }
        };

        ShareRest.AuthenticatingCallback<ResponseBody> callback =
                shareRest.new AuthenticatingCallback<ResponseBody>(delegate) {
                    @Override
                    public void onRetry() {}
                };

        // :: Act
        callback.onResponse(dummyCall(), Response.error(500, ResponseBody.create(null, "")));

        // :: Verify
        assertThat(capturedThrowable.get()).isSameInstanceAs(networkError);
    }

    /**
     * A second 500 after re-auth has already been attempted is forwarded directly to the delegate
     * without triggering another re-authentication.
     */
    @Test
    public void authenticatingCallback_onSecond500AfterRetry_delegatesResponseToDelegateWithoutReAuth() {
        // :: Setup
        simulateSuccessfulReAuth("new-session-id");

        AtomicReference<Response<ResponseBody>> capturedResponse = new AtomicReference<>();
        Callback<ResponseBody> delegate = new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                capturedResponse.set(response);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        };

        ShareRest.AuthenticatingCallback<ResponseBody> callback =
                shareRest.new AuthenticatingCallback<ResponseBody>(delegate) {
                    @Override
                    public void onRetry() {}
                };

        Call<ResponseBody> call = dummyCall();

        // :: Act — first 500 triggers re-auth (synchronous via mock)
        callback.onResponse(call, Response.error(500, ResponseBody.create(null, "")));

        // Second 500 (attempts == 1) must go directly to delegate without re-auth
        Response<ResponseBody> second500 = Response.error(500, ResponseBody.create(null, ""));
        callback.onResponse(call, second500);

        // :: Verify
        assertThat(capturedResponse.get()).isSameInstanceAs(second500);
        verify(mockApi, times(1)).getSessionId(anyMap()); // only one re-auth, not two
    }

    /**
     * Non-500 responses are forwarded directly to the delegate without re-authentication.
     */
    @Test
    public void authenticatingCallback_onSuccessResponse_delegatesDirectlyToDelegateWithoutReAuth() {
        // :: Setup
        AtomicReference<Response<ResponseBody>> capturedResponse = new AtomicReference<>();
        Callback<ResponseBody> delegate = new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                capturedResponse.set(response);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        };

        ShareRest.AuthenticatingCallback<ResponseBody> callback =
                shareRest.new AuthenticatingCallback<ResponseBody>(delegate) {
                    @Override
                    public void onRetry() {}
                };

        // :: Act
        Response<ResponseBody> successResponse = Response.success(null);
        callback.onResponse(dummyCall(), successResponse);

        // :: Verify
        assertThat(capturedResponse.get()).isSameInstanceAs(successResponse);
        verify(mockApi, times(0)).getSessionId(anyMap());
    }

    /**
     * Network failures are forwarded directly to the delegate without triggering re-auth.
     */
    @Test
    public void authenticatingCallback_onFailure_delegatesDirectlyToDelegateWithoutReAuth() {
        // :: Setup
        RuntimeException expectedException = new RuntimeException("connection refused");
        AtomicReference<Throwable> capturedThrowable = new AtomicReference<>();

        Callback<ResponseBody> delegate = new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                capturedThrowable.set(t);
            }
        };

        ShareRest.AuthenticatingCallback<ResponseBody> callback =
                shareRest.new AuthenticatingCallback<ResponseBody>(delegate) {
                    @Override
                    public void onRetry() {}
                };

        // :: Act
        callback.onFailure(dummyCall(), expectedException);

        // :: Verify
        assertThat(capturedThrowable.get()).isSameInstanceAs(expectedException);
        verify(mockApi, times(0)).getSessionId(anyMap());
    }

    // :: Helpers

    /** Configures mockGetSessionIdCall to invoke onResponse with the given session id. */
    private void simulateSuccessfulReAuth(String sessionId) {
        Answer<Void> answer = invocation -> {
            Callback<String> cb = invocation.getArgument(0);
            cb.onResponse(mockGetSessionIdCall, Response.success(sessionId));
            return null;
        };
        doAnswer(answer).when(mockGetSessionIdCall).enqueue(any(Callback.class));
    }

    /** Configures mockGetSessionIdCall to invoke onFailure with the given throwable. */
    private void simulateFailedReAuth(Throwable t) {
        Answer<Void> answer = invocation -> {
            Callback<String> cb = invocation.getArgument(0);
            cb.onFailure(mockGetSessionIdCall, t);
            return null;
        };
        doAnswer(answer).when(mockGetSessionIdCall).enqueue(any(Callback.class));
    }

    /** Configures mockGetSessionIdCall to invoke onResponse with the given HTTP error code. */
    private void simulateReAuthWithHttpError(int code) {
        Answer<Void> answer = invocation -> {
            Callback<String> cb = invocation.getArgument(0);
            cb.onResponse(mockGetSessionIdCall, Response.error(code, ResponseBody.create(null, "")));
            return null;
        };
        doAnswer(answer).when(mockGetSessionIdCall).enqueue(any(Callback.class));
    }

    private void setField(String name, Object value) throws Exception {
        Field f = ShareRest.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(shareRest, value);
    }

    private <T> Callback<T> noOpCallback() {
        return new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {}

            @Override
            public void onFailure(Call<T> call, Throwable t) {}
        };
    }

    private <T> Call<T> dummyCall() {
        return new Call<T>() {
            @Override
            public Response<T> execute() {
                return null;
            }

            @Override
            public void enqueue(Callback<T> callback) {}

            @Override
            public boolean isExecuted() {
                return false;
            }

            @Override
            public void cancel() {}

            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            @SuppressWarnings("CloneDoesntCallSuperClone")
            public Call<T> clone() {
                return this;
            }

            @Override
            public Request request() {
                return new Request.Builder().url("http://example.com").build();
            }
        };
    }
}
