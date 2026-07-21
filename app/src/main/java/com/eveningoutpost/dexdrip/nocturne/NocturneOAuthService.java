package com.eveningoutpost.dexdrip.nocturne;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.json.JSONObject;
import org.nightscoutfoundation.nocturne.ApiClient;
import org.nightscoutfoundation.nocturne.ApiException;
import org.nightscoutfoundation.nocturne.api.OAuthApi;
import org.nightscoutfoundation.nocturne.model.ClientRegistrationRequest;
import org.nightscoutfoundation.nocturne.model.ClientRegistrationResponse;
import org.nightscoutfoundation.nocturne.model.OAuthDeviceAuthorizationResponse;
import org.nightscoutfoundation.nocturne.model.OAuthTokenResponse;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * OAuth service implementing Dynamic Client Registration (RFC 7591)
 * and Device Authorization Grant (RFC 8628) for Nocturne, using the
 * nocturne-java SDK's generated OAuthApi for transport.
 * <p>
 * Orchestration stays here: token persistence, refresh policy, and the
 * distinction between real OAuth rejections and proxy/CDN errors.
 */
public class NocturneOAuthService {

    private static final String TAG = "NocturneOAuth";
    private static final String SOFTWARE_ID = "org.nightscoutfoundation.xdrip";
    private static final String REQUESTED_SCOPES = "glucose.readwrite heartrate.readwrite stepcount.readwrite treatments.readwrite devices.readwrite";
    private static final String GRANT_DEVICE_CODE = "urn:ietf:params:oauth:grant-type:device_code";

