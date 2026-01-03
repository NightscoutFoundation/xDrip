package com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.UrlQuerySanitizer;
import android.os.Handler;
import android.os.Looper;

import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.util.Base64;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.widget.LinearLayoutCompat;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import javax.security.auth.x500.X500Principal;

import okhttp3.ConnectionPool;
import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CareLinkAuthenticator {

    private static final String TAG = "CareLinkAuthenticator";

    protected static final String CAREPARTNER_APP_DISCO_URL = "https://clcloud.minimed.eu/connect/carepartner/v13/discover/android/3.6";
    protected static final String CARELINK_CONNECT_SERVER_EU = "carelink.minimed.eu";
    protected static final String CARELINK_CONNECT_SERVER_US = "carelink.minimed.com";
    protected static final String CARELINK_LANGUAGE_EN = "en";
    protected static final String CARELINK_AUTH_TOKEN_COOKIE_NAME = "auth_tmp_token";
    protected static final String CARELINK_TOKEN_VALIDTO_COOKIE_NAME = "c_token_valid_to";

    protected static final String[] ANDROID_MODELS = {
            "SM-G973F",
            "SM-G988U1",
            "SM-G981W",
            "SM-G9600"
    };

    protected static final SimpleDateFormat[] VALIDTO_DATE_FORMATS = {
            new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy", Locale.ENGLISH),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.ENGLISH),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH),
            //new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.ENGLISH),
            //new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ENGLISH),
            new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            //new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX"),
            //new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
    };

    private static final int PKCE_BASE64_ENCODE_SETTINGS =
            Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE;


    private final Semaphore available = new Semaphore(0, true);
    private AlertDialog progressDialog;
    private String carelinkCountry;
    private CareLinkCredentialStore credentialStore;
    private CarePartnerAppConfig carepartnerAppConfig;
    private String authCode = null;
    private OkHttpClient httpClient = null;
    private boolean authWebViewCancelled = false;
    private boolean carelinkCommunicationError = false;


    class RefreshTokenExpiredException extends Exception {
        public RefreshTokenExpiredException(String message) {
            super(message);
        }
    }


    public CareLinkAuthenticator(String carelinkCountry, CareLinkCredentialStore credentialStore) {
        this.carelinkCountry = carelinkCountry;
        this.credentialStore = credentialStore;
    }

    public boolean authenticate(Activity context, CareLinkAuthType authType) throws InterruptedException {

        if (Looper.myLooper() == Looper.getMainLooper())
            throw new RuntimeException("don't call authenticate() from the main thread.");

        //Execute authentication method of authentication type
        switch (authType) {
            case Browser:
                this.authenticateAsBrowser(context);
                break;
            case MobileApp:
                this.authenticateAsCpApp(context);
                break;
        }

        //Return: is authenticated
        return (credentialStore.getAuthStatus() == CareLinkCredentialStore.AUTHENTICATED);
    }

    public boolean refreshToken() {
        //Have credential, authType is known, already authenticated
        if (credentialStore.getCredential() != null && credentialStore.getCredential().authType != null && credentialStore.getAuthStatus() != CareLinkCredentialStore.NOT_AUTHENTICATED) {
            switch (credentialStore.getCredential().authType) {
                case Browser:
                    return this.refreshBrowserToken();
                case MobileApp:
                    return this.refreshCpAppToken();
                default:
                    return false;
            }
        } else {
            return false;
        }
    }


    private void authenticateAsCpApp(Activity context) {
        String androidModel;
        String clientId;
        String authUrl;

        try {

            carelinkCommunicationError = false;

            //Show progress dialog while preparing for login page
            this.showProgressDialog(context);

            //Generate ID, models
            androidModel = this.generateAndroidModel();

            //Load application config
            this.loadAppConfig();

            //Create client credential
            clientId = carepartnerAppConfig.getOAuthClientId();

            //Prepare authentication
            UserError.Log.d(TAG, "Prepare authentication");
            authUrl = createAuthUrl();

            //Hide progress dialog
            this.hideProgressDialog();

            //Authenticate in browser
            UserError.Log.d(TAG, "Start browser login");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    CareLinkAuthenticator.this.showCpAppAuthPage(context, authUrl);
                }
            });
            available.acquire();

            //Continue if not cancelled and no error
            if (!this.authWebViewCancelled && !carelinkCommunicationError) {
                //Show progress dialog while completing authentication
                this.showProgressDialog(context);

                //Get access token
                UserError.Log.d(TAG, "Get access token");
                JsonObject tokenObject = this.getAccessToken(clientId, null, authCode);

                //Store credentials
                UserError.Log.d(TAG, "Store credentials");
                this.credentialStore.setMobileAppCredential(this.carelinkCountry,
                        androidModel, clientId, null,
                        tokenObject.get("access_token").getAsString(), tokenObject.get("refresh_token").getAsString(),
                        new Date(Calendar.getInstance().getTime().getTime() + (tokenObject.get("expires_in").getAsInt() * 1000)),
                        null
                );

                //Hide progress dialog
                this.hideProgressDialog();
            }

        } catch (Exception ex) {
            UserError.Log.e(TAG, "Error authenticating as CpApp. Details: \r\n " + ex.getMessage());
            carelinkCommunicationError = true;
            this.hideProgressDialog();
        }

        //Show communication error
        if (carelinkCommunicationError) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(context)
                            .setTitle("Communication error!")
                            .setMessage("Error communicating with CareLink Server! Please try again later!")
                            .setCancelable(true)
                            .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .show();
                }
            });
        }
    }

    private String createAuthUrl() {
        return new HttpUrl.Builder()
            .scheme("https")
            .host(carepartnerAppConfig.getSSOServerHost())
            .port(carepartnerAppConfig.getSSOServerPort())
            .addPathSegments(carepartnerAppConfig.getSSOServerPrefix())
            .addPathSegments(carepartnerAppConfig.getOAuthAuthEndpoint().replaceFirst("^/", ""))
            .addQueryParameter("client_id", carepartnerAppConfig.getOAuthClientId())
            .addQueryParameter("response_type", "code")
            .addQueryParameter("scope", carepartnerAppConfig.getOAuthScope())
            .addQueryParameter("redirect_uri", carepartnerAppConfig.getOAuthRedirectUri())
            .addQueryParameter("audience", carepartnerAppConfig.getOAuthAudience())
            .build()
            .toString();
    }

    private void authenticateAsBrowser(Activity context) throws InterruptedException {

        //Authenticate in browser
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                CareLinkAuthenticator.this.showBrowserAuthPage(context, "");
            }
        });
        available.acquire();
    }

    private OkHttpClient getHttpClient() {
        if (this.httpClient == null)
            this.httpClient = new OkHttpClient();
        return this.httpClient;
    }

    private boolean loadAppConfig() throws IOException {
        if (carepartnerAppConfig == null) {
            carepartnerAppConfig = new CarePartnerAppConfig();
            UserError.Log.d(TAG, "Get region config");
            carepartnerAppConfig.regionConfig = this.getCpAppRegionConfig();
            UserError.Log.d(TAG, "Get SSO config");
            carepartnerAppConfig.ssoConfig = this.getCpAppSSOConfig(carepartnerAppConfig.getSSOConfigUrl());
        }
        return true;
    }

    private JsonObject getAccessToken(String clientId, String clientSecret, String idToken) throws IOException {
        return this.getToken(clientId, clientSecret, idToken, null);
    }

    private JsonObject refreshToken(String clientId, String clientSecret, String refreshToken) throws IOException {
        return this.getToken(clientId, clientSecret, null, refreshToken);
    }

    private JsonObject getToken(String clientId, String clientSecret, String idToken,  String refreshToken) throws IOException {

        Request.Builder requestBuilder;
        FormBody.Builder form;

        //Common token request params
        form = new FormBody.Builder()
                .add("client_id", clientId);
        if (clientSecret != null) {
                form.add("client_secret", clientSecret);
        }

        //Authentication token request params
        if (idToken != null) {
            form.add("code", idToken)
                    .add("grant_type", "authorization_code")
                    .add("redirect_uri", this.carepartnerAppConfig.getOAuthRedirectUri());
            //Refresh token request params
        } else {
            form.add("refresh_token", refreshToken)
                    .add("grant_type", "refresh_token");
        }

        requestBuilder = new Request.Builder()
                .post(form.build())
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        try {
            return this.callSsoRestApi(requestBuilder, carepartnerAppConfig.getOAuthTokenEndpoint(), null);
        } catch (RefreshTokenExpiredException ex) {
            UserError.Log.e(TAG, "Refresh token expired. Details: \r\n " + ex.getMessage());
            this.credentialStore.informExpiredRefreshToken();
            throw new IOException("Refresh token expired");
        }

    }

    private String generateAndroidModel() {
        return ANDROID_MODELS[new Random().nextInt(ANDROID_MODELS.length)];
    }


    private JsonObject callSsoRestApi(Request.Builder requestBuilder, String endpoint, Map<String, String> queryParams) throws IOException, RefreshTokenExpiredException {

        Response response = this.callSsoApi(requestBuilder, endpoint, queryParams);
        if (response.isSuccessful()) {
            return JsonParser.parseString(response.body().string()).getAsJsonObject();
        } else if(response.code() == 403) {
            // see https://auth0.com/docs/api/authentication/refresh-token/refresh-token#response-messages
            // 403 Forbidden. The refresh token is invalid or has expired.
            throw new RefreshTokenExpiredException("The Refresh token has expired");
        } else {
            return null;
        }

    }

    private Response callSsoApi(Request.Builder requestBuilder, String endpoint, Map<String, String> queryParams) throws IOException {

        HttpUrl.Builder url = null;

        //Build URL
        url = new HttpUrl.Builder()
                .scheme("https")
                .host(carepartnerAppConfig.getSSOServerHost())
                .addPathSegments(carepartnerAppConfig.getSSOServerPrefix())
                .addPathSegments(endpoint);
        //Add query params
        if (queryParams != null) {
            for (Map.Entry<String, String> param : queryParams.entrySet()) {
                url.addQueryParameter(param.getKey(), param.getValue());
            }
        }
        requestBuilder.url(url.build());
        //Send request
        return this.getHttpClient().newCall(requestBuilder.build()).execute();

    }

    private boolean refreshCpAppToken() {
        JsonObject tokenRefreshResult;

        try {
            //Get config
            this.loadAppConfig();
            //Refresh token
            tokenRefreshResult = this.refreshToken(
                    credentialStore.getCredential().clientId, credentialStore.getCredential().clientSecret, credentialStore.getCredential().refreshToken);
            //Save token
            credentialStore.updateMobileAppCredential(
                    tokenRefreshResult.get("access_token").getAsString(),
                    //new Date(Calendar.getInstance().getTime().getTime() + 15 * 60000),
                    //new Date(Calendar.getInstance().getTime().getTime() + 30 * 60000),
                    new Date(Calendar.getInstance().getTime().getTime() + (tokenRefreshResult.get("expires_in").getAsInt() * 1000)),
                    null,
                    tokenRefreshResult.get("refresh_token").getAsString());
            //Completed successfully
            return true;
        } catch (Exception ex) {
            UserError.Log.e(TAG, "Error refreshing CpApp token! Details: \r\n" + ex.getMessage());
            return false;
        }
    }

    private boolean refreshBrowserToken() {

        //If not authenticated => unable to refresh
        if (credentialStore.getAuthStatus() == CareLinkCredentialStore.NOT_AUTHENTICATED)
            return false;

        HttpUrl url;
        OkHttpClient httpClient;
        Request.Builder requestBuilder;
        Response response;
        EditableCookieJar cookieJar;


        //Build client with cookies from CredentialStore
        cookieJar = new EditableCookieJar();
        httpClient = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build();
        cookieJar.AddCookies(credentialStore.getCredential().cookies);

        //Build request
        url = new HttpUrl.Builder()
                .scheme("https")
                .host(this.careLinkServer())
                .addPathSegments("patient/sso/reauth")
                .build();
        requestBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(null, new byte[0]))
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Accept-Language", "en;q=0.9, *;q=0.8")
                .addHeader("Connection", "keep-alive")
                .addHeader("Authorization", credentialStore.getCredential().getAuthorizationFieldValue())
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Mobile Safari/537.36")
                .addHeader("Sec-Ch-Ua", "\"Google Chrome\";v=\"117\", \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"117\"");

        //Send request to refresh token
        try {
            response = httpClient.newCall(requestBuilder.build()).execute();
            //successful response
            if (response.isSuccessful()) {
                //New authentication cookies found
                if (cookieJar.contains(CARELINK_AUTH_TOKEN_COOKIE_NAME) && cookieJar.contains(CARELINK_TOKEN_VALIDTO_COOKIE_NAME)) {
                    //Update credentials
                    this.credentialStore.updateBrowserCredential(
                            cookieJar.getCookies(CARELINK_AUTH_TOKEN_COOKIE_NAME).get(0).value(),
                            this.parseValidTo(cookieJar.getCookies(CARELINK_TOKEN_VALIDTO_COOKIE_NAME).get(0).value()),
                            this.parseValidTo(cookieJar.getCookies(CARELINK_TOKEN_VALIDTO_COOKIE_NAME).get(0).value()),
                            cookieJar.getAllCookies().toArray(new Cookie[0]));
                } else {
                    return false;
                }
            }
            //error in response
            else {
                return false;
            }
            response.close();
        } catch (IOException e) {
            return false;
        } finally {

        }

        return (credentialStore.getAuthStatus() == CareLinkCredentialStore.AUTHENTICATED);
    }

    private void showProgressDialog(Activity context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                CareLinkAuthenticator.this.getProgressDialog(context).show();
            }
        });
    }

    private void hideProgressDialog() {
        if (this.progressDialog != null && this.progressDialog.isShowing()) {
            this.progressDialog.dismiss();
        }
    }

    private AlertDialog getProgressDialog(Activity context) {
        if (this.progressDialog == null) {
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(context);
            builder.setTitle(xdrip.gs(R.string.carelink_auth_login_in_progress));
            final ProgressBar progressBar = new ProgressBar(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            progressBar.setLayoutParams(lp);
            builder.setView(progressBar);
            this.progressDialog = builder.create();
        }
        //return builder;
        return this.progressDialog;
    }

    private void showBrowserAuthPage(Activity context, String url) {
        final Dialog authDialog = new Dialog(context);
        this.showAuthWebView(authDialog, url, new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (CareLinkAuthenticator.this.extractBrowserLoginCookies(url))
                    authDialog.dismiss();
            }
        });
    }

    private void showCpAppAuthPage(Activity context, String url) {

        //CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        //customTabsIntent.launchUrl(
        //        context, Uri.parse(url));

        final Dialog authDialog = new Dialog(context);
        this.showAuthWebView(authDialog, url, new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (CareLinkAuthenticator.this.extractCpAppAuthCode(url))
                    //close browser dialog
                    authDialog.dismiss();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                //Connection error
                if (error.getErrorCode() == WebViewClient.ERROR_CONNECT) {
                    carelinkCommunicationError = true;
                    authDialog.dismiss();
                }
            }
        });
    }

    private void showAuthWebView(Dialog authDialog, String url, WebViewClient webViewClient) {

        this.authWebViewCancelled = false;

        LinearLayoutCompat layout = new LinearLayoutCompat(authDialog.getContext());
        WebView webView = new WebView(authDialog.getContext());
        webView.setLayoutParams(new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.addView(webView);
        authDialog.setContentView(layout);

        authDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                unlock();
            }
        });
        authDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                authWebViewCancelled = true;
            }
        });

        //Configure Webview
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Mobile Safari/537.36");
        webView.loadUrl(url);
        webView.setWebViewClient(webViewClient);

        //Set dialog display params and show it
        authDialog.setCancelable(true);
        authDialog.getWindow().setLayout(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.MATCH_PARENT);
        authDialog.show();

    }

    private boolean extractCpAppAuthCode(String url) {

        //Redirect url => extract code, completed
        if (url.contains(this.carepartnerAppConfig.getOAuthRedirectUri())) {
            try {
                UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
                sanitizer.setAllowUnregisteredParamaters(true);
                sanitizer.parseUrl(url);
                authCode = sanitizer.getValue("code");
            } catch (Exception ex) {
                UserError.Log.e(TAG, "Error extracting authCode! Details: \r\n" + ex.getMessage());
            }
            return true;
            //Other url => authentication not completed yet
        } else
            return false;

    }

    private JsonObject getCpAppRegionConfig() throws IOException {


        //Get CarePartner app discover
        JsonObject endpointConfig = this.getConfigJson(CAREPARTNER_APP_DISCO_URL);
        //Extract region config of selected country
        JsonArray countries = endpointConfig.getAsJsonArray("supportedCountries");
        JsonArray regions = endpointConfig.getAsJsonArray("CP");
        for (JsonElement country : countries) {
            if (country.getAsJsonObject().has(this.carelinkCountry.toUpperCase(Locale.ROOT))) {
                String regionCode = country.getAsJsonObject().get(this.carelinkCountry.toUpperCase(Locale.ROOT)).getAsJsonObject().get("region").getAsString();
                for (JsonElement region : regions) {
                    if (region.getAsJsonObject().get("region").getAsString().contentEquals(regionCode)) {
                        return region.getAsJsonObject();
                    }
                }
            }
        }

        return null;

    }

    private JsonObject getCpAppSSOConfig(String url) throws IOException {
        return this.getConfigJson(url);
    }

    private JsonObject getConfigJson(String url) throws IOException {

        Request request;

        request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Response response = this.getHttpClient().newCall(request).execute();
        return JsonParser.parseString(response.body().string()).getAsJsonObject();

    }

    private String careLinkServer() {
        if (this.carelinkCountry.equals("us"))
            return CARELINK_CONNECT_SERVER_US;
        else
            return CARELINK_CONNECT_SERVER_EU;
    }

    private Boolean extractBrowserLoginCookies(String url) {

        String cookies = null;
        String authToken = null;
        String host = null;
        Date validToDate = null;
        ArrayList<Cookie> cookieList;


        cookies = CookieManager.getInstance().getCookie(url);

        //Authentication cookies are present
        if (cookies != null && cookies.contains(CARELINK_AUTH_TOKEN_COOKIE_NAME) && cookies.contains(CARELINK_TOKEN_VALIDTO_COOKIE_NAME)) {

            //Build cookies
            host = HttpUrl.parse(url).host();
            cookieList = new ArrayList<Cookie>();

            for (String cookie : cookies.split("; ")) {

                String[] cookieParts = cookie.split("=");

                Cookie.Builder cookieBuilder = new Cookie.Builder()
                        .name(cookieParts[0])
                        .value(cookieParts[1])
                        .path("/")
                        .domain(host);

                if (cookieParts[0].contains(CARELINK_AUTH_TOKEN_COOKIE_NAME)) {
                    cookieBuilder.secure();
                    authToken = cookieParts[1];
                }

                if (cookieParts[0].contains(CARELINK_TOKEN_VALIDTO_COOKIE_NAME)) {
                    validToDate = this.parseValidTo(cookieParts[1]);
                    cookieBuilder.secure();
                }

                cookieList.add(cookieBuilder.build());

            }

            //Skip cookies if authentication already expired (existing old cookies found)
            if (validToDate.getTime() < System.currentTimeMillis())
                return false;

            //Update credentials
            this.credentialStore.setBrowserCredential(this.carelinkCountry, authToken, validToDate, validToDate, cookieList.toArray(new Cookie[0]));
            //success
            return true;
        } else
            //error
            return false;
    }

    private void unlock() {
        if (available.availablePermits() <= 0)
            available.release();
    }

    private Date parseValidTo(String validToDateString) {
        for (SimpleDateFormat zonedFormat : VALIDTO_DATE_FORMATS) {
            //try until translate is successful
            try {
                return zonedFormat.parse(validToDateString);
            } catch (Exception ex) {
            }
        }
        return null;
    }

}
