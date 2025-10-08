package com.eveningoutpost.dexdrip.cgm.carelinkfollow.client;

import com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth.CareLinkAuthType;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth.CareLinkAuthentication;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth.CareLinkCredentialStore;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth.EditableCookieJar;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.ActiveNotification;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.ClearedNotification;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.CountrySettings;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.DataUpload;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.DisplayMessage;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.Marker;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.MonitorData;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.Profile;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.Patient;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.M2MEnabled;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.RecentData;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.RecentUploads;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.SensorGlucose;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.User;
import com.eveningoutpost.dexdrip.models.JoH;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * CareLink Follower Client
 * - API client for communication with CareLink
 */
public class CareLinkClient {
    protected String carelinkUsername;
    protected String carelinkPassword;
    protected String carelinkCountry;
    protected static final String CARELINK_CONNECT_SERVER_EU = "carelink.minimed.eu";
    protected static final String CARELINK_CONNECT_SERVER_US = "carelink.minimed.com";
    protected static final String CARELINK_CLOUD_SERVER_EU = "clcloud.minimed.eu";
    protected static final String CARELINK_CLOUD_SERVER_US = "clcloud.minimed.com";
    protected static final String API_PATH_DISPLAY_MESSAGE = "connect/carepartner/v11/display/message";
    protected static final String CARELINK_LANGUAGE_EN = "en";
    protected static final String CARELINK_AUTH_TOKEN_COOKIE_NAME = "auth_tmp_token";
    protected static final String CARELINK_TOKEN_VALIDTO_COOKIE_NAME = "c_token_valid_to";
    protected static final int AUTH_EXPIRE_DEADLINE_MINUTES = 1;

    protected static final SimpleDateFormat[] ZONED_DATE_FORMATS = {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
    };

    //Communication info
    protected OkHttpClient httpClient = null;
    protected boolean loginInProcess = false;
    protected boolean collectingSessionInfos = false;
    protected int lastResponseCode;

    public int getLastResponseCode() {
        return lastResponseCode;
    }

    private boolean lastLoginSuccess;

    public boolean getLastLoginSuccess() {
        return lastLoginSuccess;
    }

    private boolean lastDataSuccess;

    public boolean getLastDataSuccess() {
        return lastDataSuccess;
    }

    private String lastErrorMessage;

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    private String lastStackTraceString;

    public String getLastStackTraceString() {
        return lastStackTraceString;
    }

    //Credentials
    protected CareLinkCredentialStore credentialStore;

    //Session info
    protected  boolean sessionInfosLoaded = false;
    protected User sessionUser;

    public User getSessionUser() {
        return sessionUser;
    }

    protected Profile sessionProfile;

    public Profile getSessionProfile() {
        return sessionProfile;
    }

    protected CountrySettings sessionCountrySettings;

    public CountrySettings getSessionCountrySettings() {
        return sessionCountrySettings;
    }
    protected RecentUploads sessionRecentUploads;
    public RecentUploads getSessionRecentUploads() {
        return sessionRecentUploads;
    }
    protected Boolean sessionDeviceIsBle;
    public Boolean getSessionDeviceIsBle() {
        return sessionDeviceIsBle;
    }
    protected Boolean sessionM2MEnabled;
    public boolean getSessionM2MEnabled(){
        return sessionM2MEnabled;
    }
    protected Patient[] sessionPatients;
    public Patient[] getSessionPatients(){
        return sessionPatients;
    }

    protected MonitorData sessionMonitorData;

    public MonitorData getSessionMonitorData() {
        return sessionMonitorData;
    }

    protected enum RequestType {
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

    public CareLinkClient(CareLinkCredentialStore credentialStore) {
        this.carelinkCountry = credentialStore.getCredential().country;
        this.credentialStore = credentialStore;
        this.createHttpClient();
    }

