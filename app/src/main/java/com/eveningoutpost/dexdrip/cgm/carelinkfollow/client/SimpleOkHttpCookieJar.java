package com.eveningoutpost.dexdrip.cgm.carelinkfollow.client;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class SimpleOkHttpCookieJar implements CookieJar {

    private List<Cookie> storage = new ArrayList<>();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        if (cookies != null && cookies.size() > 0)
            storage.addAll(cookies);
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {

        List<Cookie> cookies = new ArrayList<>();

        // Remove expired Cookies
        removeExpiredCookies();

        // Only return matching Cookies
        for (Cookie cookie : storage) {
            if (cookie.matches(url)) {
                cookies.add(cookie);
            }
        }

        return cookies;
    }

    public List<Cookie> getCookies(String name) {

        List<Cookie> cookies = new ArrayList<>();

        // Remove expired Cookies
        removeExpiredCookies();

        // Only return matching Cookies
        for (Cookie cookie : storage) {
            if (cookie.name().equals(name)) {
                cookies.add(cookie);
            }
        }

        return cookies;

    }

    public boolean contains(String name) {
        return (getCookies(name).size() > 0);
    }

    public void deleteCookie(String name) {
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).name() == name) {
                storage.remove(i);
            }
        }
    }

    public void deleteAllCookies() {
        storage.clear();
    }

    private void removeExpiredCookies() {
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).expiresAt() < System.currentTimeMillis()) {
                storage.remove(i);
            }
        }
    }

}