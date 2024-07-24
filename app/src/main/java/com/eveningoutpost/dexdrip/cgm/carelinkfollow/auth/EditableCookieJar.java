package com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class EditableCookieJar implements CookieJar {

    private List<Cookie> storage = new ArrayList<>();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        //add cookies, delete cookie with same name before add
        for (Cookie cookie : cookies) {
            if (this.contains(cookie.name()))
                this.deleteCookie(cookie.name());
            this.storage.add(cookie);
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {

        List<Cookie> cookies = new ArrayList<>();

        // Remove expired Cookies
        //removeExpiredCookies();

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
        //removeExpiredCookies();

        // Only return matching Cookies
        for (Cookie cookie : storage) {
            if (cookie.name().equals(name)) {
                cookies.add(cookie);
            }
        }

        return cookies;

    }

    public List<Cookie> getAllCookies() {
        return storage;
    }

    public void AddCookie(Cookie cookie) {
        storage.add(cookie);
    }

    public void AddCookies(Cookie[] cookies) {
        storage.addAll(Arrays.asList(cookies));
    }

    public boolean contains(String name) {
        return (getCookies(name).size() > 0);
    }

    public void deleteCookie(String name) {
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).name().contains(name)) {
                storage.remove(i);
            }
        }
    }

    public void deleteAllCookies() {
        storage.clear();
    }

    /*
    private void removeExpiredCookies() {
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).expiresAt() < System.currentTimeMillis()) {
                storage.remove(i);
            }
        }
    }

     */

}