    private void createHttpClient(){

        EditableCookieJar cookieJar = null;

        cookieJar = new EditableCookieJar();
        //Add cookies if there are any
        if(this.credentialStore.getCredential().cookies != null && this.credentialStore.getCredential().cookies.length > 0) {
            cookieJar.AddCookies(this.credentialStore.getCredential().cookies);
        }

        this.httpClient = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .connectionPool(new ConnectionPool(5, 10, TimeUnit.MINUTES))
                .build();
    }

    protected String careLinkServer() {
        if (this.carelinkCountry.equals("us"))
            return CARELINK_CONNECT_SERVER_US;
        else
            return CARELINK_CONNECT_SERVER_EU;
    }

    protected String cloudServer() {
        if (this.carelinkCountry.equals("us"))
            return CARELINK_CLOUD_SERVER_US;
        else
            return CARELINK_CLOUD_SERVER_EU;
    }

    //Wrapper for common request of recent data (last 24 hours)
    public RecentData getRecentData() {

        //Use default patient username if not provided
        return this.getRecentData(this.getDefaultPatientUsername());

    }

    //Get recent data of patient
    public RecentData getRecentData(String patientUsername) {

        // Force login to get basic info
        if (getAuthentication() == null)
            return null;

        // 7xxG
        if (this.isBleDevice(patientUsername))
            return this.getConnectDisplayMessage(this.sessionProfile.username, this.sessionUser.getUserRole(), patientUsername,
                    sessionCountrySettings.blePereodicDataEndpoint);
            // Guardian + multi
        else if (this.sessionM2MEnabled)
            return this.getM2MPatientData(patientUsername);
            // Guardian + single
        else
            return this.getLast24Hours();

    }

    //Determine default patient
    public String getDefaultPatientUsername() {

        // Force login to get basic info
        if (getAuthentication() == null)
            return null;

        // Care Partner + multi follow => first patient
        if (this.sessionUser.isCarePartner() && this.sessionM2MEnabled)
            if (this.sessionPatients != null && this.sessionPatients.length > 0)
                return this.sessionPatients[0].username;
            else
                return null;
            // Not care partner or no multi follow => username from session profile
        else if (this.sessionProfile.username != null)
            return this.sessionProfile.username;
        else
            return null;
    }

    public boolean isBleDevice(String patientUsername){

        Boolean recentUploadBle;

        // Session device already determined
        if(sessionDeviceIsBle != null)
            return sessionDeviceIsBle;

        // Force login to get basic info
        if(getAuthentication() == null)
            return  false;

        // Patient: device from recent uploads if possible
        if(!this.sessionUser.isCarePartner()){
            recentUploadBle = this.isRecentUploadBle();
            if(recentUploadBle != null){
                this.sessionDeviceIsBle = recentUploadBle;
                return sessionDeviceIsBle;
            }
        }

        // Care partner (+M2M): device from patient list
        if(this.sessionM2MEnabled && this.sessionUser.isCarePartner())
            if(patientUsername == null || this.sessionPatients == null)
                return false;
            else {
                for (int i = 0; i < this.sessionPatients.length; i++) {
                    if (sessionPatients[i].username.equals(patientUsername))
                        return sessionPatients[i].isBle();
                }
                return false;
            }
        // Other: classic method (session monitor data)
        else
            return this.sessionMonitorData.isBle();

    }

    public Boolean isRecentUploadBle(){

        if(this.sessionRecentUploads == null)
            return null;

        for(DataUpload upload : this.sessionRecentUploads.recentUploads){
            if(upload.device.toUpperCase().contains("MINIMED") || upload.device.toUpperCase().contains("SIMPLERA"))
                return  true;
            else if(upload.device.toUpperCase().contains("GUARDIAN"))
                return  false;
        }
        return null;
    }

