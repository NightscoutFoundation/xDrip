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
        String sso_configuration_key = regionConfig.get("UseSSOConfiguration").getAsString();
        if (!regionConfig.has(sso_configuration_key)) {
            sso_configuration_key = "SSOConfiguration";
        }

        return regionConfig.get(sso_configuration_key).getAsString();
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
        return this.getChildJsonString(ssoConfig, "system_endpoints.authorization_endpoint_path").substring(1);
    }

    public String getOAuthTokenEndpoint() {
        return this.getChildJsonString(ssoConfig, "system_endpoints.token_endpoint_path").substring(1);
    }

    public String getOAuthClientId() {
        return getClientMemberString("client_id");
    }

    public String getOAuthScope() {
        return getClientMemberString("scope");
    }

    public String getOAuthRedirectUri() {
        return getClientMemberString("redirect_uri");
    }

    public String getOAuthAudience() {
        return getClientMemberString("audience");
    }

    private String getClientMemberString(String clientMember) {
        return this.getChildJsonString(this.getChildJsonElement(ssoConfig, "client").getAsJsonObject(), clientMember);
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
