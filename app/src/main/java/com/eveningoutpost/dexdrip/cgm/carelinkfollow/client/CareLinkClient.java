package com.eveningoutpost.dexdrip.cgm.carelinkfollow.client;

import android.util.Log;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.*;

import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.ConnectData;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.ConnectDataResult;
import com.google.gson.*;


/**
 * Medtronic CareLink Follower Client
 *   - API client for communication with CareLink
 */
public class CareLinkClient {
    protected String carelinkUsername;
    protected String carelinkPassword;
    protected String carelinkCountry;
    protected static final String CARELINK_CONNECT_SERVER_EU = "carelink.minimed.eu";
    protected static final String CARELINK_CONNECT_SERVER_US = "carelink.minimed.com";
    protected static final String CARELINK_LANGUAGE_EN = "en";
    protected static final String CARELINK_LOCALE_EN = "en";
    protected static final String CARELINK_AUTH_TOKEN_COOKIE_NAME = "auth_tmp_token";
    protected static final String CARELINK_TOKEN_VALIDTO_COOKIE_NAME = "c_token_valid_to";
    protected static final int AUTH_EXPIRE_DEADLINE_MINUTES = 1;

    protected OkHttpClient httpClient = null;

    private boolean lastLoginSuccess;
    public boolean getLastLoginSuccess(){
        return lastLoginSuccess;
    }
    private boolean lastDataSuccess;
    public boolean getLastDataSuccess(){
        return lastDataSuccess;
    }
    private String lastErrorMessage;
    public String getLastErrorMessage(){
        return lastErrorMessage;
    }
    private String lastStackTraceString;
    public String getLastStackTraceString(){
        return lastStackTraceString;
    }



    protected enum RequestType{
        HtmlGet(),
        HtmlPost(),
        Json()
    }

    public CareLinkClient(String carelinkUsername, String carelinkPassword, String carelinkCountry) {

        this.carelinkUsername = carelinkUsername;
        this.carelinkPassword = carelinkPassword;
        this.carelinkCountry = carelinkCountry;

        lastLoginSuccess = false;
        lastDataSuccess = false;

        //Create main http client with CookieJar
        this.httpClient = new OkHttpClient.Builder()
                .cookieJar(new SimpleOkHttpCookieJar())
                .build();

    }

    protected String careLinkServer() {
        if(this.carelinkCountry.equals("us"))
            return CARELINK_CONNECT_SERVER_US;
        else
            return CARELINK_CONNECT_SERVER_EU;
    }


    //Authentication methods
    protected boolean executeAuthenticationProcedure() {

        Response loginSessionResponse = null;
        Response doLoginResponse = null;
        Response consentResponse = null;


        lastLoginSuccess = false;

        try {
            //TODO handle login errors when occur, check results

            //Clear cookies
            ((SimpleOkHttpCookieJar) this.httpClient.cookieJar()).deleteAllCookies();

            //Open login (get SessionId and SessionData)
            loginSessionResponse = this.getLoginSession();

            //Login
            doLoginResponse = this.doLogin(loginSessionResponse);
            loginSessionResponse.close();

            //Consent
            consentResponse = this.doConsent(doLoginResponse);
            doLoginResponse.close();
            consentResponse.close();

            //Set login success
            lastLoginSuccess = true;

        } catch (Exception e) {
            lastErrorMessage = e.getMessage();
            Log.getStackTraceString(e);
            return lastLoginSuccess;
        }

        return lastLoginSuccess;

    }

    protected Response getLoginSession() throws IOException {

        HttpUrl url = null;
        Request.Builder requestBuilder = null;


        url = new HttpUrl.Builder()
                .scheme("https")
                .host(this.careLinkServer())
                .addPathSegments("patient/sso/login")
                .addQueryParameter("country", this.carelinkCountry)
                .addQueryParameter("lang", CARELINK_LANGUAGE_EN)
                .build();

        requestBuilder = new Request.Builder()
                .url(url);

        this.addHttpHeaders(requestBuilder, RequestType.HtmlGet);

        return this.httpClient.newCall(requestBuilder.build()).execute();

    }

    protected Response doLogin(Response loginSessionResponse) throws IOException {

        HttpUrl url = null;
        Request.Builder requestBuilder = null;
        RequestBody form = null;

        form = new FormBody.Builder()
                .add("sessionID", loginSessionResponse.request().url().queryParameter("sessionID"))
                .add("sessionData", loginSessionResponse.request().url().queryParameter("sessionData"))
                .add("locale", CARELINK_LOCALE_EN)
                .add("action", "login")
                .add("username", this.carelinkUsername)
                .add("password", this.carelinkPassword)
                .add("actionButton", "Log in")
                .build();

        url = new HttpUrl.Builder()
                .scheme("https")
                .host("mdtlogin.medtronic.com")
                .addPathSegments("mmcl/auth/oauth/v2/authorize/login")
                .addQueryParameter("locale", CARELINK_LOCALE_EN)
                .addQueryParameter("country", this.carelinkCountry)
                .build();

        requestBuilder = new Request.Builder()
                .url(url)
                .post(form);

        this.addHttpHeaders(requestBuilder, RequestType.HtmlGet);

        return this.httpClient.newCall(requestBuilder.build()).execute();

    }