    //Authentication methods
    protected boolean executeLoginProcedure() {

        Response loginSessionResponse = null;
        Response doLoginResponse = null;
        Response consentResponse = null;


        lastLoginSuccess = false;
        loginInProcess = true;
        lastErrorMessage = "";

        try {
            //Clear session
            this.clearSessionInfos();

            //Open login (get SessionId and SessionData)
            loginSessionResponse = this.getLoginSession();
            this.lastResponseCode = loginSessionResponse.code();

            //Login
            doLoginResponse = this.doLogin(loginSessionResponse);
            this.lastResponseCode = doLoginResponse.code();
            loginSessionResponse.close();

            //Consent
            consentResponse = this.doConsent(doLoginResponse);
            doLoginResponse.close();
            this.lastResponseCode = consentResponse.code();
            consentResponse.close();

            // Get required sessions infos
            if(this.getSessionInfos())
                lastLoginSuccess = true;

        } catch (Exception e) {
            lastErrorMessage = e.getClass().getSimpleName() + ":" + e.getMessage();
            // lastStackTraceString = Log.getStackTraceString(e);
        } finally {
            loginInProcess = false;
        }

        return lastLoginSuccess;

    }

    protected boolean getSessionInfos(){

        collectingSessionInfos = true;

        try {
            this.clearSessionInfos();
            // User
            this.sessionUser = this.getMyUser();
            // Profile
            this.sessionProfile = this.getMyProfile();
            // Country settings
            this.sessionCountrySettings = this.getMyCountrySettings();
            // Recent uploads (only for patients)
            if (!this.sessionUser.isCarePartner())
                this.sessionRecentUploads = this.getRecentUploads(30);
            this.sessionM2MEnabled = this.getM2MEnabled().value;
            // Multi follow + Care Partner => patients
            if (this.sessionM2MEnabled && this.sessionUser.isCarePartner())
                this.sessionPatients = this.getM2MPatients();
                // Single follow and/or Patient => monitor data
            else
                this.sessionMonitorData = this.getMonitorData();

            if (this.sessionUser != null && this.sessionProfile != null && this.sessionCountrySettings != null && this.sessionM2MEnabled != null &&
                    (((!this.sessionM2MEnabled || !this.sessionUser.isCarePartner()) && this.sessionMonitorData != null) ||
                            (this.sessionM2MEnabled && this.sessionUser.isCarePartner() && this.sessionPatients != null)))
                this.sessionInfosLoaded = true;
            else {
                this.clearSessionInfos();
            }
        } catch (Exception ex) {

        } finally {
            collectingSessionInfos = false;
        }


        return this.sessionInfosLoaded;

    }

    protected void clearSessionInfos()
    {
        //((SimpleOkHttpCookieJar) this.httpClient.cookieJar()).deleteAllCookies();
        this.sessionUser = null;
        this.sessionProfile = null;
        this.sessionCountrySettings = null;
        this.sessionMonitorData = null;
        this.sessionPatients = null;
        this.sessionInfosLoaded = false;
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

        this.addHttpHeaders(requestBuilder, RequestType.HtmlGet, true);

        return this.httpClient.newCall(requestBuilder.build()).execute();

    }

    protected Response doLogin(Response loginSessionResponse) throws IOException {

        HttpUrl url = null;
        Request.Builder requestBuilder = null;
        RequestBody form = null;

        form = new FormBody.Builder()
                .add("sessionID", loginSessionResponse.request().url().queryParameter("sessionID"))
                .add("sessionData", loginSessionResponse.request().url().queryParameter("sessionData"))
                .add("locale", loginSessionResponse.request().url().queryParameter("locale"))
                .add("action", "login")
                .add("username", this.carelinkUsername)
                .add("password", this.carelinkPassword)
                .add("actionButton", "Log in")
                .build();

        url = new HttpUrl.Builder()
                .scheme(loginSessionResponse.request().url().scheme())
                .host(loginSessionResponse.request().url().host())
                .addPathSegments(loginSessionResponse.request().url().encodedPath().substring(1))
                .addQueryParameter("locale", loginSessionResponse.request().url().queryParameter("locale"))
                .addQueryParameter("country", loginSessionResponse.request().url().queryParameter("countrycode"))
                .build();

        requestBuilder = new Request.Builder()
                .url(url)
                .post(form);

        this.addHttpHeaders(requestBuilder, RequestType.HtmlGet, true);

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

        this.addHttpHeaders(requestBuilder, RequestType.HtmlPost, true);

        return this.httpClient.newCall(requestBuilder.build()).execute();

    }