    private static final String KEY_CLIENT_ID = "nocturne_client_id";
    private static final String KEY_ACCESS_TOKEN = "nocturne_access_token";
    private static final String KEY_REFRESH_TOKEN = "nocturne_refresh_token";
    private static final String KEY_TOKEN_EXPIRY = "nocturne_token_expiry";

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
     * Upgrades http:// to https:// for non-local hosts since Cloudflare-fronted
     * servers reject form POSTs whose Origin scheme doesn't match (403).
     */
    public String getBaseUrl() {
        String url = Pref.getString("nocturne_instance_url", "").trim();
        if (!url.isEmpty() && url.startsWith("http://")) {
            final String host = url.substring(7).split("[:/]")[0];
            if (!isLocalHost(host)) {
                url = "https://" + url.substring(7);
            }
        }
        if (!url.isEmpty() && !url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }

    private static boolean isLocalHost(final String host) {
        return host.equals("localhost")
                || host.equals("127.0.0.1")
                || host.startsWith("192.168.")
                || host.startsWith("10.")
                || host.endsWith(".local");
    }

    /**
     * Builds an SDK client for the OAuth endpoints (no bearer token — these
     * are anonymous). The Origin header per RFC 6454 must not include a
     * trailing slash; Cloudflare rejects form POSTs with a malformed Origin
     * as cross-site.
     */
    private OAuthApi buildApi() {
        final String baseUrl = getBaseUrl();
        if (baseUrl.isEmpty()) {
            return null;
        }
        final String basePath = baseUrl.substring(0, baseUrl.length() - 1);
        final ApiClient client = new ApiClient(OkHttpWrapper.getClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build())
                .setBasePath(basePath)
                .addDefaultHeader("Origin", basePath);
        return new OAuthApi(client);
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

            final OAuthApi api = buildApi();
            if (api == null) {
                UserError.Log.e(TAG, "registerClient: no instance URL configured");
                return null;
            }

            final ClientRegistrationResponse response = api.oAuthRegister(new ClientRegistrationRequest()
                    .clientName("xDrip+")
                    .softwareId(SOFTWARE_ID)
                    .redirectUris(Collections.singletonList("org.nightscoutfoundation.xdrip://oauth/callback")));

            final String clientId = response.getClientId();
            if (clientId == null || clientId.isEmpty()) {
                UserError.Log.e(TAG, "registerClient: response missing client_id");
                return null;
            }
            PersistentStore.setString(KEY_CLIENT_ID, clientId);
            UserError.Log.d(TAG, "registerClient: registered client_id=" + clientId);
            return clientId;
        } catch (ApiException e) {
            UserError.Log.e(TAG, "registerClient: HTTP " + e.getCode() + " body=" + e.getResponseBody());
            return null;
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

            final OAuthApi api = buildApi();
            if (api == null) {
                UserError.Log.e(TAG, "startDeviceFlow: no instance URL configured");
                return null;
            }

            final OAuthDeviceAuthorizationResponse response = api.oAuthDeviceAuthorization(clientId, REQUESTED_SCOPES);
            return new DeviceCodeResponse(
                    response.getDeviceCode(),
                    response.getUserCode(),
                    response.getVerificationUri(),
                    response.getVerificationUriComplete() != null ? response.getVerificationUriComplete() : "",
                    response.getExpiresIn() != null ? response.getExpiresIn() : 0,
                    response.getInterval() != null ? response.getInterval() : 5
            );
        } catch (ApiException e) {
            UserError.Log.e(TAG, "startDeviceFlow: HTTP " + e.getCode() + " body=" + e.getResponseBody());
            return null;
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

            final OAuthApi api = buildApi();
            if (api == null) {
                UserError.Log.e(TAG, "pollForToken: no instance URL configured");
                return TokenPollResult.DENIED;
            }

            final OAuthTokenResponse response = api.oAuthToken(
                    GRANT_DEVICE_CODE, null, null, clientId, null, null, deviceCode, null);
            storeTokens(response);
            return TokenPollResult.SUCCESS;
        } catch (ApiException e) {
            // RFC 8628: pending/slow_down/expired arrive as HTTP 400 with an
            // OAuth error code in the JSON body — normal protocol flow.
            final String error = oauthErrorCode(e.getResponseBody());
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
                    UserError.Log.e(TAG, "pollForToken: HTTP " + e.getCode() + " error=" + error);
                    return TokenPollResult.DENIED;
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

            final OAuthApi api = buildApi();
            if (api == null) {
                UserError.Log.e(TAG, "refreshAccessToken: no instance URL configured");
                return false;
            }

            final OAuthTokenResponse response = api.oAuthToken(
                    "refresh_token", null, null, clientId, null, refreshToken, null, null);
            storeTokens(response);
            UserError.Log.d(TAG, "refreshAccessToken: success");
            return true;
        } catch (ApiException e) {
            UserError.Log.e(TAG, "refreshAccessToken: HTTP " + e.getCode());
            // Only clear tokens on definitive OAuth rejection — the response
            // must be a JSON object with an "error" field (e.g. invalid_grant).
            // Cloudflare/proxy 403s return plain text and must not nuke credentials.
            if (e.getCode() >= 400 && e.getCode() < 500 && isOAuthErrorResponse(e.getResponseBody())) {
                clearTokens();
            }
            return false;
        } catch (Exception e) {
            // Network errors are transient — don't clear tokens
            UserError.Log.e(TAG, "refreshAccessToken failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Revokes the current refresh token and clears all stored credentials.
     */
    public void revokeToken() {
        try {
            final String refreshToken = PersistentStore.getString(KEY_REFRESH_TOKEN);
            if (refreshToken.isEmpty()) {
                return;
            }
            final OAuthApi api = buildApi();
            if (api == null) {
                return;
            }
            api.oAuthRevoke(refreshToken, "refresh_token");
            UserError.Log.d(TAG, "revokeToken: revoked");
        } catch (ApiException e) {
            UserError.Log.e(TAG, "revokeToken: HTTP " + e.getCode());
        } catch (Exception e) {
            UserError.Log.e(TAG, "revokeToken failed: " + e.getMessage());
        } finally {
            clearAll();
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
            final long expiry = PersistentStore.getLong(KEY_TOKEN_EXPIRY);
            final long now = JoH.tsl();

            // If we have a valid, non-expired token, return it
            if (!accessToken.isEmpty() && expiry - now >= 60_000) {
                return accessToken;
            }

            // Token missing or near expiry — attempt refresh if we have a refresh token
            final String refreshToken = PersistentStore.getString(KEY_REFRESH_TOKEN);
            if (refreshToken.isEmpty()) {
                return null;
            }

            if (!refreshAccessToken()) {
                // Refresh failed but tokens may still be valid for transient failures
                final String current = PersistentStore.getString(KEY_ACCESS_TOKEN);
                return current.isEmpty() ? null : current;
            }
            return PersistentStore.getString(KEY_ACCESS_TOKEN);
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

    private void storeTokens(final OAuthTokenResponse response) {
        PersistentStore.setString(KEY_ACCESS_TOKEN, response.getAccessToken());
        PersistentStore.setString(KEY_REFRESH_TOKEN, response.getRefreshToken());
        final int expiresIn = response.getExpiresIn() != null ? response.getExpiresIn() : 0;
        PersistentStore.setLong(KEY_TOKEN_EXPIRY, JoH.tsl() + (expiresIn * 1000L));
    }

    private void clearTokens() {
        // Preserve client_id — the client registration is reusable and
        // should only be cleared on explicit disconnect/re-registration.
        PersistentStore.setString(KEY_ACCESS_TOKEN, "");
        PersistentStore.setString(KEY_REFRESH_TOKEN, "");
        PersistentStore.setLong(KEY_TOKEN_EXPIRY, 0);
    }

    /**
     * Clears all stored credentials including client registration.
     * Only call on explicit disconnect or re-registration.
     */
    private void clearAll() {
        PersistentStore.setString(KEY_CLIENT_ID, "");
        clearTokens();
    }

    /**
     * Extracts the OAuth "error" code from an error response body, or ""
     * when the body is not an OAuth error (e.g. a proxy/CDN error page).
     */
    private static String oauthErrorCode(final String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        try {
            return new JSONObject(body).optString("error", "");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Checks whether a response body is a valid OAuth error (JSON with "error" field).
     * Distinguishes real OAuth rejections (e.g. invalid_grant) from proxy/CDN errors
     * like Cloudflare's plain-text "Cross-site POST form submissions are forbidden".
     */
    private static boolean isOAuthErrorResponse(final String body) {
        return !oauthErrorCode(body).isEmpty();
    }
}