    protected Response doConsent(Response doLoginResponse) throws IOException {

        Request.Builder requestBuilder = null;
        RequestBody form = null;
        String doLoginRespBody = null;

        doLoginRespBody = doLoginResponse.body().string();

        //Extract data for consent
        String consentUrl = this.extractResponseData(doLoginRespBody, "(form action=\")(.*)(\" method=\"POST\")", 2);
        String consentSessionData = this.extractResponseData(doLoginRespBody, "(<input type=\"hidden\" name=\"sessionData\" value=\")(.*)(\">)", 2);
        String consentSessionId = this.extractResponseData(doLoginRespBody, "(<input type=\"hidden\" name=\"sessionID\" value=\")(.*)(\">)", 2);

        //Send consent
        form = new FormBody.Builder()
                .add("action", "consent")
                .add("sessionID", consentSessionId)
                .add("sessionData", consentSessionData)
                .add("response_type", "code")
                .add("response_mode", "query")
                .build();

        requestBuilder = new Request.Builder()
                .url(consentUrl)
                .post(form);

        this.addHttpHeaders(requestBuilder, RequestType.HtmlPost);

        return this.httpClient.newCall(requestBuilder.build()).execute();

    }

    protected String extractResponseData(String respBody, String groupRegex, int groupIndex) {

        String responseData = null;
        Matcher responseDataMatcher = null;

        responseDataMatcher = Pattern.compile(groupRegex).matcher(respBody);
        if(responseDataMatcher.find()) {
            responseData = responseDataMatcher.group(groupIndex);
        }

        return responseData;

    }

    protected String getAuthorizationToken() {



        if(!this.executeAuthenticationProcedure()){
            return null;
        };

        //there can be only one
        return "Bearer" + " " + ((SimpleOkHttpCookieJar) httpClient.cookieJar()).getCookies(CARELINK_AUTH_TOKEN_COOKIE_NAME).get(0).value();


    }


    //CareLink data APIs
    public ConnectDataResult getLast24Hours() {

        HttpUrl url = null;
        Request.Builder requestBuilder = null;
        String authToken = null;
        String currentTime = null;
        String connectDataString = null;
        ConnectDataResult connectDataResult = null;
        Response response = null;


        lastDataSuccess = false;
        connectDataResult = new ConnectDataResult();

        //Get auth token
        authToken= this.getAuthorizationToken();
        if(authToken != null){

            // TODO current time ?
            currentTime = String.valueOf(System.currentTimeMillis());

            //Get connect data of last 24 hours
            url = new HttpUrl.Builder()
                    .scheme("https")
                    .host(this.careLinkServer())
                    .addPathSegments("patient/connect/data")
                    .addQueryParameter("cpSerialNumber", "NONE")
                    .addQueryParameter("msgType", "last24hours")
                    .addQueryParameter("requestTime", currentTime)
                    .build();

            requestBuilder = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", authToken);

            this.addHttpHeaders(requestBuilder, RequestType.Json);

            //Get and convert data
            try {
                response = this.httpClient.newCall(requestBuilder.build()).execute();
                connectDataResult.responseCode = response.code();
                if(response.isSuccessful()) {
                    connectDataString = response.body().string();
                    connectDataResult.connectData = (new GsonBuilder().create()).fromJson(connectDataString, ConnectData.class);
                    //only if there is actual data
                    if (connectDataResult.connectData != null) lastDataSuccess = true;
                }
                response.close();
            } catch (Exception e) {
                lastErrorMessage = e.getMessage();
                lastStackTraceString = Log.getStackTraceString(e);
            }

        }

        //set data request success
        connectDataResult.success = lastDataSuccess;

        return connectDataResult;

    }

    protected void addHttpHeaders(Request.Builder requestBuilder, RequestType type) {

        requestBuilder
                .addHeader("Accept-Language", "en;q=0.9, *;q=0.8")
                .addHeader("Connection", "keep-alive")
                .addHeader("sec-ch-ua", "\"Google Chrome\";v=\"87\", \" Not;A Brand\";v=\"99\", \"Chromium\";v=\"87\"")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");

        switch(type) {
            case Json:
                requestBuilder.addHeader("Accept", "application/json, text/plain, */*");
                requestBuilder.addHeader("Content-Type", "application/json; charset=utf-8");
                break;
            case HtmlGet:
                requestBuilder.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            case HtmlPost:
                requestBuilder.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
                requestBuilder.addHeader("Content-Type", "application/x-www-form-urlencoded");
                break;
        }

    }

}