    // Response parsing
    protected String extractResponseData(String respBody, String groupRegex, int groupIndex) {

        String responseData = null;
        Matcher responseDataMatcher = null;

        responseDataMatcher = Pattern.compile(groupRegex).matcher(respBody);
        if (responseDataMatcher.find()) {
            responseData = responseDataMatcher.group(groupIndex);
        }

        return responseData;

    }

    protected CareLinkAuthentication getAuthentication() {

        // CredentialStore is used
        if(this.credentialStore != null){
            if(!this.sessionInfosLoaded && this.credentialStore.getAuthStatus() == CareLinkCredentialStore.AUTHENTICATED && !this.collectingSessionInfos)
            {
                this.getSessionInfos();
            }
            if(!this.collectingSessionInfos && !this.sessionInfosLoaded)
                return  null;
            else
                return this.credentialStore.getCredential().getAuthentication();
        // New token is needed:
        // a) no token or about to expire => execute authentication
        // b) last response 401
        } else {
            if (!((SimpleOkHttpCookieJar) httpClient.cookieJar()).contains(CARELINK_AUTH_TOKEN_COOKIE_NAME)
                    || !((SimpleOkHttpCookieJar) httpClient.cookieJar()).contains(CARELINK_TOKEN_VALIDTO_COOKIE_NAME)
                    || !((new Date(Date.parse(((SimpleOkHttpCookieJar) httpClient.cookieJar())
                    .getCookies(CARELINK_TOKEN_VALIDTO_COOKIE_NAME).get(0).value())))
                    .after(new Date(new Date(System.currentTimeMillis()).getTime()
                            + AUTH_EXPIRE_DEADLINE_MINUTES * 60000)))
                    || this.lastResponseCode == 401
                    || (!loginInProcess && !this.lastLoginSuccess)
            ) {
                //execute new login process
                if (this.loginInProcess || !this.executeLoginProcedure())
                    return null;
            }

            //there can be only one auth cookie
            return new CareLinkAuthentication(
                    new Headers.Builder().add("Authorization", "Bearer" + " " + ((SimpleOkHttpCookieJar) httpClient.cookieJar()).getCookies(CARELINK_AUTH_TOKEN_COOKIE_NAME).get(0).value()).build(),
                    CareLinkAuthType.Browser);
            //return "Bearer" + " " + ((SimpleOkHttpCookieJar) httpClient.cookieJar()).getCookies(CARELINK_AUTH_TOKEN_COOKIE_NAME).get(0).value();
        }


    }


    //CareLink data APIs

    // My user
    public User getMyUser() {
        return this.getData(this.careLinkServer(), "patient/users/me", null, null, User.class);
    }

    // My profile
    public Profile getMyProfile() {
        return this.getData(this.careLinkServer(), "patient/users/me/profile", null, null, Profile.class);
    }

    // Recent uploads
    public RecentUploads getRecentUploads(int numOfUploads) {

        Map<String, String> queryParams = null;

        queryParams = new HashMap<String, String>();
        queryParams.put("numUploads", String.valueOf(numOfUploads));

        return this.getData(this.careLinkServer(), "patient/dataUpload/recentUploads", queryParams, null, RecentUploads.class);
    }

    // Monitoring data
    public MonitorData getMonitorData() {
        return this.getData(this.careLinkServer(), "patient/monitor/data", null, null, MonitorData.class);
    }

    // Country settings
    public CountrySettings getMyCountrySettings() {

        Map<String, String> queryParams = null;

        queryParams = new HashMap<String, String>();
        queryParams.put("countryCode", this.carelinkCountry);
        queryParams.put("language", CARELINK_LANGUAGE_EN);

        return this.getData(this.careLinkServer(), "patient/countries/settings", queryParams, null,
                CountrySettings.class);

    }

