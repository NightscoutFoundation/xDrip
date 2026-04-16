package com.eveningoutpost.dexdrip.nocturne;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

/**
 * OAuth service implementing Dynamic Client Registration (RFC 7591)
 * and Device Authorization Grant (RFC 8628) for Nocturne.
 */
public class NocturneOAuthService {

    private static final String TAG = "NocturneOAuth";
    private static final String SOFTWARE_ID = "org.nightscoutfoundation.xdrip";
    private static final String REQUESTED_SCOPES = "entries.readwrite heartrate.readwrite stepcount.readwrite";

    private static final String KEY_CLIENT_ID = "nocturne_client_id";
    private static final String KEY_ACCESS_TOKEN = "nocturne_access_token";
    private static final String KEY_REFRESH_TOKEN = "nocturne_refresh_token";
    private static final String KEY_TOKEN_EXPIRY = "nocturne_token_expiry";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient = OkHttpWrapper.getClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    // --- Inner classes ---

    public static class DeviceCodeResponse {
        public final String deviceCode;
        public final String userCode;
        public final String verificationUri;
        public final String verificationUriComplete;
        public final int expiresIn;
        public final int interval;

        public DeviceCodeResponse(String deviceCode, String userCode, String verificationUri,
                                  String verificationUriComplete, int expiresIn, int interval) {
            this.deviceCode = deviceCode;
            this.userCode = userCode;
            this.verificationUri = verificationUri;
            this.verificationUriComplete = verificationUriComplete;
            this.expiresIn = expiresIn;
            this.interval = interval;
        }
    }

    public enum TokenPollResult {
        SUCCESS,
        PENDING,
        SLOW_DOWN,
        EXPIRED,
        DENIED
    }

    // --- Public methods ---

    /**
     * Returns the configured Nocturne instance base URL with trailing slash.
     */
    public String getBaseUrl() {
        String url = Pref.getString("nocturne_instance_url", "").trim();
        if (!url.isEmpty() && !url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }

    /**
     * Returns the Origin header value (base URL without trailing slash).
     * The Origin header per RFC 6454 must not include a trailing slash;
     * Cloudflare rejects form POSTs with a malformed Origin as cross-site.
     */
    private String getOrigin() {
        String url = getBaseUrl();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Registers this xDrip+ instance as an OAuth client via Dynamic Client Registration.
     * Idempotent -- returns existing client_id if already registered.
     *
     * @return client_id string or null on failure
     */
    public String registerClient() {
        try {
            final String existing = PersistentStore.getString(KEY_CLIENT_ID);
            if (!existing.isEmpty()) {
                return existing;
            }

            final String baseUrl = getBaseUrl();
            if (baseUrl.isEmpty()) {
                UserError.Log.e(TAG, "registerClient: no instance URL configured");
                return null;
            }

            final JSONObject body = new JSONObject();
            body.put("client_name", "xDrip+");
            body.put("software_id", SOFTWARE_ID);
            body.put("redirect_uris", new org.json.JSONArray()
                    .put("org.nightscoutfoundation.xdrip://oauth/callback"));

            final Request request = new Request.Builder()
                    .url(baseUrl + "api/oauth/register")
                    .header("Origin", getOrigin())
                    .post(RequestBody.create(JSON, body.toString()))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    UserError.Log.e(TAG, "registerClient: HTTP " + response.code());
                    return null;
                }

                final JSONObject result = new JSONObject(response.body().string());
                final String clientId = result.getString("client_id");
                PersistentStore.setString(KEY_CLIENT_ID, clientId);
                UserError.Log.d(TAG, "registerClient: registered client_id=" + clientId);
                return clientId;
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "registerClient failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Starts the Device Authorization flow (RFC 8628).
     * Automatically registers a client if needed.
     *
     * @return DeviceCodeResponse or null on failure
     */
    public DeviceCodeResponse startDeviceFlow() {
        try {
            String clientId = PersistentStore.getString(KEY_CLIENT_ID);
            if (clientId.isEmpty()) {
                clientId = registerClient();
                if (clientId == null) {
                    return null;
                }
            }

            final String baseUrl = getBaseUrl();
            if (baseUrl.isEmpty()) {
                UserError.Log.e(TAG, "startDeviceFlow: no instance URL configured");
                return null;
            }

            final RequestBody body = new FormBody.Builder()
                    .add("client_id", clientId)
                    .add("scope", REQUESTED_SCOPES)
                    .build();

            final Request request = new Request.Builder()
                    .url(baseUrl + "api/oauth/device")
                    .header("Origin", getOrigin())
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    UserError.Log.e(TAG, "startDeviceFlow: HTTP " + response.code());
                    return null;
                }

                final JSONObject result = new JSONObject(response.body().string());
                return new DeviceCodeResponse(
                        result.getString("device_code"),
                        result.getString("user_code"),
                        result.getString("verification_uri"),
                        result.optString("verification_uri_complete", ""),
                        result.getInt("expires_in"),
                        result.optInt("interval", 5)
                );
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "startDeviceFlow failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Polls the token endpoint during device authorization flow.
     *
     * @param deviceCode the device_code from startDeviceFlow()
     * @return TokenPollResult indicating the outcome
     */
    public TokenPollResult pollForToken(final String deviceCode) {
        try {
            final String clientId = PersistentStore.getString(KEY_CLIENT_ID);
            if (clientId.isEmpty()) {
                UserError.Log.e(TAG, "pollForToken: no client_id");
                return TokenPollResult.DENIED;
            }

            final String baseUrl = getBaseUrl();
            if (baseUrl.isEmpty()) {
                UserError.Log.e(TAG, "pollForToken: no instance URL configured");
                return TokenPollResult.DENIED;
            }

            final RequestBody body = new FormBody.Builder()
                    .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    .add("device_code", deviceCode)
                    .add("client_id", clientId)
                    .build();

            final Request request = new Request.Builder()
                    .url(baseUrl + "api/oauth/token")
                    .header("Origin", getOrigin())
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() == null) {
                    UserError.Log.e(TAG, "pollForToken: null response body");
                    return TokenPollResult.DENIED;
                }

                final String responseBody = response.body().string();
                final JSONObject result = new JSONObject(responseBody);

                if (response.isSuccessful()) {
                    storeTokens(
                            result.getString("access_token"),
                            result.getString("refresh_token"),
                            result.getInt("expires_in")
                    );
                    return TokenPollResult.SUCCESS;
                }

                final String error = result.optString("error", "");
                switch (error) {
                    case "authorization_pending":
                        return TokenPollResult.PENDING;
                    case "slow_down":
                        return TokenPollResult.SLOW_DOWN;
                    case "expired_token":
                        return TokenPollResult.EXPIRED;
                    case "access_denied":
                        return TokenPollResult.DENIED;
                    default:
                        UserError.Log.e(TAG, "pollForToken: unexpected error=" + error);
                        return TokenPollResult.DENIED;
                }
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "pollForToken failed: " + e.getMessage());
            return TokenPollResult.DENIED;
        }
    }

    /**
     * Refreshes an expired access token using the stored refresh token.
     *
     * @return true if refresh succeeded, false otherwise
     */
    public boolean refreshAccessToken() {
        try {
            final String clientId = PersistentStore.getString(KEY_CLIENT_ID);
            final String refreshToken = PersistentStore.getString(KEY_REFRESH_TOKEN);
            if (clientId.isEmpty() || refreshToken.isEmpty()) {
                UserError.Log.e(TAG, "refreshAccessToken: missing client_id or refresh_token");
                clearTokens();
                return false;
            }

            final String baseUrl = getBaseUrl();
            if (baseUrl.isEmpty()) {
                UserError.Log.e(TAG, "refreshAccessToken: no instance URL configured");
                return false;
            }

            final RequestBody body = new FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .add("client_id", clientId)
                    .build();

            final Request request = new Request.Builder()
                    .url(baseUrl + "api/oauth/token")
                    .header("Origin", getOrigin())
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    UserError.Log.e(TAG, "refreshAccessToken: HTTP " + response.code());
                    clearTokens();
                    return false;
                }

                final JSONObject result = new JSONObject(response.body().string());
                storeTokens(
                        result.getString("access_token"),
                        result.getString("refresh_token"),
                        result.getInt("expires_in")
                );
                UserError.Log.d(TAG, "refreshAccessToken: success");
                return true;
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "refreshAccessToken failed: " + e.getMessage());
            clearTokens();
            return false;
        }
    }

