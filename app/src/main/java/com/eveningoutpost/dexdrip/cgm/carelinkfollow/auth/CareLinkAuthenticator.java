package com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CareLinkAuthenticator {

    protected static final String CARELINK_CONNECT_SERVER_EU = "carelink.minimed.eu";
    protected static final String CARELINK_CONNECT_SERVER_US = "carelink.minimed.com";
    protected static final String CARELINK_LANGUAGE_EN = "en";
    protected static final String CARELINK_AUTH_TOKEN_COOKIE_NAME = "auth_tmp_token";
    protected static final String CARELINK_TOKEN_VALIDTO_COOKIE_NAME = "c_token_valid_to";

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


    private final Semaphore available = new Semaphore(0, true);
    private String carelinkCountry;
    private CareLinkCredentialStore credentialStore;


    public CareLinkAuthenticator(String carelinkCountry, CareLinkCredentialStore credentialStore) {
        this.carelinkCountry = carelinkCountry;
        this.credentialStore = credentialStore;
    }

    /*
    public synchronized CareLinkCredential getCreditential() throws InterruptedException {
        if(Looper.myLooper() == Looper.getMainLooper())
            throw new RuntimeException("don't call getAccessToken() from the main thread.");

        switch (credentialStore.getAuthStatus()) {
            case CareLinkCredentialStore.NOT_AUTHENTICATED:
                authenticate();
                available.acquire();
                break;
            case CareLinkCredentialStore.TOKEN_EXPIRED:
                refreshToken();
                available.acquire();
                break;
            case CareLinkCredentialStore.AUTHENTICATED:
                break;
        }

        return credentialStore.getCredential();
    }
     */

    public boolean authenticate(Activity context) throws InterruptedException {
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new RuntimeException("don't call authenticate() from the main thread.");

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                showDialog(context);
            }
        });
        available.acquire();
        return (credentialStore.getAuthStatus() == CareLinkCredentialStore.AUTHENTICATED);
    }

    public boolean refreshToken() {
        //If not authenticated => unable to refresh
        if (credentialStore.getAuthStatus() == CareLinkCredentialStore.NOT_AUTHENTICATED)
            return false;

        HttpUrl url = null;
        OkHttpClient httpClient = null;
        Request.Builder requestBuilder = null;
        Response response = null;
        EditableCookieJar cookieJar = null;


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
                    this.credentialStore.setCredential(
                            this.carelinkCountry,
                            cookieJar.getCookies(CARELINK_AUTH_TOKEN_COOKIE_NAME).get(0).value(),
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

    private void showDialog(Activity context) {

        //Create dialog
        final Dialog authDialog = new Dialog(context);
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

        //Configure Webview
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Mobile Safari/537.36");
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept-Language", "en;q=0.9, *;q=0.8");
        headers.put("Sec-Ch-Ua", "\"Google Chrome\";v=\"117\", \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"117\"");
        webView.loadUrl(this.getLoginUrl(), headers);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (CareLinkAuthenticator.this.extractCookies(url))
                    authDialog.dismiss();
            }

        });


        //Set dialog display infos and show it
        authDialog.setCancelable(true);
        authDialog.getWindow().setLayout(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.MATCH_PARENT);
        authDialog.show();
    }

    protected String getLoginUrl() {

        HttpUrl url = null;

        url = new HttpUrl.Builder()
                .scheme("https")
                .host(this.careLinkServer())
                .addPathSegments("patient/sso/login")
                .addQueryParameter("country", this.carelinkCountry)
                .addQueryParameter("lang", CARELINK_LANGUAGE_EN)
                .build();

        return url.toString();

    }

    protected String careLinkServer() {
        if (this.carelinkCountry.equals("us"))
            return CARELINK_CONNECT_SERVER_US;
        else
            return CARELINK_CONNECT_SERVER_EU;
    }

    protected Boolean extractCookies(String url) {
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
            this.credentialStore.setCredential(this.carelinkCountry, authToken, validToDate, cookieList.toArray(new Cookie[0]));
            //success
            return true;
        } else
            //error
            return false;
    }

    protected void unlock() {
        if (available.availablePermits() <= 0)
            available.release();
    }

    protected Date parseValidTo(String validToDateString) {
        for (SimpleDateFormat zonedFormat : VALIDTO_DATE_FORMATS) {
            try {
                return zonedFormat.parse(validToDateString);
            } catch (Exception ex) {
            }
        }
        return null;
    }

}
