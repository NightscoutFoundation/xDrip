package com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CarePartnerAppConfig {

    public JsonObject regionConfig = null;
    public JsonObject ssoConfig = null;

    public String getRegion() {
        return regionConfig.get("region").getAsString();
    }

    public String getSSOConfigUrl() {
        return regionConfig.get("SSOConfiguration").getAsString();
    }

    public String getCloudBaseUrl() {
        return regionConfig.get("baseUrlCumulus").getAsString();
    }

    public String getCareLinkBaseUrl() {
        return regionConfig.get("baseUrlCareLink").getAsString();
    }

    public String getSSOServerHost() {
        return this.getChildJsonString(ssoConfig, "server.hostname");
    }

    public String getSSOServerPrefix() {
        return this.getChildJsonString(ssoConfig, "server.prefix");
    }

    public int getSSOServerPort() {
        return this.getChildJsonElement(ssoConfig, "server.port").getAsInt();
    }

    public String getOAuthAuthEndpoint() {
        return this.getChildJsonString(ssoConfig, "oauth.system_endpoints.authorization_endpoint_path").substring(1);
    }

    public String getOAuthTokenEndpoint() {
        return this.getChildJsonString(ssoConfig, "oauth.system_endpoints.token_endpoint_path").substring(1);
    }

    public String getMagCredentialInitEndpoint() {
        return this.getChildJsonString(ssoConfig, "mag.system_endpoints.client_credential_init_endpoint_path").substring(1);
    }

    public String getMagDeviceRegisterEndpoint() {
        return this.getChildJsonString(ssoConfig, "mag.system_endpoints.device_register_endpoint_path").substring(1);
    }

    public String getClientId() {
        return getClientMemberString("client_id");
    }

    public String getOAuthScope() {
        return getClientMemberString("scope");
    }

    public String getOAuthRedirectUri() {
        return getClientMemberString("redirect_uri");
    }

    public int getRefreshLifetimeSec() {
        return Integer.parseInt(getClientMemberString("client_key_custom.lifetimes.oauth2_refresh_token_lifetime_sec"));
    }

    private String getClientMemberString(String clientMember) {
        return this.getChildJsonString(this.getChildJsonElement(ssoConfig, "oauth.client.client_ids").getAsJsonArray().get(0)
                .getAsJsonObject(), clientMember);
    }

    private String getChildJsonString(JsonObject parent, String path) {
        return getChildJsonElement(parent, path).getAsString();
    }

    private JsonElement getChildJsonElement(JsonObject parent, String path) {

        JsonElement obj = parent;

        for (String member : path.split("\\.")) {
            obj = obj.getAsJsonObject().get(member);
        }

        return obj;

    }

}