    /**
     * Revokes the current refresh token and clears all stored credentials.
     */
    public void revokeToken() {
        try {
            final String clientId = PersistentStore.getString(KEY_CLIENT_ID);
            final String refreshToken = PersistentStore.getString(KEY_REFRESH_TOKEN);
            if (clientId.isEmpty() || refreshToken.isEmpty()) {
                return;
            }

            final String baseUrl = getBaseUrl();
            if (baseUrl.isEmpty()) {
                return;
            }

            final RequestBody body = new FormBody.Builder()
                    .add("token", refreshToken)
                    .add("token_type_hint", "refresh_token")
                    .add("client_id", clientId)
                    .build();

            final Request request = new Request.Builder()
                    .url(baseUrl + "api/oauth/revoke")
                    .header("Origin", getOrigin())
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                UserError.Log.d(TAG, "revokeToken: HTTP " + response.code());
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "revokeToken failed: " + e.getMessage());
        } finally {
            clearTokens();
        }
    }

    /**
     * Returns a valid access token, refreshing if necessary.
     *
     * @return access token string or null if unavailable
     */
    public String getValidAccessToken() {
        try {
            final String accessToken = PersistentStore.getString(KEY_ACCESS_TOKEN);
            if (accessToken.isEmpty()) {
                return null;
            }

            final long expiry = PersistentStore.getLong(KEY_TOKEN_EXPIRY);
            final long now = JoH.tsl();

            // Refresh if within 60 seconds of expiry
            if (expiry - now < 60_000) {
                if (!refreshAccessToken()) {
                    return null;
                }
                return PersistentStore.getString(KEY_ACCESS_TOKEN);
            }

            return accessToken;
        } catch (Exception e) {
            UserError.Log.e(TAG, "getValidAccessToken failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks whether the service has stored OAuth credentials.
     *
     * @return true if both access and refresh tokens are present
     */
    public static boolean isConnected() {
        return !PersistentStore.getString(KEY_ACCESS_TOKEN).isEmpty()
                && !PersistentStore.getString(KEY_REFRESH_TOKEN).isEmpty();
    }

    // --- Private helpers ---

    private void storeTokens(final String accessToken, final String refreshToken, final int expiresIn) {
        PersistentStore.setString(KEY_ACCESS_TOKEN, accessToken);
        PersistentStore.setString(KEY_REFRESH_TOKEN, refreshToken);
        PersistentStore.setLong(KEY_TOKEN_EXPIRY, JoH.tsl() + (expiresIn * 1000L));
    }

    private void clearTokens() {
        PersistentStore.setString(KEY_CLIENT_ID, "");
        PersistentStore.setString(KEY_ACCESS_TOKEN, "");
        PersistentStore.setString(KEY_REFRESH_TOKEN, "");
        PersistentStore.setLong(KEY_TOKEN_EXPIRY, 0);
    }
}
