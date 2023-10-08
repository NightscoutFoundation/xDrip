package com.eveningoutpost.dexdrip.cgm.nsfollow.utils;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import java.net.URI;
import java.net.URISyntaxException;

import lombok.RequiredArgsConstructor;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;

/**
 * jamorham
 *
 * Representation of a Nightscout URL with utility methods and caching
 */

@RequiredArgsConstructor
public class NightscoutUrl {

    private final String url;

    private URI uri;
    private int apiVersion = -1;
    private String secret;
    private String hashedSecret;


    public URI getURI() {
        if (uri == null) {
            if (url == null) return null;
            try {
                // auto fix missing api strings
                String this_url = url.trim();
                if (!this_url.contains("/api/")) {
                    if (!this_url.endsWith("/")) {
                        this_url += "/";
                    }
                    this_url += "api/v1/";
                } else {
                    if (!this_url.endsWith("/")) {
                        this_url += "/";
                    }
                }

                uri = new URI(this_url);

            } catch (URISyntaxException e) {
                return null;
            }
        }
        return uri;
    }

    public int getApiVersion() {
        if (apiVersion == -1) {
            final URI uri = getURI();
            if (uri != null) {
                if (uri.getPath().endsWith("/v1/")) {
                    apiVersion = 1;
                } else {
                    apiVersion = 0;
                }
            }
        }
        return apiVersion;
    }

    public String getSecret() {
        if (secret == null) {
            final URI uri = getURI();
            if (uri != null) {
                secret = uri.getUserInfo();
            }
        }
        return (emptyString(secret) ? null : secret);
    }

    public String getHashedSecret() {
        if (hashedSecret == null) {
            final String secret = getSecret();
            hashedSecret = secret != null ? Hashing.sha1().hashBytes(secret.getBytes(Charsets.UTF_8)).toString() : null;
        }
        return hashedSecret;
    }
}