    // M2M Enabled
    public M2MEnabled getM2MEnabled() {
        return this.getData(this.careLinkServer(), "patient/configuration/system/personal.cp.m2m.enabled", null, null, M2MEnabled.class);
    }

    // M2M Patients
    public Patient[] getM2MPatients() {
        return this.getData(this.careLinkServer(), "patient/m2m/links/patients", null, null, Patient[].class);
    }

    // Classic last24hours webapp data
    public RecentData getLast24Hours() {

        Map<String, String> queryParams = null;
        RecentData recentData = null;
        boolean useCloudServer = false;

        queryParams = new HashMap<String, String>();
        queryParams.put("cpSerialNumber", "NONE");
        queryParams.put("msgType", "last24hours");
        queryParams.put("requestTime", String.valueOf(System.currentTimeMillis()));

        //use cloud server in every region
        useCloudServer = true;

        try {
            //Get data
            //carelink cloud server and no query params
            if(useCloudServer)
                recentData = this.getData(this.cloudServer(), "patient/connect/data", null, null, RecentData.class);
            //old carelink minimed server + query params
            else
                recentData = this.getData(this.careLinkServer(), "patient/connect/data", queryParams, null, RecentData.class);

            //Correct time
            if (recentData != null)
                correctTimeInRecentData(recentData);
        } catch (Exception e) {
            lastErrorMessage = e.getClass().getSimpleName() + ":" + e.getMessage();
        }

        return recentData;

    }

    // Periodic data from CareLink Cloud
    public RecentData getConnectDisplayMessage(String username, String role, String patientUsername, String endpointUrl) {

        RequestBody requestBody;
        Gson gson;
        JsonObject userJson;
        RecentData recentData = null;
        DisplayMessage displayMessage;
        boolean useNewEndpoint;
        HttpUrl newEndpointUrl;


        // Build user json for request
        userJson = new JsonObject();
        userJson.addProperty("username", username);
        userJson.addProperty("role", role);
        if(!JoH.emptyString(patientUsername))
            userJson.addProperty("patientId", patientUsername);

        gson = new GsonBuilder().create();

        requestBody = RequestBody.create(MediaType.get("application/json; charset=utf-8"), gson.toJson(userJson));

        //use new v11 endpoint in every region
        useNewEndpoint = true;

        //new endpoint url
        newEndpointUrl = new HttpUrl.Builder()
                .scheme("https")
                .host(this.cloudServer())
                .addPathSegments(API_PATH_DISPLAY_MESSAGE)
                .build();

        //get data and correct time
        try {
            //Use old data format for old endpoint
            if (!useNewEndpoint) {
                recentData = this.getData(HttpUrl.parse(endpointUrl), requestBody, RecentData.class);
                if (recentData != null) {
                    correctTimeInRecentData(recentData);
                }
            }
            //Use new data format for new endpoint
            else {
                displayMessage = this.getData(newEndpointUrl, requestBody, DisplayMessage.class);
                if (displayMessage != null && displayMessage.patientData != null) {
                    correctTimeInDisplayMessage(displayMessage);
                    recentData = displayMessage.patientData;
                }
            }
        } catch (Exception e) {
            lastErrorMessage = e.getClass().getSimpleName() + ":" + e.getMessage();
        }
        return recentData;

    }

    // New M2M last24hours webapp data
    public RecentData getM2MPatientData(String patientUsername) {

        RecentData recentData = null;
        boolean useCloudServer = false;
        Map<String, String> queryParams = null;

        //Patient username is mandantory!
        if(patientUsername == null || patientUsername.isEmpty())
            return null;

        queryParams = new HashMap<String, String>();
        queryParams.put("cpSerialNumber", "NONE");
        queryParams.put("msgType", "last24hours");
        queryParams.put("requestTime", String.valueOf(System.currentTimeMillis()));

        //use cloud server in every region
        useCloudServer = true;

        //Get data
        //carelink cloud server and no query params
        if(useCloudServer)
            recentData = this.getData(this.cloudServer(), "patient/m2m/connect/data/gc/patients/" + patientUsername, null, null, RecentData.class);
        //old carelink minimed server + query params
        else
            recentData = this.getData(this.careLinkServer(), "patient/m2m/connect/data/gc/patients/" + patientUsername, queryParams, null, RecentData.class);

        //Correct time
        if (recentData != null)
            correctTimeInRecentData(recentData);

        return recentData;

    }

