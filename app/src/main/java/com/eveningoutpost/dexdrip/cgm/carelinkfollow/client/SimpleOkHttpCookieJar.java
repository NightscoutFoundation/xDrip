package com.eveningoutpost.dexdrip.cgm.carelinkfollow.client;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.ArrayList;
import java.util.List;

public class SimpleOkHttpCookieJar implements CookieJar {

    private List<Cookie> storage = new ArrayList<>();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        storage.addAll(cookies);
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {

        List<Cookie> cookies = new ArrayList<>();

        // Remove expired Cookies
        removeExpiredCookies();

        // Only return matching Cookies
        for (int i = 0; i < storage.size(); i++) {
            if(storage.get(i).matches(url)) {
                cookies.add(storage.get(i));
            }
        }

        return cookies;
    }

    public List<Cookie> getCookies(String name) {

        List<Cookie> cookies = new ArrayList<>();

        // Remove expired Cookies
        removeExpiredCookies();

        // Only return matching Cookies
        for (int i = 0; i < storage.size(); i++) {
            if(storage.get(i).name().equals(name)) {
                cookies.add(storage.get(i));
            }
        }

        return cookies;

    }

    public boolean contains(String name) {
        return (getCookies(name).size() > 0);
    }

    public void deleteCookie(String name) {
        for (int i = 0; i < storage.size(); i++) {
            if(storage.get(i).name() == name) {
                storage.remove(i);
            }
        }
    }

    public void deleteAllCookies() {
        storage.clear();
    }

    private void removeExpiredCookies(){
        for (int i = 0; i < storage.size(); i++) {
            if(storage.get(i).expiresAt() < System.currentTimeMillis()) {
                storage.remove(i);
            }
        }
    }

}