    // General data request for API calls

    protected <T> T getData(String host, String path, RequestBody requestBody, Class<T> dataClass) {
           return  this.getData(new HttpUrl.Builder().scheme("https").host(host).addPathSegments(path).build(), requestBody, dataClass);
    }

    protected <T> T getData(HttpUrl url, RequestBody requestBody, Class<T> dataClass) {

        Request.Builder requestBuilder = null;
        HttpUrl.Builder urlBuilder = null;
        CareLinkAuthentication authentication = null;
        String responseString = null;
        Response response = null;
        Object data = null;
        boolean isBrowserClient = true;

        this.lastDataSuccess = false;
        this.lastErrorMessage = "";

        // Get authentication
        authentication = this.getAuthentication();

        if (authentication != null) {

            // Create request for URL with authToken
            //requestBuilder = new Request.Builder().url(url).addHeader("Authorization", authToken);
            requestBuilder = new Request.Builder().url(url).headers(authentication.getHeaders());

            // Add additional headers
            if (requestBody == null) {
                this.addHttpHeaders(requestBuilder, RequestType.Json, authentication.authType == CareLinkAuthType.Browser);
            } else {
                requestBuilder.post(requestBody);
                this.addHttpHeaders(requestBuilder, RequestType.HtmlPost, authentication.authType == CareLinkAuthType.Browser);
            }

            // Send request
            try {
                response = this.httpClient.newCall(requestBuilder.build()).execute();
                this.lastResponseCode = response.code();
                if (response.isSuccessful()) {
                    try {
                        responseString = response.body().string();
                        data = new GsonBuilder().create().fromJson(responseString, dataClass);
                        this.lastDataSuccess = true;
                    } catch (Exception e) {
                        lastErrorMessage = e.getClass().getSimpleName() + ":" + e.getMessage();
                    }
                }
                response.close();
            } catch (Exception e) {
                lastErrorMessage = e.getClass().getSimpleName() + ":" + e.getMessage();
            }

        }

        //Return result
        if (data != null)
            return dataClass.cast(data);
        else
            return null;

    }

    protected <T> T getData(String host, String path, Map<String, String> queryParams, RequestBody requestBody,
                            Class<T> dataClass) {

        HttpUrl.Builder urlBuilder = null;
        HttpUrl url = null;

        // Build url
        urlBuilder = new HttpUrl.Builder().scheme("https").host(host).addPathSegments(path);
        if (queryParams != null) {
            for (Map.Entry<String, String> param : queryParams.entrySet()) {
                urlBuilder.addQueryParameter(param.getKey(), param.getValue());
            }
        }

        url = urlBuilder.build();

        return this.getData(url, requestBody, dataClass);

    }

    // Http header builder for requests
    protected void addHttpHeaders(Request.Builder requestBuilder, RequestType type, boolean isBrowserClient) {

        //Add common browser headers
        if(isBrowserClient) {
            requestBuilder
                    .addHeader("Accept-Language", "en;q=0.9, *;q=0.8")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Sec-Ch-Ua", "\"Google Chrome\";v=\"117\", \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"117\"")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Mobile Safari/537.36");
        }

        //Set media type based on request type
        switch (type) {
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

    protected void correctTimeInDisplayMessage(DisplayMessage displayMessage) {

        boolean timezoneMissing = false;
        String offsetString = null;
        RecentData recentData = null;

        recentData = displayMessage.patientData;

        //time data is available to check and correct time if needed
        if (recentData.lastConduitDateTime != null && recentData.lastConduitDateTime.getTime() > 1
                && recentData.lastConduitUpdateServerDateTime  > 1) {

            //Correct times if server <> device > 26 mins => possibly different time zones
            int diffInHour = (int) Math.round(((recentData.lastConduitUpdateServerDateTime - recentData.lastConduitDateTime.getTime()) / 3600000D));
            if (diffInHour != 0 && diffInHour < 26) {

                recentData.lastConduitDateTime = shiftDateByHours(recentData.lastConduitDateTime, diffInHour);

                //Sensor glucose
                if (recentData.sgs != null) {
                    for (SensorGlucose sg : recentData.sgs) {
                        if(sg.timestamp != null)
                            sg.timestamp = shiftDateByHours(sg.timestamp, diffInHour);
                    }
                }

                //Markers
                if (recentData.markers != null) {
                    for (Marker marker : recentData.markers) {
                        if(marker.timestamp != null)
                            marker.timestamp = shiftDateByHours(marker.timestamp, diffInHour);
                    }
                }
                //Notifications
                if (recentData.notificationHistory != null) {
                    if (recentData.notificationHistory.clearedNotifications != null) {
                        for (ClearedNotification notification : recentData.notificationHistory.clearedNotifications) {
                            if(notification.dateTime != null) {
                                notification.dateTime = shiftDateByHours(notification.dateTime, diffInHour);
                                notification.triggeredDateTime = shiftDateByHours(notification.triggeredDateTime, diffInHour);
                            }
                        }
                    }
                    if (recentData.notificationHistory.activeNotifications != null) {
                        for (ActiveNotification notification : recentData.notificationHistory.activeNotifications) {
                            if(notification.dateTime != null)
                                notification.dateTime = shiftDateByHours(notification.dateTime, diffInHour);
                        }
                    }
                }

            }

        }

        displayMessage.patientData = recentData;

    }

    protected void correctTimeInRecentData(RecentData recentData) {

        boolean timezoneMissing = false;
        String offsetString = null;

        if (recentData.sMedicalDeviceTime != null && !recentData.sMedicalDeviceTime.isEmpty() && recentData.lastMedicalDeviceDataUpdateServerTime > 1) {

            //MedicalDeviceTime string has no timezone information
            if (parseDateString(recentData.sMedicalDeviceTime) == null) {

                timezoneMissing = true;

                //Try get TZ offset string: lastSG or lastAlarm
                if(recentData.lastSG != null && recentData.lastSG.datetime != null)
                    offsetString = this.getZoneOffset(recentData.lastSG.datetime);
                else
                    offsetString = this.getZoneOffset(recentData.lastAlarm.datetime);

                //Set last alarm datetimeAsDate
                if(recentData.lastAlarm != null && recentData.lastAlarm.datetime != null)
                    recentData.lastAlarm.datetimeAsDate = parseDateString(recentData.lastAlarm.datetime);
                //Build correct dates with timezone
                recentData.sMedicalDeviceTime = recentData.sMedicalDeviceTime + offsetString;
                recentData.medicalDeviceTimeAsString = recentData.medicalDeviceTimeAsString + offsetString;
                recentData.sLastSensorTime = recentData.sLastSensorTime + offsetString;
                recentData.lastSensorTSAsString = recentData.lastSensorTSAsString + offsetString;

            } else {
                timezoneMissing = false;
            }

            //Parse dates
            recentData.dMedicalDeviceTime = parseDateString(recentData.sMedicalDeviceTime);
            recentData.medicalDeviceTimeAsDate = parseDateString(recentData.medicalDeviceTimeAsString);
            recentData.dLastSensorTime = parseDateString(recentData.sLastSensorTime);
            recentData.lastSensorTSAsDate = parseDateString(recentData.lastSensorTSAsString);

            //Sensor
            if (recentData.sgs != null) {
                for (SensorGlucose sg : recentData.sgs) {
                    sg.datetimeAsDate = parseDateString(sg.datetime);
                }
            }

            //Timezone was present => check if time needs correction
            if (!timezoneMissing) {

                //Calc time diff between event time and actual local time
                int diffInHour = (int) Math.round(((recentData.lastMedicalDeviceDataUpdateServerTime - recentData.dMedicalDeviceTime.getTime()) / 3600000D));

                //Correct times if server <> device > 26 mins => possibly different time zones
                if (diffInHour != 0 && diffInHour < 26) {


                    recentData.medicalDeviceTimeAsDate = shiftDateByHours(recentData.medicalDeviceTimeAsDate, diffInHour);
                    recentData.dMedicalDeviceTime = shiftDateByHours(recentData.dMedicalDeviceTime, diffInHour);
                    recentData.lastConduitDateTime = shiftDateByHours(recentData.lastConduitDateTime, diffInHour);
                    recentData.lastSensorTSAsDate = shiftDateByHours(recentData.lastSensorTSAsDate, diffInHour);
                    recentData.dLastSensorTime = shiftDateByHours(recentData.dLastSensorTime, diffInHour);
                    //Sensor
                    if (recentData.sgs != null) {
                        for (SensorGlucose sg : recentData.sgs) {
                            sg.datetimeAsDate = shiftDateByHours(sg.datetimeAsDate, diffInHour);
                        }
                    }
                    //Markers
                    if (recentData.markers != null) {
                        for (Marker marker : recentData.markers) {
                            marker.dateTime = shiftDateByHours(marker.dateTime, diffInHour);
                        }
                    }
                    //Notifications
                    if (recentData.notificationHistory != null) {
                        if (recentData.notificationHistory.clearedNotifications != null) {
                            for (ClearedNotification notification : recentData.notificationHistory.clearedNotifications) {
                                notification.dateTime = shiftDateByHours(notification.dateTime, diffInHour);
                                notification.triggeredDateTime = shiftDateByHours(notification.triggeredDateTime, diffInHour);
                            }
                        }
                        if (recentData.notificationHistory.activeNotifications != null) {
                            for (ActiveNotification notification : recentData.notificationHistory.activeNotifications) {
                                notification.dateTime = shiftDateByHours(notification.dateTime, diffInHour);
                            }
                        }
                    }
                }
            }

        }

        //Set dateTime of Markers using index if dateTime is missing (Guardian Connect)
        if (recentData.dLastSensorTime != null && recentData.markers != null) {
            for (Marker marker : recentData.markers) {
                if (marker != null && marker.dateTime == null) {
                    try {
                        marker.dateTime = calcTimeByIndex(recentData.dLastSensorTime, marker.index, true);
                    } catch (Exception ex) {
                        continue;
                    }
                }
            }
        }

    }

    protected String getZoneOffset(String dateString) {
        Matcher offsetDataMatcher = Pattern.compile(("(.*)([\\+|-].*)")).matcher(dateString);
        if (offsetDataMatcher.find())
            return offsetDataMatcher.group(2);
        else
            return null;
    }

    protected Date parseDateString(String dateString) {
        for (SimpleDateFormat zonedFormat : ZONED_DATE_FORMATS) {
            try {
                return zonedFormat.parse(dateString);
            } catch (Exception ex) {
            }
        }
        return null;
    }

    protected Date shiftDateByHours(Date date, int hours) {
        if (date != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.HOUR_OF_DAY, hours);
            return calendar.getTime();
        } else {
            return null;
        }
    }

    //Calculate DateTime using graph index (1 index = 5 minute)
    protected static Date calcTimeByIndex(Date lastSensorTime, int index, boolean round) {
        if (lastSensorTime == null)
            return null;
        else if (round)
            //round to 10 minutes
            return new Date((Math.round((calcTimeByIndex(lastSensorTime, index, false).getTime()) / 600_000D) * 600_000L));
        else
            return new Date((lastSensorTime.getTime() - ((287 - index) * 300_000L)));
    }

}
